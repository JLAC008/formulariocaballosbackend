# Tasks

## 1. Implementación

- [x] 1.1 Registrar tree efectivo y avisos OSV.
- [x] 1.2 Actualizar el BOM a Spring Boot 4.0.9.
- [ ] 1.5 Actualizar la imagen runtime cuando el registro oficial publique Temurin 21.0.13+9 para Alpine.
- [x] 1.3 Comprobar resolución, tests seguros y auditoría posterior.
- [x] 1.4 Revisar diff independientemente.

La suite completa ejecuta 32 pruebas: 21 pasan y 9 fallan en `BookingServiceTest` por fechas fijas antiguas. Las mismas 9 fallan con el parent base 4.0.6, así que no son una regresión de Boot 4.0.9.

## 2. Publicación

- [x] 2.1 Con autorización del usuario, verificar `origin/master`, publicar la actualización del BOM y comprobar salud en Dokploy.
- [ ] 2.2 Confirmar el runtime parcheado cuando se actualice la imagen oficial de Temurin.
