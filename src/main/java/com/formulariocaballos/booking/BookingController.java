package com.formulariocaballos.booking;

import com.formulariocaballos.booking.dto.BookingResponse;
import com.formulariocaballos.booking.dto.CreateBookingRequest;
import com.formulariocaballos.booking.dto.UpdateBookingStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) { this.service = service; }

    @PostMapping
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request, Authentication auth) {
        return service.create(auth.getName(), request);
    }

    @GetMapping("/me")
    public List<BookingResponse> mine(Authentication auth) { return service.mine(auth.getName()); }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication auth) { return service.cancel(auth.getName(), id); }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> all() { return service.all(); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse status(@PathVariable Long id, @Valid @RequestBody UpdateBookingStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
