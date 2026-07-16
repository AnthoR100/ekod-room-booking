package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ChatRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ChatResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.chatbot.ConversationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(
            @Valid @RequestBody ChatRequestDto dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.chat(dto, user));
    }

    @GetMapping("/history")
    public ResponseEntity<ConversationResponseDto> history(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getHistory(user));
    }
}