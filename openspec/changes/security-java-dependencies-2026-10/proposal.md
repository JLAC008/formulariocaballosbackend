# Proposal

## Why

La auditoría del 2026-10-23 detecta versiones afectadas de Spring Framework, Tomcat, Jackson y pgjdbc gestionadas por Spring Boot 4.0.6.

## What Changes

Actualizar el parent a Spring Boot 4.0.9. Temurin 21.0.13 se publicó upstream, pero el 23-10 el registro oficial de imágenes todavía no ofrece esa etiqueta Alpine; documentar y completar el runtime cuando esté publicada. Mantener contratos y referencias DB.

## Capabilities

Sin cambios funcionales intencionados; skip_specs por mantenimiento de dependencias.

## Impact

pom.xml. Sin cambios de esquema, variables ni Compose. Publicar solo la corrección de dependencias Java tras las comprobaciones; el parche del runtime Java queda pendiente de la imagen oficial.
