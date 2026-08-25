package com.formulariocaballos.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminUpdateUserRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Pattern(regexp = "^(\\+34|0034)?[6789]\\d{8}$", message = "El teléfono debe ser un número español válido") String phone,
    @NotBlank @Email String email,
    @Pattern(
        regexp = "^$|^(?=.*[a-záéíóúüñ])(?=.*[A-ZÁÉÍÓÚÜÑ])(?=.*\\d).{8,72}$",
        message = "La contraseña debe tener entre 8 y 72 caracteres, mayúscula, minúscula y número"
    ) String password,
    @NotBlank String role,
    Integer sessions,
    Boolean active
) {}
