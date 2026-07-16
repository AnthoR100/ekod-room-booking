package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.dto.reservation.ReservationResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomRequestDto;
import fr.ekod.cda.ja.ekod_room_booking.dto.room.RoomResponseDto;
import fr.ekod.cda.ja.ekod_room_booking.model.User;
import fr.ekod.cda.ja.ekod_room_booking.model.enums.ReservationStatus;
import fr.ekod.cda.ja.ekod_room_booking.service.EquipmentService;
import fr.ekod.cda.ja.ekod_room_booking.service.ReservationService;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomFileService;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final EquipmentService equipmentService;
    private final RoomFileService roomFileService;

    @GetMapping("/")
    public String index(Model model) {
        var allRooms = roomService.findAll();
        long confirmed = reservationService.countByStatus(ReservationStatus.CONFIRMED);
        int occupancy = allRooms.isEmpty() ? 0 : (int) Math.min(confirmed * 100 / allRooms.size(), 100);

        model.addAttribute("totalRooms", allRooms.size());
        model.addAttribute("availableRooms", roomService.findAvailable());
        model.addAttribute("activeReservations", confirmed);
        model.addAttribute("occupancyRate", occupancy);
        model.addAttribute("activePage", "home");
        return "index";
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("activePage", "rooms");
        return "rooms/list";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Model model, Authentication authentication) {
        RoomResponseDto room = roomService.findById(id);
        if (room.getEquipment() == null) {
            room.setEquipment(List.of());
        }
        model.addAttribute("room", room);
        model.addAttribute("roomFiles", roomFileService.listByRoom(id));
        model.addAttribute("confirmedSlots", reservationService.findUpcomingConfirmedByRoomId(id));
        model.addAttribute("isAuthenticated", isLoggedIn(authentication));
        model.addAttribute("activePage", "rooms");
        return "rooms/detail";
    }

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) return "redirect:/";
        model.addAttribute("activePage", "");
        return "auth/login";
    }

    @GetMapping("/oauth2/success")
    public String oauth2Success() {
        return "auth/oauth2-success";
    }

    @GetMapping("/register")
    public String register(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) return "redirect:/";
        model.addAttribute("activePage", "");
        return "auth/register";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("activePage", "");
        return "error/access-denied";
    }

    @GetMapping("/admin/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRooms(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("activePage", "admin");
        return "admin/rooms";
    }

    @GetMapping("/admin/rooms/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRoomNew(Model model) {
        model.addAttribute("roomForm", new RoomRequestDto());
        model.addAttribute("allEquipment", equipmentService.findAll());
        model.addAttribute("roomFiles", List.of());
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", "admin");
        return "admin/room-form";
    }

    @PostMapping("/admin/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRoomCreate(@Valid @ModelAttribute("roomForm") RoomRequestDto dto,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("allEquipment", equipmentService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "admin");
            return "admin/room-form";
        }
        try {
            roomService.create(dto);
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("allEquipment", equipmentService.findAll());
            model.addAttribute("roomFiles", List.of());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "admin");
            return "admin/room-form";
        }
    }

    @GetMapping("/admin/rooms/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRoomEdit(@PathVariable Long id, Model model) {
        RoomResponseDto room = roomService.findById(id);
        RoomRequestDto form = new RoomRequestDto();
        form.setName(room.getName());
        form.setDescription(room.getDescription());
        form.setCapacity(room.getCapacity());
        form.setLocation(room.getLocation());
        form.setAvailable(room.isAvailable());
        form.setImageUrl(room.getImageUrl());
        if (room.getEquipment() != null) {
            form.setEquipmentIds(room.getEquipment().stream()
                    .map(e -> e.getId()).collect(Collectors.toList()));
        }
        model.addAttribute("roomForm", form);
        model.addAttribute("roomId", id);
        model.addAttribute("allEquipment", equipmentService.findAll());
        model.addAttribute("roomFiles", roomFileService.listByRoom(id));
        model.addAttribute("isEdit", true);
        model.addAttribute("activePage", "admin");
        return "admin/room-form";
    }

    @PostMapping("/admin/rooms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRoomUpdate(@PathVariable Long id,
                                  @Valid @ModelAttribute("roomForm") RoomRequestDto dto,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roomId", id);
            model.addAttribute("allEquipment", equipmentService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "admin");
            return "admin/room-form";
        }
        try {
            roomService.update(id, dto);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roomId", id);
            model.addAttribute("allEquipment", equipmentService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "admin");
            return "admin/room-form";
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/admin/rooms/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminRoomDelete(@PathVariable Long id) {
        roomService.delete(id);
        return "redirect:/admin/rooms";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("activePage", "profile");
        model.addAttribute("user", user);
        return "user/profile";
    }

    @GetMapping("/reservations/me")
    public String myReservations(@AuthenticationPrincipal User user, Model model) {
        List<ReservationResponseDto> reservations = reservationService.findByUserId(user);
        model.addAttribute("reservations", reservations);
        model.addAttribute("activePage", "reservations");
        return "reservations/my-reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @AuthenticationPrincipal User user) {
        reservationService.cancel(id, user);
        return "redirect:/reservations/me";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(@RequestParam(required = false) String filter, Model model) {
        var allRooms = roomService.findAll();
        long confirmed = reservationService.countByStatus(ReservationStatus.CONFIRMED);
        long pending = reservationService.countByStatus(ReservationStatus.PENDING);
        int occupancy = allRooms.isEmpty() ? 0 : (int) Math.min(confirmed * 100 / allRooms.size(), 100);

        List<ReservationResponseDto> reservations = (filter != null && !filter.isBlank())
                ? reservationService.findByStatus(ReservationStatus.valueOf(filter))
                : reservationService.findAll();

        model.addAttribute("totalRooms", allRooms.size());
        model.addAttribute("activeReservations", confirmed);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("occupancyRate", occupancy);
        model.addAttribute("reservations", reservations);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("activePage", "admin");
        return "admin/dashboard";
    }

    @PostMapping("/admin/reservations/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminConfirm(@PathVariable Long id) {
        reservationService.confirm(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/reservations/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminReject(@PathVariable Long id) {
        reservationService.reject(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/reservations/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        reservationService.cancel(id, user);
        return "redirect:/admin/dashboard";
    }
}
