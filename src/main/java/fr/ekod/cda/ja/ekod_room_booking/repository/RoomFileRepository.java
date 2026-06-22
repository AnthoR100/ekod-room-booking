package fr.ekod.cda.ja.ekod_room_booking.repository;

import fr.ekod.cda.ja.ekod_room_booking.model.RoomFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomFileRepository extends JpaRepository<RoomFile, Long> {

    List<RoomFile> findByRoomId(Long roomId);
}
