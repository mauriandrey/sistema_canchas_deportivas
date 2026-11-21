-- Migración aditiva de referencia para entornos con ddl-auto=validate.
-- Ejecutar sobre sistema_canchas_deportivas tras revisar el esquema del entorno.
-- En local, Hibernate update crea esta misma tabla al arrancar.
CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cedula VARCHAR(10) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NULL,
    creado DATETIME(6) NOT NULL,
    modificado DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cliente_cedula UNIQUE (cedula),
    CONSTRAINT uk_cliente_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
