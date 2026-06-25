package fr.ekod.cda.ja.ekod_room_booking.controller;

import fr.ekod.cda.ja.ekod_room_booking.service.ReservationService;
import fr.ekod.cda.ja.ekod_room_booking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    private void addCommonAttributes(Model model, String activePage) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("isAdmin", false); // TODO: remplacer par le rôle réel de l'utilisateur connecté
    }

    @GetMapping("/")
    public String home(Model model) {
        addCommonAttributes(model, "home");
        model.addAttribute("availableRooms", roomService.findAvailable());
        model.addAttribute("totalRooms", roomService.findAll().size());
        model.addAttribute("activeReservations", 0);
        model.addAttribute("occupancyRate", 0);
        return "index";
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        addCommonAttributes(model, "rooms");
        model.addAttribute("rooms", roomService.findAll());
        return "rooms/list";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model, "rooms");
        model.addAttribute("room", roomService.findById(id));
        return "rooms/detail";
    }

    @GetMapping("/me/reservations")
    public String myReservations(Model model) {
        addCommonAttributes(model, "reservations");
        model.addAttribute("reservations", List.of());
        return "reservations/my-reservations";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@RequestParam(required = false) String filter, Model model) {
        addCommonAttributes(model, "admin");
        var allReservations = reservationService.findAll();
        var filtered = filter != null && !filter.equals("all")
                ? allReservations.stream().filter(r -> r.getStatus().name().equals(filter)).toList()
                : allReservations;
        model.addAttribute("reservations", filtered);
        model.addAttribute("currentFilter", filter);
        model.addAttribute("totalRooms", roomService.findAll().size());
        model.addAttribute("activeReservations", allReservations.stream()
                .filter(r -> r.getStatus().name().equals("CONFIRMED") || r.getStatus().name().equals("PENDING"))
                .count());
        model.addAttribute("pendingCount", allReservations.stream()
                .filter(r -> r.getStatus().name().equals("PENDING")).count());
        model.addAttribute("occupancyRate", 0);
        return "admin/dashboard";
    }
}
