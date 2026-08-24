package com.formulariocaballos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(min = 2, max = 80) @Pattern(
        regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+(?:[ '\\-][A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+)*$",
        message = "Introduce un nombre válido"
    ) String firstName,
    @NotBlank @Size(min = 2, max = 80) @Pattern(
        regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+(?:[ '\\-][A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+)*$",
        message = "Introduce unos apellidos válidos"
    ) String lastName,
    @NotBlank @Pattern(
        regexp = "^(\\+34|0034)?[6789]\\d{8}$",
        message = "El teléfono debe ser un número español válido"
    ) String phone
) {}
