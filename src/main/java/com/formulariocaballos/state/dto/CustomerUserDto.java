package com.formulariocaballos.state.dto;

public record CustomerUserDto(
    Long id,
    String name,
    String phone,
    String email,
    String password,
    String role,
    Integer bonuses,
    String createdAt
) {}
