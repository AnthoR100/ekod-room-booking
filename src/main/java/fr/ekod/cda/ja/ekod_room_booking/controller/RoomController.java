package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.file.RoomFileResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomFileService;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomService;
import fr.ekod.cda.ja.ekod_room_booking.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final StorageService storageService;
    private final RoomFileService roomFileService;

    public RoomController(RoomService roomService, StorageService storageService, RoomFileService roomFileService) {
        this.roomService = roomService;
        this.storageService = storageService;
        this.roomFileService = roomFileService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDto>> findAll() {
        return ResponseEntity.ok(roomService.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponseDto>> findAvailable() {
        return ResponseEntity.ok(roomService.findAvailable());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponseDto>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(roomService.search(name, description));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoomResponseDto> create(@Valid @RequestBody RoomRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDto> update(@PathVariable Long id, @Valid @RequestBody RoomRequestDto dto) {
        return ResponseEntity.ok(roomService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles() {
        return ResponseEntity.ok(storageService.list());
    }

    @GetMapping("/files/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String key) throws IOException {
        ResponseInputStream<GetObjectResponse> response = storageService.download(key);
        String contentType = response.response().contentType();
        String filename = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(response.readAllBytes());
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadStandaloneImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = roomFileService.uploadStandaloneImage(file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = roomFileService.uploadImage(id, file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<RoomFileResponseDto>> listRoomFiles(@PathVariable Long id) {
        return ResponseEntity.ok(roomFileService.listByRoom(id));
    }

    @PostMapping("/{id}/files")
    public ResponseEntity<RoomFileResponseDto> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomFileService.uploadDocument(id, file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteRoomFile(
            @PathVariable Long id,
            @PathVariable Long fileId) {
        roomFileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
}
