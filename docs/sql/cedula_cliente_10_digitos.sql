-- Migración de longitud; la aplicación/JPA valida además región y módulo 10.
-- Revisar primero: SELECT COUNT(*) FROM cliente WHERE CHAR_LENGTH(cedula) > 10;
-- Si existen valores largos, corregirlos con información real antes de continuar.
-- El modo estricto evita truncar valores históricos de forma silenciosa.
SET @sql_mode_previo_cedula = @@SESSION.sql_mode;
SET SESSION sql_mode = CONCAT_WS(',', @@SESSION.sql_mode, 'STRICT_ALL_TABLES');
ALTER TABLE cliente MODIFY COLUMN cedula VARCHAR(10) NOT NULL;
SET SESSION sql_mode = @sql_mode_previo_cedula;
