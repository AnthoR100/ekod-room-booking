package fr.ekod.cda.ja.ekod_room_booking.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequestDto {

    @NotBlank
    private String content;
}