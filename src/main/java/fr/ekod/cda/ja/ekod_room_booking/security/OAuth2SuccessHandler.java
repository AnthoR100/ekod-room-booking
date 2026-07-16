package fr.ekod.cda.ja.ekod_room_booking.security;

import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.Role;
import fr.ekod.cda.ja.ekod_room_booking.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email     = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName  = oAuth2User.getAttribute("family_name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .firstName(firstName != null && !firstName.isEmpty() ? firstName : email.split("@")[0])
                    .lastName(lastName   != null ? lastName : "")
                    .role(Role.ROLE_USER)
                    .build();
            return userRepository.save(newUser);
        });

        String token = jwtService.generateToken(user);

        Cookie cookie = new Cookie("accessToken", token);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(jwtExpiration / 1000);
        response.addCookie(cookie);

        request.getSession().invalidate();

        getRedirectStrategy().sendRedirect(request, response, "/oauth2/success");
    }
}
