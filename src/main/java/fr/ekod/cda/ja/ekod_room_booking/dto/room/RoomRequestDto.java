package fr.ekod.cda.ja.ekod_room_booking.dto.room;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RoomRequestDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer capacity;

    @Size(max = 100)
    private String location;

    private boolean available = true;

    @Size(max = 500)
    private String imageUrl;

    private List<Long> equipmentIds;
}
