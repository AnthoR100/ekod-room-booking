package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
}
