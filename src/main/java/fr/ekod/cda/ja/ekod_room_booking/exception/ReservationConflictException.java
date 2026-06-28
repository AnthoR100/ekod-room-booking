package fr.ekod.cda.ja.ekod_room_booking.exception;

public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        //super("Ce créneau est déjà réservé pour cette salle") ;
        super(message);
    }
}
