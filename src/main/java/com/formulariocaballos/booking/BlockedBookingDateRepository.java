package com.formulariocaballos.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BlockedBookingDateRepository extends JpaRepository<BlockedBookingDate, LocalDate> {
    List<BlockedBookingDate> findAllByOrderByDateKeyAsc();
}
