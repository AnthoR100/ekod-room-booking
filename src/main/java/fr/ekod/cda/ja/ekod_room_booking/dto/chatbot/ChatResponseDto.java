package fr.ekod.cda.ja.ekod_room_booking.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatResponseDto {

    private Long conversationId;
    private String content;
    private LocalDateTime createdAt;
}