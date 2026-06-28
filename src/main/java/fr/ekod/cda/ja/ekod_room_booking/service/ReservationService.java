package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.InvalidReservationException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ReservationConflictException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import fr.ekod.cda.ja.ekod_room_booking.mapper.ReservationMapper;
import fr.ekod.cda.ja.ekod_room_booking.model.Reservation;
import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ReservationStatus;
import fr.ekod.cda.ja.ekod_room_booking.repository.ReservationRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final RoomRepository roomRepository;


    //create
    @Transactional
    public ReservationResponseDto create(ReservationRequestDto dto, User user) {

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle", dto.getRoomId()));

        if (!room.isAvailable()){
            throw new ReservationConflictException("Cette salle n'est pas disponible");
        }

        if (reservationRepository.existsOverlap(dto.getRoomId(), dto.getStartDateTime(), dto.getEndDateTime())) {
            throw new ReservationConflictException("Cette salle est déjà réservée sur ce créneau");
        }

        if (dto.getNumberOfPeople() > room.getCapacity()) {
            throw new InvalidReservationException("Le nombre de personnes dépasse la capacité de la salle");
        }

        Reservation reservation = reservationMapper.toEntity(dto);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.PENDING);

        return reservationMapper.toResponseDto(reservationRepository.save(reservation));

    }


    //findAll
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findAll() {
        return reservationRepository.findAll()
                .stream().map(reservationMapper::toResponseDto).toList();
    }

    //findByCurrentUser
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findByUserId(User user) {
        return reservationRepository.findByUserId(user.getId())
                .stream()
                .map(reservationMapper::toResponseDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public long countByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).size();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status)
                .stream().map(reservationMapper::toResponseDto).toList();
    }

    //findPending
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findPending() {
        return reservationRepository.findByStatus(ReservationStatus.PENDING)
                .stream().map(reservationMapper::toResponseDto).toList();
    }

    //findByRoomId
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findByRoomId(Long roomId) {
        return reservationRepository.findByRoomId(roomId)
                .stream()
                .map(reservationMapper::toResponseDto)
                .toList();
    }

    //findByUserId (admin)
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> findByUserId(Long userId) {
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(reservationMapper::toResponseDto)
                .toList();
    }

    //updateStatus
    @Transactional
    public ReservationResponseDto updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));

        reservation.setStatus(status);

        return reservationMapper.toResponseDto(reservationRepository.save(reservation));
    }

    //cancel
    @Transactional
    public ReservationResponseDto cancel(Long id, User user) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));

        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = reservation.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à annuler cette réservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationException("Cette réservation est déjà annulée");
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            reservation.getRoom().setAvailable(true);
            roomRepository.save(reservation.getRoom());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        return reservationMapper.toResponseDto(reservationRepository.save(reservation));
    }

    //confirm (admin)
    @Transactional
    public ReservationResponseDto confirm(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationException("Seule une réservation en attente peut être confirmée");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.getRoom().setAvailable(false);
        roomRepository.save(reservation.getRoom());

        reservationRepository.rejectOverlappingPending(
                reservation.getRoom().getId(),
                reservation.getId(),
                reservation.getStartDateTime(),
                reservation.getEndDateTime(),
                ReservationStatus.PENDING,
                ReservationStatus.REJECTED
        );

        return reservationMapper.toResponseDto(reservationRepository.save(reservation));
    }

    //reject (admin)
    @Transactional
    public ReservationResponseDto reject(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationException("Seule une réservation en attente peut être rejetée");
        }

        reservation.setStatus(ReservationStatus.REJECTED);

        return reservationMapper.toResponseDto(reservationRepository.save(reservation));
    }

    //delete
    @Transactional
    public void delete(Long id, User user) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation", id));

        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = reservation.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette réservation");
        }

        reservationRepository.deleteById(id);
    }
}
