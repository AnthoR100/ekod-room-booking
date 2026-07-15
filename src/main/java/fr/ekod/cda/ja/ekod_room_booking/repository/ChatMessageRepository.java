package fr.ekod.cda.ja.ekod_room_booking.repository;

import fr.ekod.cda.ja.ekod_room_booking.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage>findTop10ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
