package fr.ekod.cda.ja.ekod_room_booking.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}
