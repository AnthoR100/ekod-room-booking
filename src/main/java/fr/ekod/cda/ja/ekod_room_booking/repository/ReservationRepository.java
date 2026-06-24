package fr.ekod.cda.ja.ekod_room_booking.repository;

import fr.ekod.cda.ja.ekod_room_booking.model.Reservation;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByRoomId(Long roomId);

    List<Reservation> findByStatus(ReservationStatus status);

    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.room.id = :roomId
            AND r.status = 'CONFIRMED'
            AND r.startDateTime < :endDateTime
            AND r.endDateTime > :startDateTime
            """)
    boolean existsOverlap(
            @Param("roomId") Long roomId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.room.id = :roomId
            AND r.status = 'CONFIRMED'
            AND r.startDateTime <= :now
            AND r.endDateTime >= :now
            """)
    boolean existsActiveConfirmedReservation(
            @Param("roomId") Long roomId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.room.id = :roomId
            AND r.id <> :excludedId
            AND r.status = 'PENDING'
            AND r.startDateTime < :endDateTime
            AND r.endDateTime > :startDateTime
            """)
    List<Reservation> findOverlappingPending(
            @Param("roomId") Long roomId,
            @Param("excludedId") Long excludedId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
