package fr.ekod.cda.ja.ekod_room_booking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDto {

    @NotBlank
    private String refreshToken;
}
