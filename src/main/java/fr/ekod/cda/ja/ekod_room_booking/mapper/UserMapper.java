package fr.ekod.cda.ja.ekod_room_booking.mapper;

import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.user.UserResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toResponseDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserRequestDto dto);
}
