package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.file.RoomFileResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import fr.ekod.cda.ja.ekod_room_booking.model.RoomFile;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomFileRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomFileService {

    static final String DOWNLOAD_PREFIX = "/api/rooms/files/download?key=";

    private final RoomRepository roomRepository;
    private final RoomFileRepository roomFileRepository;
    private final StorageService storageService;

    public String uploadStandaloneImage(MultipartFile file) throws IOException {
        String key = "images/" + UUID.randomUUID() + ext(file.getOriginalFilename());
        storageService.upload(key, file);
        return DOWNLOAD_PREFIX + key;
    }

    @Transactional
    public String uploadImage(Long roomId, MultipartFile file) throws IOException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
        String key = "rooms/" + roomId + "/image/" + UUID.randomUUID() + ext(file.getOriginalFilename());
        storageService.upload(key, file);
        String imageUrl = DOWNLOAD_PREFIX + key;
        room.setImageUrl(imageUrl);
        roomRepository.save(room);
        return imageUrl;
    }

    @Transactional
    public RoomFileResponseDto uploadDocument(Long roomId, MultipartFile file) throws IOException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
        String key = "rooms/" + roomId + "/files/" + UUID.randomUUID() + ext(file.getOriginalFilename());
        storageService.upload(key, file);
        RoomFile roomFile = RoomFile.builder()
                .filename(file.getOriginalFilename())
                .storedPath(key)
                .contentType(file.getContentType())
                .size(file.getSize())
                .room(room)
                .build();
        return toDto(roomFileRepository.save(roomFile));
    }

    public List<RoomFileResponseDto> listByRoom(Long roomId) {
        return roomFileRepository.findByRoomId(roomId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        RoomFile f = roomFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable"));
        storageService.delete(f.getStoredPath());
        roomFileRepository.delete(f);
    }

    private RoomFileResponseDto toDto(RoomFile f) {
        RoomFileResponseDto dto = new RoomFileResponseDto();
        dto.setId(f.getId());
        dto.setFilename(f.getFilename());
        dto.setContentType(f.getContentType());
        dto.setSize(f.getSize());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setDownloadUrl(DOWNLOAD_PREFIX + f.getStoredPath());
        return dto;
    }

    private String ext(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
