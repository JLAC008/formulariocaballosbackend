# Formulario Caballos Backend

Backend Spring Boot para `FormularioCaballosFront`, siguiendo la estructura usada en `PistasDeportivasBackend`: configuración por `application.yml`, PostgreSQL, Flyway, capas por dominio y DTOs.

## Arranque local

1. Levantar PostgreSQL y la API:

```bash
docker compose up -d
```

La API queda en `http://localhost:8081/api`.

## Endpoints

- `POST /api/auth/login`: valida admin o cliente y devuelve token, usuario y rol.
- `POST /api/auth/register`: crea cliente y devuelve token, usuario y rol.
- `GET /api/state`: devuelve usuarios, clases y reservas.
- `PUT /api/state`: sincroniza el estado completo del frontend en base de datos. Requiere `Authorization: Bearer <token>`.

El frontend está configurado para consumir `http://localhost:8081/api`.

El admin por defecto se configura con `ADMIN_USERNAME` y `ADMIN_PASSWORD`; si no se indican variables de entorno, usa `admin / admin`.
