package fr.ekod.cda.ja.ekod_room_booking.dto.reservation;

import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ReservationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationResponseDto {

    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private ReservationStatus status;
    private String purpose;
    private int numberOfPeople;
    private UserResponseDto user;
    private RoomResponseDto room;
    private LocalDateTime createdAt;
}
