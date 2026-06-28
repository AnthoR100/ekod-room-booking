package fr.ekod.cda.ja.ekod_room_booking.dto.room;

import fr.ekod.cda.ja.ekod_room_booking.dto.equipment.EquipmentResponseDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomResponseDto {

    private Long id;
    private String name;
    private String description;
    private int capacity;
    private String location;
    private boolean available;
    private String imageUrl;
    private LocalDateTime createdAt;
    private List<EquipmentResponseDto> equipment;
}
