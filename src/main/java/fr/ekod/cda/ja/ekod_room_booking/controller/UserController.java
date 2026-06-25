package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserProfileUpdateDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileUpdateDto dto) {
        return ResponseEntity.ok(userService.updateProfile(user, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
