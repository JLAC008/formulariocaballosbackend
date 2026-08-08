package com.formulariocaballos.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.booking.Booking;
import com.formulariocaballos.booking.BookingRepository;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.experience.Experience;
import com.formulariocaballos.experience.ExperienceRepository;
import com.formulariocaballos.state.dto.AppStateDto;
import com.formulariocaballos.state.dto.BookingDto;
import com.formulariocaballos.state.dto.CustomerUserDto;
import com.formulariocaballos.state.dto.ExperienceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AppStateService {

    private final CustomerUserRepository customerUserRepository;
    private final ExperienceRepository experienceRepository;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    public AppStateService(CustomerUserRepository customerUserRepository,
                           ExperienceRepository experienceRepository,
                           BookingRepository bookingRepository,
                           ObjectMapper objectMapper) {
        this.customerUserRepository = customerUserRepository;
        this.experienceRepository = experienceRepository;
        this.bookingRepository = bookingRepository;
        this.objectMapper = objectMapper;
    }

    public AppStateDto getState() {
        return new AppStateDto(
            customerUserRepository.findAll().stream()
                .sorted(Comparator.comparing(CustomerUser::getCreatedAt).reversed())
                .map(this::toCustomerDto)
                .toList(),
            experienceRepository.findAll().stream()
                .sorted(Comparator.comparing(Experience::getId).reversed())
                .map(this::toExperienceDto)
                .toList(),
            bookingRepository.findAll().stream()
                .sorted(Comparator.comparing(Booking::getId).reversed())
                .map(this::toBookingDto)
                .toList()
        );
    }

    @Transactional
    public AppStateDto replaceState(AppStateDto state) {
        List<ExperienceDto> experiences = state.experiences() == null ? List.of() : state.experiences();
        List<CustomerUserDto> users = state.users() == null ? List.of() : state.users();
        List<BookingDto> bookings = state.bookingHistory() == null ? List.of() : state.bookingHistory();

        bookingRepository.deleteAll();
        customerUserRepository.deleteAll();
        experienceRepository.deleteAll();
        experienceRepository.flush();
        customerUserRepository.flush();

        Map<Long, Experience> experiencesByClientId = experiences.stream()
            .map(this::toExperienceEntity)
            .collect(java.util.stream.Collectors.toMap(Experience::getId, experienceRepository::save));

        Map<Long, CustomerUser> usersByClientId = users.stream()
            .map(this::toCustomerEntity)
            .collect(java.util.stream.Collectors.toMap(CustomerUser::getId, customerUserRepository::save));

        bookings.stream()
            .map(booking -> toBookingEntity(booking, usersByClientId, experiencesByClientId))
            .forEach(bookingRepository::save);

        return getState();
    }

    private ExperienceDto toExperienceDto(Experience experience) {
        return new ExperienceDto(
            experience.getId(),
            experience.getType(),
            experience.getTitle(),
            experience.getDescription(),
            experience.getLevel(),
            experience.getDuration(),
            experience.getPrice(),
            experience.getImage(),
            experience.getActive(),
            readJson(experience.getHours(), new TypeReference<>() {}, List.of()),
            readJson(experience.getHourMessages(), new TypeReference<>() {}, Map.of())
        );
    }

    private CustomerUserDto toCustomerDto(CustomerUser user) {
        return new CustomerUserDto(
            user.getId(),
            user.getName(),
            user.getPhone(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            user.getBonuses(),
            user.getCreatedAt().toString()
        );
    }

    private BookingDto toBookingDto(Booking booking) {
        return new BookingDto(
            booking.getId(),
            booking.getUser() == null ? null : booking.getUser().getId(),
            booking.getExperience() == null ? null : booking.getExperience().getId(),
            booking.getType(),
            booking.getTitle(),
            booking.getDate(),
            booking.getDateKey().toString(),
            booking.getHour(),
            booking.getPayment(),
            booking.getCustomerName(),
            booking.getPhone(),
            booking.getAmount(),
            booking.getStatus()
        );
    }

    private Experience toExperienceEntity(ExperienceDto dto) {
        Experience experience = new Experience();
        experience.setId(dto.id());
        experience.setType(valueOrDefault(dto.type(), "lessons"));
        experience.setTitle(valueOrDefault(dto.title(), "Clase"));
        experience.setDescription(valueOrDefault(dto.description(), ""));
        experience.setLevel(valueOrDefault(dto.level(), ""));
        experience.setDuration(valueOrDefault(dto.duration(), ""));
        experience.setPrice(dto.price() == null ? java.math.BigDecimal.ZERO : dto.price());
        experience.setImage(valueOrDefault(dto.image(), ""));
        experience.setActive(dto.active() == null || dto.active());
        experience.setHours(writeJson(dto.hours() == null ? List.of() : dto.hours()));
        experience.setHourMessages(writeJson(dto.hourMessages() == null ? Map.of() : dto.hourMessages()));
        return experience;
    }

    private CustomerUser toCustomerEntity(CustomerUserDto dto) {
        CustomerUser user = new CustomerUser();
        user.setId(dto.id());
        user.setName(valueOrDefault(dto.name(), ""));
        user.setPhone(valueOrDefault(dto.phone(), ""));
        user.setEmail(valueOrDefault(dto.email(), ""));
        user.setPassword(valueOrDefault(dto.password(), ""));
        user.setRole(valueOrDefault(dto.role(), "CUSTOMER"));
        user.setBonuses(dto.bonuses() == null ? 0 : Math.max(0, dto.bonuses()));
        user.setCreatedAt(parseDateTime(dto.createdAt()));
        return user;
    }

    private Booking toBookingEntity(BookingDto dto, Map<Long, CustomerUser> users, Map<Long, Experience> experiences) {
        Booking booking = new Booking();
        booking.setId(dto.id());
        booking.setUser(dto.userId() == null ? null : users.get(dto.userId()));
        booking.setExperience(dto.experienceId() == null ? null : experiences.get(dto.experienceId()));
        booking.setType(valueOrDefault(dto.type(), "lessons"));
        booking.setTitle(valueOrDefault(dto.title(), ""));
        booking.setDate(valueOrDefault(dto.date(), ""));
        booking.setDateKey(LocalDate.parse(dto.dateKey()));
        booking.setHour(valueOrDefault(dto.hour(), "00:00"));
        booking.setPayment(valueOrDefault(dto.payment(), ""));
        booking.setCustomerName(valueOrDefault(dto.customerName(), ""));
        booking.setPhone(valueOrDefault(dto.phone(), ""));
        booking.setAmount(dto.amount() == null ? java.math.BigDecimal.ZERO : dto.amount());
        booking.setStatus(dto.status() == null ? com.formulariocaballos.booking.ReservationStatus.CONFIRMED : dto.status());
        return booking;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }

    private <T> T readJson(String value, TypeReference<T> type, T fallback) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON value");
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
