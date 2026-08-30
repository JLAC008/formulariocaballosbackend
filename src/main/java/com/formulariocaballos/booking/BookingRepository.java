package com.formulariocaballos.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByExperienceIdAndDateKeyAndHourAndStatusNot(
        Long experienceId, java.time.LocalDate dateKey, String hour, ReservationStatus status);

    @Query("""
        select coalesce(sum(b.participantCount), 0)
        from Booking b
        where b.experience.id = :experienceId
          and b.dateKey = :dateKey
          and b.hour = :hour
          and b.status <> :status
        """)
    long sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(
        @Param("experienceId") Long experienceId,
        @Param("dateKey") java.time.LocalDate dateKey,
        @Param("hour") String hour,
        @Param("status") ReservationStatus status);

    boolean existsByUserIdAndDateKeyAndHourAndStatusNot(
        Long userId, java.time.LocalDate dateKey, String hour, ReservationStatus status);
}
