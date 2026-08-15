package com.formulariocaballos.booking;

import com.formulariocaballos.booking.dto.BookingResponse;
import com.formulariocaballos.booking.dto.CreateBookingRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.customer.SpanishPhoneNumber;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.exception.ResourceNotFoundException;
import com.formulariocaballos.experience.Experience;
import com.formulariocaballos.experience.ExperienceRepository;
import com.formulariocaballos.notification.NotificationService;
import com.formulariocaballos.payment.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final CustomerUserRepository users;
    private final ExperienceRepository experiences;
    private final PaymentService payments;
    private final NotificationService notifications;

    public BookingService(BookingRepository bookings, CustomerUserRepository users,
                          ExperienceRepository experiences, PaymentService payments,
                          NotificationService notifications) {
        this.bookings = bookings;
        this.users = users;
        this.experiences = experiences;
        this.payments = payments;
        this.notifications = notifications;
    }

    @Transactional
    public BookingResponse create(String email, CreateBookingRequest request) {
        CustomerUser user = user(email);
        if (request.dateKey().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede reservar una fecha pasada");
        }
        Experience experience = experiences.findById(request.experienceId())
            .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        if (!Boolean.TRUE.equals(experience.getActive())) throw new BusinessException("Experience is not active");
        if (bookings.existsByExperienceIdAndDateKeyAndHourAndStatusNot(
            experience.getId(), request.dateKey(), request.hour(), ReservationStatus.CANCELLED)) {
            throw new BusinessException("That booking slot is already reserved");
        }

        Booking booking = new Booking();
        booking.setId(System.currentTimeMillis());
        booking.setUser(user);
        booking.setExperience(experience);
        booking.setType(experience.getType());
        booking.setTitle(experience.getTitle());
        booking.setDate(request.date() == null ? request.dateKey().toString() : request.date());
        booking.setDateKey(request.dateKey());
        booking.setHour(request.hour());
        booking.setPayment(request.payment() == null ? "mock" : request.payment());
        booking.setCustomerName(request.customerName() == null
            ? user.getFirstName() + " " + user.getLastName() : request.customerName());
        booking.setPhone(request.phone() == null ? user.getPhone() : SpanishPhoneNumber.normalize(request.phone()));
        booking.setAmount(experience.getPrice());
        booking.setPaymentStatus(payments.charge(experience.getPrice(), booking.getPayment()));
        if (booking.getPaymentStatus() != PaymentStatus.APPROVED) throw new BusinessException("Payment declined");
        booking.setStatus(ReservationStatus.CONFIRMED);
        booking = bookings.save(booking);
        notifications.bookingCreated(booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(String email) {
        return bookings.findByUserIdOrderByCreatedAtDesc(user(email).getId()).stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse cancel(String email, Long id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) throw new BusinessException("Booking does not belong to user");
        if (booking.getStatus() == ReservationStatus.CANCELLED) return BookingResponse.from(booking);
        booking.setStatus(ReservationStatus.CANCELLED);
        booking = bookings.save(booking);
        notifications.bookingCancelled(booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> all() { return bookings.findAll().stream().map(BookingResponse::from).toList(); }

    @Transactional
    public BookingResponse updateStatus(Long id, ReservationStatus status) {
        Booking booking = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setStatus(status);
        booking = bookings.save(booking);
        if (status == ReservationStatus.CANCELLED) notifications.bookingCancelled(booking);
        return BookingResponse.from(booking);
    }

    private CustomerUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
