package fr.ekod.cda.ja.ekod_room_booking.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException() {
        super("Type de fichier non autorisé. Seuls les images et les PDF sont acceptés.");
    }
}
