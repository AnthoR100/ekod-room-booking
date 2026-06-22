package fr.ekod.cda.ja.ekod_room_booking.mapper;

import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, RoomMapper.class})
public interface ReservationMapper {

    ReservationResponseDto toResponseDto(Reservation reservation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "room", ignore = true)
    Reservation toEntity(ReservationRequestDto dto);
}
