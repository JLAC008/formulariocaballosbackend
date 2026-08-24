package com.formulariocaballos.auth.dto;

import com.formulariocaballos.state.dto.CustomerUserDto;

public record AuthResponse(
    String token,
    String username,
    String role,
    CustomerUserDto user,
    boolean verificationResent
) {}
