package fr.ekod.cda.ja.ekod_room_booking.repository;

import fr.ekod.cda.ja.ekod_room_booking.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserId(Long userId);
}
