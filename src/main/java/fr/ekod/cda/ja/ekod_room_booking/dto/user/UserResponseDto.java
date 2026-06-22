package fr.ekod.cda.ja.ekod_room_booking.dto.user;

import fr.ekod.cda.ja.ekod_room_booking.model.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}
