package fr.ekod.cda.ja.ekod_room_booking.mapper;

import fr.ekod.cda.ja.ekod_room_booking.dto.equipment.EquipmentRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.equipment.EquipmentResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.Equipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    EquipmentResponseDto toResponseDto(Equipment equipment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    Equipment toEntity(EquipmentRequestDto dto);
}
