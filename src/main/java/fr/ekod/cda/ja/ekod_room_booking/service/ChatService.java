package fr.ekod.cda.ja.ekod_room_booking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ChatMessageResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ChatRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ChatResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ConversationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ChatException;
import fr.ekod.cda.ja.ekod_room_booking.model.ChatMessage;
import fr.ekod.cda.ja.ekod_room_booking.model.Conversation;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ChatRole;
import fr.ekod.cda.ja.ekod_room_booking.repository.ChatMessageRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final OpenAIClient openAIClient;

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant de réservation de salles pour EKOD. \
            Tu peux chercher des salles disponibles, créer des réservations et annuler des réservations. \
            Quand l'utilisateur veut réserver ou annuler une réservation, utilise les outils mis à ta disposition. \
            Pour annuler une réservation, récupère d'abord son identifiant via get_my_reservations si tu ne le connais pas déjà, \
            puis appelle cancel_reservation. \
            Ne confirme jamais qu'une action a réussi (réservation créée ou annulée) tant que tu n'as pas reçu le résultat de l'outil correspondant : \
            base ta réponse uniquement sur ce résultat, jamais sur une supposition. \
            Un utilisateur ne peut annuler que ses propres réservations ; si l'outil renvoie une erreur d'autorisation, informe-en l'utilisateur.\
            """;

    @Transactional
    public ChatResponseDto chat(ChatRequestDto dto, User user) {
        // 7.1 — Récupérer ou créer la conversation
        Conversation conversation = conversationRepository.findByUserId(user.getId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder().user(user).build()
                ));

        // 7.2 — Sauvegarder le message utilisateur
        chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .chatRole(ChatRole.USER)
                .content(dto.getContent())
                .build());

        // 7.3 — Charger les 10 derniers messages en ordre chronologique
        List<ChatMessage> history = chatMessageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(history);

        // 7.4-7.7 — Appel OpenAI avec gestion des tool calls
        String response = callOpenAI(history, user);

        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .chatRole(ChatRole.ASSISTANT)
                .content(response)
                .build());

        return new ChatResponseDto(conversation.getId(), response, assistantMessage.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ConversationResponseDto getHistory(User user) {
        return conversationRepository.findByUserId(user.getId())
                .map(conversation -> {
                    List<ChatMessageResponseDto> messages = chatMessageRepository
                            .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                            .stream()
                            .map(m -> new ChatMessageResponseDto(
                                    m.getId(),
                                    m.getChatRole().name(),
                                    m.getContent(),
                                    m.getCreatedAt()
                            ))
                            .toList();
                    return new ConversationResponseDto(conversation.getId(), conversation.getCreatedAt(), messages);
                })
                .orElseGet(() -> new ConversationResponseDto(null, null, List.of()));
    }

    private static final int MAX_TOOL_ITERATIONS = 5;

    private String callOpenAI(List<ChatMessage> history, User user) {
        List<ChatCompletionMessageParam> messages = buildMessages(history);

        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("gpt-4.1-mini")
                    .messages(messages)
                    .tools(buildTools())
                    .maxCompletionTokens(400L)
                    .build();

            ChatCompletionMessage responseMessage;
            try {
                ChatCompletion completion = openAIClient.chat().completions().create(params);
                responseMessage = completion.choices().get(0).message();
            } catch (Exception e) {
                throw new ChatException("Erreur lors de la communication avec l'IA : " + e.getMessage());
            }

            List<ChatCompletionMessageToolCall> toolCalls = responseMessage.toolCalls()
                    .orElse(List.of());

            if (toolCalls.isEmpty()) {
                return responseMessage.content().orElse("");
            }

            appendToolCallExchange(messages, responseMessage, toolCalls, user);
        }

        throw new ChatException("Nombre maximum d'appels d'outils atteint");
    }

    private List<ChatCompletionMessageParam> buildMessages(List<ChatMessage> history) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(SYSTEM_PROMPT)
                        .build()
        ));

        for (ChatMessage msg : history) {
            if (msg.getChatRole() == ChatRole.USER) {
                messages.add(ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(msg.getContent())
                                .build()
                ));
            } else {
                messages.add(ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                                .content(msg.getContent())
                                .build()
                ));
            }
        }

        return messages;
    }

    private void appendToolCallExchange(
            List<ChatCompletionMessageParam> messages,
            ChatCompletionMessage assistantMessage,
            List<ChatCompletionMessageToolCall> toolCalls,
            User user
    ) {
        // Ajouter le message assistant (avec ses tool calls) à l'historique
        ChatCompletionAssistantMessageParam.Builder assistantParamBuilder =
                ChatCompletionAssistantMessageParam.builder()
                        .toolCalls(toolCalls);
        assistantMessage.content().ifPresent(assistantParamBuilder::content);
        messages.add(ChatCompletionMessageParam.ofAssistant(assistantParamBuilder.build()));

        // Exécuter chaque outil et ajouter les résultats
        for (ChatCompletionMessageToolCall toolCall : toolCalls) {
            String result = executeToolCall(
                    toolCall.function().name(),
                    toolCall.function().arguments(),
                    user
            );
            messages.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCall.id())
                            .content(result)
                            .build()
            ));
        }
    }

    private String executeToolCall(String functionName, String arguments, User user) {
        try {
            JsonNode args = objectMapper.readTree(arguments);

            return switch (functionName) {
                case "search_available_rooms" -> {
                    int numberOfPeople = args.get("numberOfPeople").asInt();
                    var rooms = roomService.findAvailable().stream()
                            .filter(r -> r.getCapacity() >= numberOfPeople)
                            .toList();
                    yield objectMapper.writeValueAsString(rooms);
                }
                case "create_reservation" -> {
                    ReservationRequestDto reservationDto = new ReservationRequestDto();
                    reservationDto.setRoomId(args.get("roomId").asLong());
                    reservationDto.setStartDateTime(LocalDateTime.parse(args.get("startDateTime").asText()));
                    reservationDto.setEndDateTime(LocalDateTime.parse(args.get("endDateTime").asText()));
                    reservationDto.setNumberOfPeople(args.get("numberOfPeople").asInt());
                    if (args.has("purpose") && !args.get("purpose").isNull()) {
                        reservationDto.setPurpose(args.get("purpose").asText());
                    }
                    var reservation = reservationService.create(reservationDto, user);
                    yield objectMapper.writeValueAsString(reservation);
                }
                case "get_my_reservations" -> {
                    var reservations = reservationService.findByUserId(user);
                    yield objectMapper.writeValueAsString(reservations);
                }
                case "cancel_reservation" -> {
                    long reservationId = args.get("reservationId").asLong();
                    var reservation = reservationService.cancel(reservationId, user);
                    yield objectMapper.writeValueAsString(reservation);
                }
                default -> "Fonction inconnue : " + functionName;
            };
        } catch (Exception e) {
            System.out.println("TOOL ERROR: " + e.getMessage());
            e.printStackTrace();
            return "Erreur lors de l'exécution de l'outil : " + e.getMessage();
        }
    }

    private List<ChatCompletionTool> buildTools() {
        return List.of(
                buildTool(
                        "search_available_rooms",
                        "Cherche les salles disponibles pour un créneau et une capacité donnés",
                        Map.of(
                                "startDateTime", Map.of("type", "string", "description", "Date et heure de début (ISO-8601)"),
                                "endDateTime", Map.of("type", "string", "description", "Date et heure de fin (ISO-8601)"),
                                "numberOfPeople", Map.of("type", "integer", "description", "Nombre de personnes")
                        ),
                        List.of("startDateTime", "endDateTime", "numberOfPeople")
                ),
                buildTool(
                        "create_reservation",
                        "Crée une réservation de salle pour l'utilisateur connecté",
                        Map.of(
                                "roomId", Map.of("type", "integer", "description", "Identifiant de la salle"),
                                "startDateTime", Map.of("type", "string", "description", "Date et heure de début (ISO-8601)"),
                                "endDateTime", Map.of("type", "string", "description", "Date et heure de fin (ISO-8601)"),
                                "numberOfPeople", Map.of("type", "integer", "description", "Nombre de personnes"),
                                "purpose", Map.of("type", "string", "description", "Objet de la réservation (optionnel)")
                        ),
                        List.of("roomId", "startDateTime", "endDateTime", "numberOfPeople")
                ),
                buildTool(
                        "get_my_reservations",
                        "Récupère les réservations de l'utilisateur connecté",
                        Map.of(),
                        List.of()
                ),
                buildTool(
                        "cancel_reservation",
                        "Annule une réservation. Un utilisateur ne peut annuler que ses propres réservations (sauf un administrateur, qui peut annuler celles de n'importe qui)",
                        Map.of(
                                "reservationId", Map.of("type", "integer", "description", "Identifiant de la réservation à annuler")
                        ),
                        List.of("reservationId")
                )
        );
    }

    private ChatCompletionTool buildTool(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> required
    ) {
        return ChatCompletionTool.builder()
                .function(FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .parameters(FunctionParameters.builder()
                                .putAdditionalProperty("type", JsonValue.from("object"))
                                .putAdditionalProperty("properties", JsonValue.from(properties))
                                .putAdditionalProperty("required", JsonValue.from(required))
                                .build())
                        .build())
                .build();
    }
}
