package com.formulariocaballos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Pattern(
        regexp = "^(?=.*[a-záéíóúüñ])(?=.*[A-ZÁÉÍÓÚÜÑ])(?=.*\\d).{8,72}$",
        message = "La contraseña debe tener entre 8 y 72 caracteres, mayúscula, minúscula y número"
    ) String newPassword
) {}
