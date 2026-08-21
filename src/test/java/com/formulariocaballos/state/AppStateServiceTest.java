package com.formulariocaballos.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.booking.BookingRepository;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.customer.Role;
import com.formulariocaballos.experience.ExperienceRepository;
import com.formulariocaballos.notification.NotificationService;
import com.formulariocaballos.state.dto.AppStateDto;
import com.formulariocaballos.state.dto.CustomerUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppStateServiceTest {
    @Test
    void replaceStateUpdatesUsersWithoutDeletingThem() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        ExperienceRepository experiences = mock(ExperienceRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        CustomerUser existing = new CustomerUser();
        existing.setId(10L);
        existing.setEmail("user@example.com");
        existing.setPasswordHash("hash");
        existing.setRole(Role.USER);
        existing.setBonuses(1);
        existing.setEmailVerified(true);
        existing.setActive(true);
        existing.setCreatedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());

        when(users.findAll()).thenReturn(List.of(existing));
        when(bookings.findAll()).thenReturn(List.of());
        when(experiences.findAll()).thenReturn(List.of());
        when(users.save(any(CustomerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppStateService service = new AppStateService(
            users,
            experiences,
            bookings,
            new ObjectMapper(),
            new BCryptPasswordEncoder(),
            mock(NotificationService.class)
        );

        service.replaceState(new AppStateDto(
            List.of(new CustomerUserDto(10L, "Ada", "Lovelace", "+34600000000", "user@example.com", "USER", 4, true, true,
                LocalDateTime.now().toString(), LocalDateTime.now().toString())),
            List.of(),
            List.of()
        ));

        verify(users, never()).deleteAll();
        verify(users).save(existing);
    }
}
