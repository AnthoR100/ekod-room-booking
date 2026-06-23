package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
}
