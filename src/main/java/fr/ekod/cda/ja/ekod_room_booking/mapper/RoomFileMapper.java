package fr.ekod.cda.ja.ekod_room_booking.mapper;

import fr.ekod.cda.ja.ekod_room_booking.dto.file.RoomFileResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.RoomFile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomFileMapper {

    RoomFileResponseDto toResponseDto(RoomFile file);
}
