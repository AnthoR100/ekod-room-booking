package fr.ekod.cda.ja.ekod_room_booking.scheduler;

import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RoomAvailabilityScheduler {

    private final RoomRepository roomRepository;

    @Scheduled(fixedRateString = "${scheduler.room-availability.rate-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        var rooms = roomRepository.findRoomsWithNoActiveReservation(LocalDateTime.now());
        if (rooms.isEmpty()) return;
        rooms.forEach(r -> r.setAvailable(true));
        roomRepository.saveAll(rooms);
    }
}
