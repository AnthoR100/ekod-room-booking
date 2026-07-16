package fr.ekod.cda.ja.ekod_room_booking.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ConversationResponseDto {

    private Long id;
    private LocalDateTime createdAt;
    private List<ChatMessageResponseDto> messages;
}