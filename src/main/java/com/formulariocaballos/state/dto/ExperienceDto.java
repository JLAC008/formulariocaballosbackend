package com.formulariocaballos.state.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ExperienceDto(
    Long id,
    String type,
    String title,
    String description,
    String level,
    String duration,
    BigDecimal price,
    Integer capacity,
    String image,
    Boolean active,
    Boolean fridayAvailable,
    List<String> fridayHours,
    Map<String, String> fridayHourMessages,
    List<String> hours,
    Map<String, String> hourMessages
) {}
