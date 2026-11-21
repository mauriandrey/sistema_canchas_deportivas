-- Migración aditiva para MySQL 8. Ejecutar en la base del proyecto antes de usar ddl-auto=validate.
-- No crea personas ficticias ni asigna perfiles por coincidencias de nombres/email.
-- Los ADMIN y las cuentas anteriores pueden conservar cliente_id NULL.
-- Cada nueva cuenta CLIENTE creada por la API exige su perfil completo.

SET @ddl_perfil = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'cliente_id') = 0,
    'ALTER TABLE usuarios ADD COLUMN cliente_id BIGINT NULL', 'SELECT 1');
PREPARE migracion_perfil FROM @ddl_perfil;
EXECUTE migracion_perfil;
DEALLOCATE PREPARE migracion_perfil;

SET @ddl_perfil = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'cliente_id' AND non_unique = 0) = 0,
    'ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_cliente UNIQUE (cliente_id)', 'SELECT 1');
PREPARE migracion_perfil FROM @ddl_perfil;
EXECUTE migracion_perfil;
DEALLOCATE PREPARE migracion_perfil;

SET @ddl_perfil = IF(
    (SELECT COUNT(*) FROM information_schema.key_column_usage
     WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'cliente_id'
       AND referenced_table_name = 'cliente' AND referenced_column_name = 'id') = 0,
    'ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)', 'SELECT 1');
PREPARE migracion_perfil FROM @ddl_perfil;
EXECUTE migracion_perfil;
DEALLOCATE PREPARE migracion_perfil;

-- El administrador completa explícitamente los perfiles de las cuentas anteriores mediante
-- POST /api/admin/clientes y PUT /api/admin/usuarios/{id} con el clienteId correcto.
-- No ejecutar UPDATE masivos ni eliminar cuentas existentes para aplicar esta migración.
