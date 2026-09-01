package com.formulariocaballos.booking;

import com.formulariocaballos.booking.dto.BlockedDateRequest;
import com.formulariocaballos.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/blocked-dates")
public class BlockedBookingDateController {
    private final BlockedBookingDateRepository blockedDates;
    private final BookingRepository bookings;

    public BlockedBookingDateController(BlockedBookingDateRepository blockedDates, BookingRepository bookings) {
        this.blockedDates = blockedDates;
        this.bookings = bookings;
    }

    @GetMapping
    public List<LocalDate> list() {
        return blockedDates.findAllByOrderByDateKeyAsc().stream()
            .map(BlockedBookingDate::getDateKey)
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<LocalDate> add(@Valid @RequestBody BlockedDateRequest request) {
        LocalDate dateKey = request.dateKey();
        if (dateKey.isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede bloquear una fecha pasada.");
        }
        if (bookings.existsByDateKeyAndStatus(dateKey, ReservationStatus.CONFIRMED)) {
            throw new BusinessException("No se puede bloquear ese día porque ya tiene reservas confirmadas.");
        }
        blockedDates.save(new BlockedBookingDate(dateKey));
        return list();
    }

    @DeleteMapping("/{dateKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LocalDate> remove(@PathVariable LocalDate dateKey) {
        blockedDates.deleteById(dateKey);
        return list();
    }
}
