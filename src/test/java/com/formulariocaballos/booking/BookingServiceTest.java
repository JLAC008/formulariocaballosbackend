package com.formulariocaballos.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.booking.dto.CreateBookingRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.experience.Experience;
import com.formulariocaballos.experience.ExperienceRepository;
import com.formulariocaballos.notification.NotificationService;
import com.formulariocaballos.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock CustomerUserRepository users;
    @Mock ExperienceRepository experiences;
    @Mock PaymentService payments;
    @Mock NotificationService notifications;
    BookingService service;
    CustomerUser user;
    Experience experience;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, users, experiences, payments, notifications, new ObjectMapper());
        user = new CustomerUser();
        user.setId(7L);
        user.setEmail("rider@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setPhone("+34600000000");
        user.setBonuses(3);
        experience = new Experience();
        experience.setId(2L);
        experience.setTitle("Trail ride");
        experience.setType("routes");
        experience.setCapacity(8);
        experience.setPrice(BigDecimal.ONE);
        experience.setActive(true);
        experience.setHours("[\"11:00\"]");
    }

    @Test
    void createsApprovedBookingAndNotifies() {
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(0L);
        when(payments.charge(any(), any())).thenReturn(PaymentStatus.APPROVED);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("rider@example.com", request);

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.remainingBonuses()).isEqualTo(2);
        assertThat(user.getBonuses()).isEqualTo(2);
        verify(users).save(user);
        verify(notifications).bookingCreated(any(Booking.class));
    }

    @Test
    void rejectsBookingWithoutBonusesBeforeCharging() {
        user.setBonuses(0);
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(0L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("bonos suficientes");
        verifyNoInteractions(payments);
    }

    @Test
    void createsBookingWithConfiguredBonusCost() {
        experience.setPrice(new BigDecimal("2"));
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(0L);
        when(payments.charge(new BigDecimal("2"), "mock")).thenReturn(PaymentStatus.APPROVED);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("rider@example.com", request);

        assertThat(result.amount()).isEqualTo(new BigDecimal("2"));
        assertThat(result.remainingBonuses()).isEqualTo(1);
        assertThat(user.getBonuses()).isEqualTo(1);
    }

    @Test
    void createsBookingWithGuestAndConsumesAdditionalBonusAndCapacity() {
        user.setBonuses(3);
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, 1);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(6L);
        when(payments.charge(new BigDecimal("2"), "mock")).thenReturn(PaymentStatus.APPROVED);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("rider@example.com", request);

        assertThat(result.amount()).isEqualTo(new BigDecimal("2"));
        assertThat(result.participantCount()).isEqualTo(2);
        assertThat(result.guestCount()).isEqualTo(1);
        assertThat(result.remainingBonuses()).isEqualTo(1);
        assertThat(user.getBonuses()).isEqualTo(1);
    }

    @Test
    void refundsBonusWhenCustomerCancelsConfirmedBooking() {
        Booking booking = new Booking();
        booking.setId(20L);
        booking.setUser(user);
        booking.setExperience(experience);
        booking.setType(experience.getType());
        booking.setTitle(experience.getTitle());
        booking.setDateKey(LocalDate.of(2026, 9, 1));
        booking.setHour("11:00");
        booking.setAmount(experience.getPrice());
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setStatus(ReservationStatus.CONFIRMED);
        when(bookings.findById(20L)).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.cancel("rider@example.com", 20L);

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.remainingBonuses()).isEqualTo(4);
        assertThat(user.getBonuses()).isEqualTo(4);
        verify(users).save(user);
        verify(notifications).bookingCancelled(booking);
    }

    @Test
    void rejectsCustomerCancellationInsideThreeHours() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        Booking booking = new Booking();
        booking.setId(22L);
        booking.setUser(user);
        booking.setExperience(experience);
        booking.setType(experience.getType());
        booking.setTitle(experience.getTitle());
        booking.setDateKey(start.toLocalDate());
        booking.setHour(start.format(DateTimeFormatter.ofPattern("HH:mm")));
        booking.setAmount(experience.getPrice());
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setStatus(ReservationStatus.CONFIRMED);
        when(bookings.findById(22L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancel("rider@example.com", 22L))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("3 horas");
        assertThat(user.getBonuses()).isEqualTo(3);
        verify(users, never()).save(user);
        verify(bookings, never()).save(any(Booking.class));
        verify(notifications, never()).bookingCancelled(any(Booking.class));
    }

    @Test
    void doesNotNotifyAgainWhenAlreadyCancelledBookingIsUpdatedToCancelled() {
        Booking booking = new Booking();
        booking.setId(21L);
        booking.setUser(user);
        booking.setExperience(experience);
        booking.setType(experience.getType());
        booking.setTitle(experience.getTitle());
        booking.setDateKey(LocalDate.of(2026, 9, 1));
        booking.setHour("11:00");
        booking.setAmount(experience.getPrice());
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setStatus(ReservationStatus.CANCELLED);
        when(bookings.findById(21L)).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateStatus(21L, ReservationStatus.CANCELLED);

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(notifications, never()).bookingCancelled(any(Booking.class));
    }

    @Test
    void rejectsUserBookingAnotherExperienceAtSameDateAndHourBeforeCharging() {
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("Ya tienes una reserva");
        verifyNoInteractions(payments);
    }

    @Test
    void rejectsFullRouteAtEightBookingsBeforeCharging() {
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(8L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("aforo completo");
        verifyNoInteractions(payments);
    }

    @Test
    void rejectsFullLessonAtFiveBookingsBeforeCharging() {
        experience.setType("lessons");
        experience.setCapacity(5);
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(5L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("aforo completo");
        verifyNoInteractions(payments);
    }

    @Test
    void rejectsBookingAtConfiguredCapacityBeforeCharging() {
        experience.setCapacity(3);
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(3L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("aforo completo");
        verifyNoInteractions(payments);
    }
}
