package com.formulariocaballos.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByExperienceIdAndDateKeyAndHourAndStatusNot(
        Long experienceId, java.time.LocalDate dateKey, String hour, ReservationStatus status);

    boolean existsByUserIdAndDateKeyAndHourAndStatusNot(
        Long userId, java.time.LocalDate dateKey, String hour, ReservationStatus status);
}
