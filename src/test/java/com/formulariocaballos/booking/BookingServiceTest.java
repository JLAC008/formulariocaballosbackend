package com.formulariocaballos.booking;

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
        service = new BookingService(bookings, users, experiences, payments, notifications);
        user = new CustomerUser();
        user.setId(7L);
        user.setEmail("rider@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setPhone("+34600000000");
        experience = new Experience();
        experience.setId(2L);
        experience.setTitle("Trail ride");
        experience.setType("routes");
        experience.setPrice(new BigDecimal("25.00"));
        experience.setActive(true);
    }

    @Test
    void createsApprovedBookingAndNotifies() {
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        when(bookings.countByExperienceIdAndDateKeyAndHourAndStatusNot(any(), any(), any(), any())).thenReturn(0L);
        when(payments.charge(any(), any())).thenReturn(PaymentStatus.APPROVED);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("rider@example.com", request);

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(notifications).bookingCreated(any(Booking.class));
    }

    @Test
    void rejectsUserBookingAnotherExperienceAtSameDateAndHourBeforeCharging() {
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null);
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
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.countByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(8L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("aforo completo");
        verifyNoInteractions(payments);
    }

    @Test
    void rejectsFullLessonAtFiveBookingsBeforeCharging() {
        experience.setType("lessons");
        CreateBookingRequest request = new CreateBookingRequest(2L, LocalDate.of(2026, 9, 1), "11:00", null, null, null, null);
        when(users.findByEmailIgnoreCase("rider@example.com")).thenReturn(Optional.of(user));
        when(experiences.findById(2L)).thenReturn(Optional.of(experience));
        when(bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(7L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(false);
        when(bookings.countByExperienceIdAndDateKeyAndHourAndStatusNot(2L, request.dateKey(), request.hour(), ReservationStatus.CANCELLED)).thenReturn(5L);

        assertThatThrownBy(() -> service.create("rider@example.com", request))
            .isInstanceOf(com.formulariocaballos.exception.BusinessException.class)
            .hasMessageContaining("aforo completo");
        verifyNoInteractions(payments);
    }
}
