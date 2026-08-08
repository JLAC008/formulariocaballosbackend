package com.formulariocaballos.state.dto;

import java.util.List;

public record AppStateDto(
    List<CustomerUserDto> users,
    List<ExperienceDto> experiences,
    List<BookingDto> bookingHistory
) {}
