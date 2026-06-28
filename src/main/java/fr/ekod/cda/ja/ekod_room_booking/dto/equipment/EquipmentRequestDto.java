package fr.ekod.cda.ja.ekod_room_booking.dto.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentRequestDto {

    @NotBlank
    @Size(max = 100)
    private String name;
}
