INSERT INTO experiences (id, type, title, description, level, duration, price, image, active, hours, hour_messages)
VALUES
  (1, 'lessons', 'Clase de Iniciacion', 'Sesion guiada en pista para aprender postura, control basico y seguridad desde cero.', 'Principiante', '60 min', 38, 'assets/route-sendero.jpg', TRUE, '["11:00","18:00","18:45","19:30"]', '{}'),
  (2, 'lessons', 'Clase Tecnica Privada', 'Trabajo personalizado para mejorar ayudas, asiento y confianza con seguimiento individual.', 'Intermedio', '75 min', 55, 'assets/route-crepusculo.jpg', TRUE, '["11:00","18:00","18:45","19:30"]', '{}')
ON CONFLICT (id) DO NOTHING;
