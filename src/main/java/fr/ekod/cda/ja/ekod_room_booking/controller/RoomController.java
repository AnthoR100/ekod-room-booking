package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
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

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final StorageService storageService;

    public RoomController(RoomService roomService, StorageService storageService) {
        this.roomService = roomService;
        this.storageService = storageService;
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

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String key = file.getOriginalFilename();
        storageService.upload(key, file);
        return ResponseEntity.ok("Fichier uploadé : " + key);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles() throws Exception {
        return ResponseEntity.ok(storageService.list());
    }

    @GetMapping("/files/{key}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String key) throws IOException {
        ResponseInputStream<GetObjectResponse> response = storageService.download(key);
        String contentType = response.response().contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(response.readAllBytes());
    }

    @DeleteMapping("/files/{key}")
    public ResponseEntity<Void> deleteFile(@PathVariable String key) {
        storageService.delete(key);
        return ResponseEntity.noContent().build();
    }
}
