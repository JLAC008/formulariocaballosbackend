# Design

## Context

Árbol Maven efectivo y consulta OSV sobre GAV/versiones exactas. Evidencias en work/java-audit.md del chat. Runtime real desplegado desconocido.

## Goals / Non-Goals

Eliminar avisos con BOM compatible. No cambiar APIs/autenticación ni migraciones.

## Decisions

Usar Boot 4.0.9 BOM en lugar de overrides individuales y verificar tree y tests sin BD real. Temurin 21.0.13+9 está publicado por Adoptium, pero Docker Hub aún no ofrece la etiqueta Alpine específica; la etiqueta flotante `21-jre-alpine` comprobada en build local resolvió a Temurin 21.0.12.1+1. No afirmar corregido el runtime hasta que exista una imagen oficial que incluya 21.0.13+9 y se despliegue.

## Risks / Trade-offs

Las etiquetas flotantes no garantizan la versión desplegada. La publicación de esta corrección actualiza las dependencias Java y conserva el runtime actual; el parche de la JVM queda abierto. Los tests deben ejecutarse con H2 o mocks ya existentes, sin producción.

## Migration Plan

No migraciones nuevas. Conservar PostgreSQL externo y migraciones existentes. La publicación autorizada en `master` dispara producción; verificar estado de Dokploy y salud pública. Mantener pendiente el cambio de imagen de runtime hasta que la etiqueta oficial de Temurin 21.0.13 esté disponible.
