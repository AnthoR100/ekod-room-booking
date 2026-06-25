package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.exception.InvalidTokenException;
import fr.ekod.cda.ja.ekod_room_booking.model.RefreshToken;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public RefreshToken generate(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalide."));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expiré.");
        }

        return refreshToken;
    }

    @Transactional
    public RefreshToken rotate(RefreshToken oldToken) {
        refreshTokenRepository.delete(oldToken);
        return generate(oldToken.getUser());
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }
}
