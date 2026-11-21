# Memoria del backend

Actualizado: 20 de noviembre de 2026, America/Guayaquil.

## Estado vigente

Completado el ajuste solicitado de CRUD ADMIN y cuentas CLIENTE con perfil personal.
También completada la validación de cédula ecuatoriana de 10 dígitos por módulo 10.
Leer este archivo, README.md, [docs/VALIDACION_CEDULA.md](docs/VALIDACION_CEDULA.md) y
[docs/CRUD_ADMIN_CLIENTE.md](docs/CRUD_ADMIN_CLIENTE.md) al retomar.
Los informes FASE_1–FASE_7 y AJUSTES_CLASE.md son históricos cuando contradicen este contrato.

- ADMIN tiene CRUD en /api/admin para Cancha, HorarioAtencion, Utileria, Cliente, Usuario,
  Reserva y ReservaUtileria. Los detalles son recursos anidados de su reserva.
- Usuario contiene credenciales y permisos; Cliente contiene datos personales. Cada nueva
  cuenta pública o creada por ADMIN es CLIENTE activa y crea su Cliente en la misma transacción.
- Registro exige username/password más cedula, nombres, apellidos, email y telefono.
  La respuesta incluye clienteId; nunca contraseña/hash. Login conserva su contrato previo.
- Cédula es String de 10 dígitos ASCII exactos; se validan región, tercer dígito y módulo 10,
  incluidas las series consulares 30 con tercer dígito 4/5. No recortar espacios ni convertir
  a número. La regla se comparte entre dominio, DTOs y entidad JPA. Error HTTP 400 en
  detalles.cedula; los duplicados válidos conservan 409. No verifica identidad en Registro Civil.
- Relación usuarios.cliente_id única, nullable por compatibilidad con ADMIN y cuentas antiguas.
  ADMIN completa perfiles históricos explícitamente; no inventar personas ni asignar por coincidencias.
- Reservas conservan responsableId como ID de cuenta. CLIENTE consulta/reserva/cancela lo propio;
  ADMIN usa las rutas administrativas para crear, editar, cancelar y eliminar reservas ajenas.
- DELETE es físico y protege referencias con 409. Solo Reserva elimina sus detalles por cascada.
  No elimina cuentas, perfiles, canchas o implementos asociados. ADMIN del sistema está protegido.
- Cambios de reservas verifican ocupación/stock/límite excluyendo la propia reserva y mantienen
  bloqueos ordenados. La auditoría se conserva; cambiar detalles actualiza modificado de la reserva.
- APP_INITIALIZE_CATALOGS=false por defecto para no restaurar datos eliminados al reiniciar.

## Arquitectura que se conserva

Java 25, Spring Boot 4.1.2 y MySQL. Se mantienen los nombres infraestructure/securities.
Login directo en AuthController con AuthenticationManager y JWTServices; PasswordOut para BCrypt.
Sin puertos específicos de autenticación, aspectos propios ni application/models.
Los siete modelos/entidades conservan creado/modificado como LocalDateTime; el código de
persistencia administra esas fechas. fechaCreacion de Reserva ahora usa precisión de microsegundos.

## Base local y verificación

Se aplicó docs/sql/vincular_usuario_cliente.sql de forma aditiva e idempotente a la base local.
Las huellas de los datos originales de las siete entidades coincidieron antes/después.
No se borraron cuentas, perfiles ni reservas existentes para realizar la migración.
Evidencia: target/crud-migracion-verificacion.txt. Los perfiles antiguos quedan pendientes de
completar con datos reales, por ADMIN; las cuentas mantienen acceso y propiedad de reservas.

Resultado vigente después del ajuste de cédula: **303 pruebas aprobadas, cero fallos, errores u omisiones**.
Incluye 81 pruebas MySQL, de las cuales 16 son de concurrencia; las otras 222 cubren dominio,
servicios, HTTP, permisos, validación, mappers y configuración. Las pruebas MySQL ordinarias
usan rollback; las concurrentes limpian sus propios fixtures por ID. AUTO_INCREMENT puede avanzar.
Se probó un fallo después de insertar Cliente: la transacción revirtió el perfil y no creó la cuenta.

Logs vigentes: target/cedula-final.log y target/surefire-reports. El comando package ejecutó
las 303 pruebas y generó el JAR con BUILD SUCCESS. El ajuste CRUD anterior aprobó 274 pruebas;
sus logs históricos se conservan en target/crud-final.log y target/crud-package.log.

Se revisó Cliente antes de estrechar cedula: la tabla estaba vacía y no hubo valores que corregir.
Se aplicó docs/sql/cedula_cliente_10_digitos.sql y la columna quedó VARCHAR(10), sin truncar datos.
Evidencia: target/cedula-migracion.txt. El número dado por el usuario pasó el algoritmo en pruebas;
no se creó ninguna cuenta real con él. Los fixtures de integración generan números sintéticos.

Ejecutable generado: target/sistema_canchas_deportivas-0.0.1-SNAPSHOT.jar.
No se reinició ningún servidor que pudiera estar usando el usuario; ejecutar/reiniciar el backend
con la versión nueva para exponer las rutas actualizadas.

## Siguiente integración

El frontend hermano no se modificó en esta solicitud. Su formulario de registro debe incorporar
los cinco campos personales; enviar solo username/password ahora produce 400. Añadir también
las acciones nuevas de ADMIN, tratamiento de DELETE 204 y vínculo usuario/perfil.
Aplicar también las reglas de docs/VALIDACION_CEDULA.md al registro y formularios de Cliente,
con campo de texto, teclado numérico, 10 caracteres y errores junto al campo cedula.
El frontend previo tenía imágenes por disciplina y tablas, con fases 0–8 completas y mejora UI
del 19 de noviembre. No regenerar el proyecto ni perder esa implementación al integrar este cambio.

No guardar credenciales/JWT/datos personales en esta memoria. No repetir el borrado histórico
de auditoría ni generar datos falsos para completar cuentas antiguas.
