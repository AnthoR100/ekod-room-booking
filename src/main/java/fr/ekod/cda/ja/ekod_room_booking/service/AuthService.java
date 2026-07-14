package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.auth.AuthResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.auth.LoginRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.auth.RegisterRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceAlreadyExistsException;
import fr.ekod.cda.ja.ekod_room_booking.model.RefreshToken;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.Role;
import fr.ekod.cda.ja.ekod_room_booking.repository.UserRepository;
import fr.ekod.cda.ja.ekod_room_booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Un compte avec cet email existe déjà");
        }

        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        return buildResponse(user);
    }

    @Transactional
    public AuthResponseDto createAdmin(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Un compte avec cet email existe déjà");
        }
        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .build();
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow();

        return buildResponse(user);
    }

    @Transactional
    public AuthResponseDto refresh(String refreshToken) {
        RefreshToken token = refreshTokenService.validate(refreshToken);
        User user = token.getUser();
        RefreshToken newToken = refreshTokenService.rotate(token);
        return new AuthResponseDto(
                jwtService.generateToken(user),
                newToken.getToken(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponseDto buildResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.generate(user);
        return new AuthResponseDto(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
