-- Verificación de solo lectura para la fase 2.
USE sistema_canchas_deportivas;

SELECT DATABASE() AS base_actual, VERSION() AS version_mysql;

SELECT 'usuarios' AS tabla, COUNT(*) AS registros FROM usuarios
UNION ALL SELECT 'canchas', COUNT(*) FROM canchas
UNION ALL SELECT 'horarios_atencion', COUNT(*) FROM horarios_atencion
UNION ALL SELECT 'utileria', COUNT(*) FROM utileria
UNION ALL SELECT 'utileria_disciplinas', COUNT(*) FROM utileria_disciplinas
UNION ALL SELECT 'reservas', COUNT(*) FROM reservas
UNION ALL SELECT 'reserva_utileria', COUNT(*) FROM reserva_utileria;

SELECT id, nombre, tipo, superficie, activa + 0 AS activa FROM canchas ORDER BY id;
SELECT tipo, COUNT(*) AS cantidad FROM canchas GROUP BY tipo;
SELECT dia, apertura, cierre FROM horarios_atencion
ORDER BY FIELD(dia, 'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY');
SELECT u.id, u.nombre, u.tipo, u.existencias, u.activa + 0 AS activa,
       GROUP_CONCAT(d.disciplina ORDER BY d.disciplina) AS disciplinas
FROM utileria u
LEFT JOIN utileria_disciplinas d ON d.utileria_id = u.id
GROUP BY u.id, u.nombre, u.tipo, u.existencias, u.activa
ORDER BY u.id;

-- Verifica el formato sin mostrar la contraseña ni su hash.
SELECT id, username, rol, activo + 0 AS activo,
       CHAR_LENGTH(password_hash) = 60
       AND LEFT(password_hash, 4) IN ('$2a$', '$2b$', '$2y$') AS formato_bcrypt
FROM usuarios WHERE username = 'admin';

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

