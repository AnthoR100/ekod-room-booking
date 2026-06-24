package fr.ekod.cda.ja.ekod_room_booking.repository;

import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByAvailableTrue();

    boolean existsByName(String name);

    List<Room> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    @Query("""
            SELECT r FROM Room r
            WHERE r.available = false
            AND NOT EXISTS (
                SELECT res FROM Reservation res
                WHERE res.room = r
                AND res.status = 'CONFIRMED'
                AND res.startDateTime <= :now
                AND res.endDateTime >= :now
            )
            """)
    List<Room> findRoomsWithNoActiveReservation(@Param("now") LocalDateTime now);
}
