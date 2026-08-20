# Formulario Caballos Backend

Backend Spring Boot para `FormularioCaballosFront`, siguiendo la estructura usada en `PistasDeportivasBackend`: configuración por `application.yml`, PostgreSQL, Flyway, capas por dominio y DTOs.

## Arranque local

1. Levantar PostgreSQL y la API:

```bash
docker compose up -d
```

La API queda en `http://localhost:8081/api`.

## Endpoints

- `POST /api/auth/login`: inicia sesión usando email y contraseña.
- `POST /api/auth/register`: crea un usuario y envía confirmación por email.
- `GET /api/auth/verify-email?token=...`: confirma el email.
- `POST /api/auth/resend-verification`: reenvía la confirmación.
- `POST /api/auth/forgot-password`: inicia la recuperación.
- `POST /api/auth/reset-password`: cambia la contraseña con un token.
- `POST /api/bookings`: crea una reserva y la aprueba mediante pago mock.
- `GET /api/bookings/me`: devuelve las reservas del usuario autenticado.
- `PATCH /api/bookings/{id}/cancel`: cancela una reserva propia.
- `GET /api/bookings/admin`: devuelve todas las reservas para administradores.
- `PATCH /api/bookings/{id}/status`: cambia el estado para administradores.
- `GET /api/state`: devuelve usuarios, clases y reservas.
- `PUT /api/state`: sincroniza el estado completo; requiere rol `ADMIN`.

El frontend está configurado para consumir `http://localhost:8081/api`.

El administrador se configura con `ADMIN_EMAIL` y `ADMIN_PASSWORD`. En producción deben definirse obligatoriamente secretos propios.

## Email

El backend envía correos con la **Transactional Email API de Brevo** (POST `/v3/smtp/email`, `BrevoEmailService`):

```
BREVO_API_KEY=tu_api_key_de_brevo
MAIL_FROM=martinezluna@pistasvalleperdido.es
```

- `BREVO_API_KEY`: API key de Brevo (Panel > SMTP & API > API Keys), empieza por `xkeysib-`. Es un secreto: en local se pone en `.env.local`. **Si existe `.env.local`, la aplicación usa solo `.env.local` e ignora `.env`.**
- **IP autorizada**: la IP desde la que se envía debe estar en Brevo (Panel > Seguridad > IPs autorizadas).
- `MAIL_FROM`: remitente (email o dominio) verificado en Brevo (Panel > Senders).
- `MAIL_FROM` y `FRONTEND_URL` se usan para los enlaces de los correos (verificar email / recuperar contraseña). La plantilla de confirmación está en `src/main/resources/templates/email/verification.html`.

## Verificación

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn clean test -Dmaven.compiler.release=17
```

El proyecto está configurado para Java 21; el comando anterior permite verificarlo en el entorno actual, que dispone de un JDK 17 completo. Flyway es la única fuente de creación y evolución del esquema.
