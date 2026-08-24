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
    String image,
    Boolean active,
    Boolean fridayAvailable,
    List<String> fridayHours,
    List<String> hours,
    Map<String, String> hourMessages
) {}
