package fr.ekod.cda.ja.ekod_room_booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.ekod.cda.ja.ekod_room_booking.repository.RefreshTokenRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.ReservationRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Autowired WebApplicationContext context;

    MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired UserRepository userRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private String userToken;
    private String adminToken;

    private static final String USER_EMAIL   = "test-user@integration.test";
    private static final String ADMIN_EMAIL  = "test-admin@integration.test";
    private static final String PASSWORD     = "testPassword123";
    private static final String ADMIN_SECRET = "test-admin-secret";

    @BeforeAll
    void setUpMvcAndAccounts() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Test","lastName":"User",
                         "email":"%s","password":"%s"}
                        """.formatted(USER_EMAIL, PASSWORD)));

        mockMvc.perform(post("/api/auth/create-admin")
                .header("X-Admin-Secret", ADMIN_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Test","lastName":"Admin",
                         "email":"%s","password":"%s","role":"ROLE_ADMIN"}
                        """.formatted(ADMIN_EMAIL, PASSWORD)));

        userToken  = getToken(USER_EMAIL,  PASSWORD);
        adminToken = getToken(ADMIN_EMAIL, PASSWORD);
    }

    @AfterEach
    void cleanTestRoomsAndReservations() {
        roomRepository.findAll().stream()
                .filter(r -> r.getName().startsWith("TEST"))
                .forEach(room -> {
                    reservationRepository.findByRoomId(room.getId())
                            .forEach(reservationRepository::delete);
                    roomRepository.delete(room);
                });
    }

    @AfterAll
    void cleanTestUsers() {
        for (String email : new String[]{USER_EMAIL, ADMIN_EMAIL}) {
            userRepository.findByEmail(email).ifPresent(u -> {
                refreshTokenRepository.findAll().stream()
                        .filter(t -> t.getUser().getId().equals(u.getId()))
                        .forEach(refreshTokenRepository::delete);
                reservationRepository.findByUserId(u.getId())
                        .forEach(reservationRepository::delete);
                userRepository.delete(u);
            });
        }
    }

    private String getToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @Order(1)
    void getRooms_returns200_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void createRoom_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TEST-Salle","capacity":10}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void createRoom_returns403_whenUser() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TEST-Salle","capacity":10}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void createRoom_returns201_whenAdmin() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TEST-Salle-Admin","capacity":15,"available":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TEST-Salle-Admin"))
                .andExpect(jsonPath("$.capacity").value(15));
    }

    @Test
    @Order(5)
    void createRoom_returns400_whenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","capacity":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void getMyReservations_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void getMyReservations_returns200_whenUser() throws Exception {
        mockMvc.perform(get("/api/reservations/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    void getAllReservations_returns403_whenUser() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void getAllReservations_returns200_whenAdmin() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    void createReservation_returns201_whenUser() throws Exception {
        String roomBody = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TEST-Reservation-Room","capacity":20,"available":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long roomId = objectMapper.readTree(roomBody).get("id").asLong();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end   = start.plusHours(2);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "numberOfPeople": 5,
                                  "purpose": "Réunion TEST"
                                }
                                """.formatted(roomId, start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.numberOfPeople").value(5));
    }

    @Test
    @Order(11)
    void register_returns400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Zak","lastName":"Test",
                                 "email":"pas-un-email","password":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    void register_returns400_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Zak","lastName":"Test",
                                 "email":"valid@test.com","password":"court"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
