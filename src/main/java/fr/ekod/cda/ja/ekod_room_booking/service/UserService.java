package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserProfileUpdateDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.mapper.UserMapper;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.repository.ReservationRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(User user) {
        return userMapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateProfile(User user, UserProfileUpdateDto dto) {
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        return userMapper.toResponseDto(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        reservationRepository.deleteAll(reservationRepository.findByUserId(id));
        userRepository.delete(user);
    }
}
