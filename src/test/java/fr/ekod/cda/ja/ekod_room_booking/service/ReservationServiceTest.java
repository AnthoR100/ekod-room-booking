package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.exception.InvalidReservationException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ReservationConflictException;
import fr.ekod.cda.ja.ekod_room_booking.exception.ResourceNotFoundException;
import fr.ekod.cda.ja.ekod_room_booking.mapper.ReservationMapper;
import fr.ekod.cda.ja.ekod_room_booking.model.Reservation;
import fr.ekod.cda.ja.ekod_room_booking.model.Room;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ReservationStatus;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.Role;
import fr.ekod.cda.ja.ekod_room_booking.repository.ReservationRepository;
import fr.ekod.cda.ja.ekod_room_booking.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private RoomRepository roomRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Room room;
    private User owner;
    private User admin;
    private User otherUser;
    private ReservationRequestDto dto;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.now().plusDays(1);
        end   = start.plusHours(2);

        room = Room.builder()
                .id(1L)
                .name("Salle Ada")
                .capacity(20)
                .available(true)
                .build();

        owner = User.builder().id(10L).role(Role.ROLE_USER).build();
        admin = User.builder().id(99L).role(Role.ROLE_ADMIN).build();
        otherUser = User.builder().id(42L).role(Role.ROLE_USER).build();

        dto = new ReservationRequestDto();
        dto.setRoomId(1L);
        dto.setStartDateTime(start);
        dto.setEndDateTime(end);
        dto.setNumberOfPeople(10);
    }

    @Test
    void create_throwsResourceNotFoundException_whenRoomNotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(dto, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsReservationConflictException_whenRoomIsUnavailable() {
        room.setAvailable(false);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> reservationService.create(dto, owner))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    void create_throwsReservationConflictException_whenSlotOverlaps() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlap(1L, start, end)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(dto, owner))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("créneau");
    }

    @Test
    void create_throwsInvalidReservationException_whenNumberOfPeopleExceedsCapacity() {
        dto.setNumberOfPeople(room.getCapacity() + 1);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlap(1L, start, end)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.create(dto, owner))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("capacité");
    }

    @Test
    void create_returnsPendingReservation_whenAllRulesPass() {
        Reservation saved = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.PENDING)
                .room(room)
                .user(owner)
                .startDateTime(start)
                .endDateTime(end)
                .numberOfPeople(10)
                .build();
        ReservationResponseDto responseDto = new ReservationResponseDto();
        responseDto.setStatus(ReservationStatus.PENDING);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlap(1L, start, end)).thenReturn(false);
        when(reservationMapper.toEntity(dto)).thenReturn(saved);
        when(reservationRepository.save(any())).thenReturn(saved);
        when(reservationMapper.toResponseDto(saved)).thenReturn(responseDto);

        ReservationResponseDto result = reservationService.create(dto, owner);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository).save(saved);
    }

    @Test
    void cancel_throwsResourceNotFoundException_whenReservationNotFound() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_throwsAccessDeniedException_whenUserIsNeitherOwnerNorAdmin() {
        Reservation reservation = pendingReservation(owner);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(1L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_throwsInvalidReservationException_whenAlreadyCancelled() {
        Reservation reservation = Reservation.builder()
                .id(1L).user(owner).room(room)
                .status(ReservationStatus.CANCELLED)
                .startDateTime(start).endDateTime(end).numberOfPeople(5)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(1L, owner))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("déjà annulée");
    }

    @Test
    void cancel_setsStatusCancelled_whenUserIsOwner() {
        Reservation reservation = pendingReservation(owner);
        ReservationResponseDto responseDto = new ReservationResponseDto();
        responseDto.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(responseDto);

        ReservationResponseDto result = reservationService.cancel(1L, owner);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancel_setsStatusCancelled_whenUserIsAdmin() {
        Reservation reservation = pendingReservation(owner);
        ReservationResponseDto responseDto = new ReservationResponseDto();
        responseDto.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(responseDto);

        ReservationResponseDto result = reservationService.cancel(1L, admin);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancel_releasesRoom_whenCancellingConfirmedReservation() {
        room.setAvailable(false);
        Reservation reservation = Reservation.builder()
                .id(1L).user(owner).room(room)
                .status(ReservationStatus.CONFIRMED)
                .startDateTime(start).endDateTime(end).numberOfPeople(5)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(new ReservationResponseDto());

        reservationService.cancel(1L, owner);

        assertThat(room.isAvailable()).isTrue();
        verify(roomRepository).save(room);
    }

    @Test
    void confirm_throwsInvalidReservationException_whenStatusIsNotPending() {
        Reservation reservation = Reservation.builder()
                .id(1L).user(owner).room(room)
                .status(ReservationStatus.CONFIRMED)
                .startDateTime(start).endDateTime(end).numberOfPeople(5)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(1L))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("attente");
    }

    @Test
    void confirm_setsConfirmedAndBlocksRoom_whenPending() {
        Reservation reservation = pendingReservation(owner);
        ReservationResponseDto responseDto = new ReservationResponseDto();
        responseDto.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(responseDto);

        ReservationResponseDto result = reservationService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(room.isAvailable()).isFalse();
        verify(roomRepository).save(room);
        verify(reservationRepository).rejectOverlappingPending(
                eq(room.getId()), eq(1L), eq(start), eq(end),
                eq(ReservationStatus.PENDING), eq(ReservationStatus.REJECTED));
    }

    @Test
    void reject_throwsInvalidReservationException_whenStatusIsNotPending() {
        Reservation reservation = Reservation.builder()
                .id(1L).user(owner).room(room)
                .status(ReservationStatus.CANCELLED)
                .startDateTime(start).endDateTime(end).numberOfPeople(5)
                .build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.reject(1L))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("attente");
    }

    @Test
    void reject_setsStatusRejected_whenPending() {
        Reservation reservation = pendingReservation(owner);
        ReservationResponseDto responseDto = new ReservationResponseDto();
        responseDto.setStatus(ReservationStatus.REJECTED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(responseDto);

        ReservationResponseDto result = reservationService.reject(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
    }

    private Reservation pendingReservation(User user) {
        return Reservation.builder()
                .id(1L)
                .user(user)
                .room(room)
                .status(ReservationStatus.PENDING)
                .startDateTime(start)
                .endDateTime(end)
                .numberOfPeople(5)
                .build();
    }
}
