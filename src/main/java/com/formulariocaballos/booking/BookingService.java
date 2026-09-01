package com.formulariocaballos.booking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final CustomerUserRepository users;
    private final ExperienceRepository experiences;
    private final PaymentService payments;
    private final NotificationService notifications;
    private final ObjectMapper objectMapper;

    public BookingService(BookingRepository bookings, CustomerUserRepository users,
                          ExperienceRepository experiences, PaymentService payments,
                          NotificationService notifications, ObjectMapper objectMapper) {
        this.bookings = bookings;
        this.users = users;
        this.experiences = experiences;
        this.payments = payments;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingResponse create(String email, CreateBookingRequest request) {
        CustomerUser user = user(email);
        if (request.dateKey().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede reservar una fecha pasada");
        }
        if (isWeekend(request.dateKey())) {
            throw new BusinessException("Las experiencias no están disponibles sábados ni domingos.");
        }
        Experience experience = experiences.findById(request.experienceId())
            .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        if (!Boolean.TRUE.equals(experience.getActive())) throw new BusinessException("Experience is not active");
        if (isFriday(request.dateKey()) && !Boolean.TRUE.equals(experience.getFridayAvailable())) {
            throw new BusinessException("Esta experiencia no está disponible los viernes.");
        }
        if (!experienceHours(experience, request.dateKey()).contains(request.hour())) {
            throw new BusinessException("Esta experiencia no está disponible en esa hora.");
        }
        if (!canCustomerBook(request.dateKey(), request.hour())) {
            throw new BusinessException("No se puede reservar una experiencia cuando faltan 2 horas o menos para que empiece.");
        }
        if (bookings.existsByUserIdAndDateKeyAndHourAndStatusNot(
            user.getId(), request.dateKey(), request.hour(), ReservationStatus.CANCELLED)) {
            throw new BusinessException("Ya tienes una reserva para esa fecha y hora.");
        }
        int participantCount = participantCount(request);
        long occupiedPlaces = bookings.sumParticipantsByExperienceIdAndDateKeyAndHourAndStatusNot(
            experience.getId(), request.dateKey(), request.hour(), ReservationStatus.CANCELLED);
        if (occupiedPlaces + participantCount > capacityFor(experience)) {
            throw new BusinessException("Esta hora ya tiene el aforo completo.");
        }
        int bonusCost = bonusCostFor(experience) * participantCount;
        if (currentBonuses(user) < bonusCost) {
            throw new BusinessException("No tienes sesiones suficientes.");
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
        booking.setAmount(BigDecimal.valueOf(bonusCost));
        booking.setParticipantCount(participantCount);
        booking.setPaymentStatus(payments.charge(BigDecimal.valueOf(bonusCost), booking.getPayment()));
        if (booking.getPaymentStatus() != PaymentStatus.APPROVED) throw new BusinessException("Payment declined");
        booking.setStatus(ReservationStatus.CONFIRMED);
        user.setBonuses(currentBonuses(user) - bonusCost);
        users.save(user);
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
        if (!canCustomerCancel(booking)) {
            throw new BusinessException("No se puede cancelar una reserva cuando faltan 2 horas o menos para que empiece.");
        }
        refundBonus(booking);
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
        boolean newlyCancelled = status == ReservationStatus.CANCELLED && booking.getStatus() != ReservationStatus.CANCELLED;
        if (newlyCancelled) {
            refundBonus(booking);
        }
        booking.setStatus(status);
        booking = bookings.save(booking);
        if (newlyCancelled) notifications.bookingCancelled(booking);
        return BookingResponse.from(booking);
    }

    private CustomerUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private int capacityFor(Experience experience) {
        Integer capacity = experience.getCapacity();
        if (capacity == null || capacity < 1) {
            String type = experience.getType();
            return ("routes".equalsIgnoreCase(type) || "route".equalsIgnoreCase(type)) ? 8 : 5;
        }
        return capacity;
    }

    private int currentBonuses(CustomerUser user) {
        return user.getBonuses() == null ? 0 : Math.max(0, user.getBonuses());
    }

    private int participantCount(CreateBookingRequest request) {
        Integer guestCount = request.guestCount();
        if (guestCount == null || guestCount < 1) {
            return 1;
        }
        return 2;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isFriday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    private boolean canCustomerCancel(Booking booking) {
        return bookingStart(booking).isAfter(LocalDateTime.now().plusHours(2));
    }

    private boolean canCustomerBook(LocalDate date, String hour) {
        return bookingStart(date, hour).isAfter(LocalDateTime.now().plusHours(2));
    }

    private LocalDateTime bookingStart(Booking booking) {
        return bookingStart(booking.getDateKey(), booking.getHour());
    }

    private LocalDateTime bookingStart(LocalDate date, String hour) {
        return LocalDateTime.of(date, LocalTime.parse(hour));
    }

    private List<String> experienceHours(Experience experience, LocalDate date) {
        String hours = isFriday(date) ? experience.getFridayHours() : experience.getHours();
        try {
            return objectMapper.readValue(hours, new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void refundBonus(Booking booking) {
        CustomerUser user = booking.getUser();
        if (user == null) {
            return;
        }

        user.setBonuses(currentBonuses(user) + bookingBonusAmount(booking));
        users.save(user);
    }

    private int bonusCostFor(Experience experience) {
        BigDecimal price = experience.getPrice();
        if (price == null || price.compareTo(BigDecimal.ONE) < 0 || price.compareTo(BigDecimal.TEN) > 0) {
            return 1;
        }
        return price.setScale(0, RoundingMode.UP).intValue();
    }

    private int bookingBonusAmount(Booking booking) {
        BigDecimal amount = booking.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(BigDecimal.TEN) > 0) {
            return 1;
        }
        return amount.setScale(0, RoundingMode.UP).intValue();
    }
}
