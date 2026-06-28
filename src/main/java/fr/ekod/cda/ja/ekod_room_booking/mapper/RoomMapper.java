package fr.ekod.cda.ja.ekod_room_booking.mapper;

import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EquipmentMapper.class})
public interface RoomMapper {

    RoomResponseDto toResponseDto(Room room);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "equipment", ignore = true)
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Room toEntity(RoomRequestDto dto);
}
