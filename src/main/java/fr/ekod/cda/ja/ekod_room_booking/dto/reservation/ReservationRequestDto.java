package fr.ekod.cda.ja.ekod_room_booking.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequestDto {

    @NotNull
    @Future
    private LocalDateTime startDateTime;

    @NotNull
    @Future
    private LocalDateTime endDateTime;

    @Size(max = 255)
    private String purpose;

    @NotNull
    @Min(1)
    private Integer numberOfPeople;

    @NotNull
    private Long roomId;
}
