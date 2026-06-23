package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
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
}
