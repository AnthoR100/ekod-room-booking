package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceAlreadyExistsException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.mapper.RoomMapper;
import fr.ekod.cda.ja.ekod_room_booking.model.Equipment;
import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import fr.ekod.cda.ja.ekod_room_booking.repository.EquipmentRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public RoomResponseDto findById(Long id) {
        return roomRepository.findById(id)
                .map(roomMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Salle", id));
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDto> findAll() {
        return roomRepository.findAll()
                .stream()
                .map(roomMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDto> findAvailable() {
        return roomRepository.findByAvailableTrue()
                .stream()
                .map(roomMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDto> search(String name, String description) {
        return roomRepository.findByNameLikeIgnoreCaseOrDescriptionLikeIgnoreCase(name, description)
                .stream()
                .map(roomMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public RoomResponseDto create(RoomRequestDto dto) {
        if (roomRepository.existsByName(dto.getName())) {
            throw new ResourceAlreadyExistsException("Une salle avec ce nom existe déjà");
        }

        Room room = roomMapper.toEntity(dto);

        if (dto.getEquipmentIds() != null && !dto.getEquipmentIds().isEmpty()) {
            List<Equipment> equipment = equipmentRepository.findByIdIn(dto.getEquipmentIds());
            room.setEquipment(equipment);
        }

        return roomMapper.toResponseDto(roomRepository.save(room));
    }

    @Transactional
    public RoomResponseDto update(Long id, RoomRequestDto dto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle", id));

        if (!room.getName().equals(dto.getName()) && roomRepository.existsByName(dto.getName())) {
            throw new ResourceAlreadyExistsException("Une salle avec ce nom existe déjà");
        }

        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());
        room.setAvailable(dto.isAvailable());
        room.setImageUrl(dto.getImageUrl());

        if (dto.getEquipmentIds() != null) {
            List<Equipment> equipment = equipmentRepository.findByIdIn(dto.getEquipmentIds());
            room.setEquipment(equipment);
        }

        return roomMapper.toResponseDto(roomRepository.save(room));
    }

    @Transactional
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Salle", id);
        }
        roomRepository.deleteById(id);
    }
}
