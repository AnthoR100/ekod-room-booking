package fr.ekod.cda.ja.ekod_room_booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
public @interface EndDateAfterStartDate {
    String message() default "La date de fin doit être après la date de début";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
