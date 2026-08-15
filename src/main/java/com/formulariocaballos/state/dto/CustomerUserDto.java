package com.formulariocaballos.state.dto;

public record CustomerUserDto(
    Long id,
    String firstName,
    String lastName,
    String phone,
    String email,
    String role,
    Integer bonuses,
    boolean emailVerified,
    boolean active,
    String createdAt,
    String updatedAt
) {}
