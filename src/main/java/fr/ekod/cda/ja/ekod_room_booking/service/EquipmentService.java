package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.equipment.EquipmentRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.equipment.EquipmentResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceAlreadyExistsException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.mapper.EquipmentMapper;
import fr.ekod.cda.ja.ekod_room_booking.model.Equipment;
import fr.ekod.cda.ja.ekod_room_booking.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;


    @Transactional(readOnly = true)
    public EquipmentResponseDto findById(Long id) {
        return equipmentRepository.findById(id)
                .map(equipmentMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement", id));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponseDto>  findAll() {
        return equipmentRepository.findAll()
                .stream()
                .map(equipmentMapper::toResponseDto)
                .toList();
    }


@Transactional
    public EquipmentResponseDto create(EquipmentRequestDto dto) {
        if (equipmentRepository.existsByName(dto.getName())) {
            throw new ResourceAlreadyExistsException("Un équipement avec ce nom existe déjà");
        }

    Equipment equipment = equipmentMapper.toEntity(dto);

    return equipmentMapper.toResponseDto(equipmentRepository.save(equipment));

}

@Transactional
public EquipmentResponseDto update(Long id, EquipmentRequestDto dto) {
    Equipment equipment =  equipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Équipement", id));

    if (!equipment.getName().equals(dto.getName()) && equipmentRepository.existsByName(dto.getName())) {
        throw new ResourceAlreadyExistsException("Un équipement avec ce nom existe déjà");
    }

    equipment.setName(dto.getName());

    return equipmentMapper.toResponseDto(equipmentRepository.save(equipment));

}

    @Transactional
        public void delete(Long id) {
            if (!equipmentRepository.existsById((id))) {
                throw  new ResourceNotFoundException("Équipement", id);
            }
            equipmentRepository.deleteById(id);
    }



}
