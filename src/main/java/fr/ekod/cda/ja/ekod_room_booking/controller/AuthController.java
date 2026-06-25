package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.auth.AuthResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.auth.LoginRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.auth.RefreshRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.auth.RegisterRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequestDto dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDto dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
