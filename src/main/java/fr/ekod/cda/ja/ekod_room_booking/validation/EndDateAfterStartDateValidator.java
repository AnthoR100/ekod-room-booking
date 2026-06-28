package fr.ekod.cda.ja.ekod_room_booking.validation;

import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EndDateAfterStartDateValidator
        implements ConstraintValidator<EndDateAfterStartDate, ReservationRequestDto> {

    @Override
    public boolean isValid(ReservationRequestDto dto, ConstraintValidatorContext context) {
        if (dto.getStartDateTime() == null || dto.getEndDateTime() == null) {
            return true;
        }
        return dto.getEndDateTime().isAfter(dto.getStartDateTime());
    }
}
