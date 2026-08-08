CREATE TABLE experiences (
    id BIGINT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    level VARCHAR(80),
    duration VARCHAR(60),
    price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    image VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    hours TEXT NOT NULL,
    hour_messages TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE customer_users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(60) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(180) NOT NULL,
    bonuses INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY,
    user_id BIGINT REFERENCES customer_users(id) ON DELETE SET NULL,
    experience_id BIGINT REFERENCES experiences(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    date_label VARCHAR(180) NOT NULL,
    date_key DATE NOT NULL,
    hour VARCHAR(5) NOT NULL,
    payment VARCHAR(80) NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    phone VARCHAR(60) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO experiences (id, type, title, description, level, duration, price, image, active, hours, hour_messages)
VALUES
  (1, 'lessons', 'Clase de Iniciacion', 'Sesion guiada en pista para aprender postura, control basico y seguridad desde cero.', 'Principiante', '60 min', 38, 'assets/route-sendero.jpg', TRUE, '["11:00","18:00","18:45","19:30"]', '{}'),
  (2, 'lessons', 'Clase Tecnica Privada', 'Trabajo personalizado para mejorar ayudas, asiento y confianza con seguimiento individual.', 'Intermedio', '75 min', 55, 'assets/route-crepusculo.jpg', TRUE, '["11:00","18:00","18:45","19:30"]', '{}');
