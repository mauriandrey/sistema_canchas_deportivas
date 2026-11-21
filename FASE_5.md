# Fase 5 — Gestión de reservas

## 1. Alcance y resultado

Fase 5 completada sobre la arquitectura hexagonal existente.
Se mantienen Java 25, Spring Boot 4.1.2, MySQL, BCrypt, JWT y la auditoría creado/modificado.
Se implementan creación, consultas privadas, consulta administrativa y cancelación de reservas.
La utilería opcional se integrará en la Fase 6; en esta fase las reservas nuevas se crean sin implementos.

## 2. Endpoints

| Método y ruta | Acceso | Resultado |
|---|---|---|
| POST /api/reservas | CLIENTE activo | 201, reserva CONFIRMADA y cabecera Location |
| GET /api/reservas/mis-reservas | Usuario autenticado activo | 200, únicamente las propias |
| GET /api/reservas/{id} | Propietario o ADMIN | 200, detalle |
| PATCH /api/reservas/{id}/cancelar | Propietario | 200, reserva CANCELADA |
| GET /api/admin/reservas | ADMIN activo | 200, todas las reservas |

Las listas incluyen confirmadas y canceladas, ordenadas de más recientes a más antiguas.
El ADMIN consulta reservas de otros; la cancelación sigue reservada al propietario.
La política existente permite crear reservas solamente al rol CLIENTE.

## 3. Archivos creados o modificados

Código bajo src/main/java/org/example/sistema_canchas_deportivas/:

Creados:

- domain/exception/AccesoDenegadoException.java
- application/models/ReferenciaReserva.java
- infraestructure/exceptions/ReservaExceptionHandler.java

Completados o modificados:

- domain/models/Reserva.java
- application/ports/in/ReservaIn.java
- application/ports/out/ReservaOut.java
- application/ports/out/UsuarioOut.java
- application/services/ReservaService.java
- infraestructure/controllers/ReservaController.java
- infraestructure/controllers/dtos/ReservaRequest.java
- infraestructure/controllers/dtos/ReservaResponse.java
- infraestructure/repositories/ReservaRepository.java
- infraestructure/repositories/UsuarioRepository.java
- infraestructure/adapters/ReservaAdapter.java
- infraestructure/adapters/UsuarioAdapter.java
- infraestructure/mappers/ReservaMapper.java
- infraestructure/securities/AutorizacionReserva.java (comentario de alcance)

Pruebas nuevas bajo src/test/java/org/example/sistema_canchas_deportivas/:

- application/services/ReservaServiceTest.java
- infraestructure/persistence/ReservasMysqlTest.java
- infraestructure/persistence/ReservaConcurrenciaMysqlTest.java

Se actualizó AuthMysqlTest: el JWT del administrador ahora obtiene 200 en /api/admin/reservas.
Documentación: FASE_5.md, docs/fase5.http y el ejemplo administrativo de docs/fase3.http.

No se modificaron el POM, el esquema, la inicialización de datos ni el mecanismo de auditoría.

## 4. Arquitectura y patrón de clase

```text
JWTFilter → UsuarioPrincipal
  → ReservaController → ReservaIn → ReservaService
  → Reserva.nueva / Reserva.cancelar
  → puertos OUT → adaptadores → repositorios JPA → MySQL
  → ReservaMapper → ReservaResponse
```

El controlador obtiene actorId exclusivamente de UsuarioPrincipal, construido a partir del JWT
y del usuario vigente. El DTO de creación no acepta responsableId, estado, ID de reserva ni fechas
de auditoría. Los campos desconocidos provocan 400.

Reserva.nueva(...) valida las reglas propias de la reserva y construye una CONFIRMADA.
Reserva.cancelar(...) devuelve una nueva instancia manteniendo ID, datos y auditoría.
Se conserva el patrón inmutable del proyecto y los constructores completos para reconstruir el dominio.
La auditoría JPA conserva creado y asigna modificado al persistir una cancelación.

Los servicios solo dependen de puertos OUT y modelos; no utilizan entidades JPA ni Spring Security.
Además de las comprobaciones HTTP, los casos de uso comprueban usuario activo, rol y propiedad.
Las respuestas no exponen contraseñas ni hashes.

## 5. Reglas de negocio

### Creación

1. El responsable existe, está activo y tiene rol CLIENTE.
2. La cancha existe y está activa.
3. La disciplina es compatible con el tipo de cancha.
4. La fecha y el inicio son actuales o futuros en America/Guayaquil.
5. El inicio tiene minutos, segundos y fracciones en cero.
6. La duración es exactamente 1 o 2 horas.
7. La reserva cabe completamente dentro del horario del día.
8. El responsable no supera **2 horas confirmadas acumuladas por día**, contando todas sus canchas.
9. No existe otra reserva confirmada superpuesta del responsable, aunque sea en otra cancha.
10. No existe otra reserva confirmada superpuesta sobre esa cancha física.

El límite diario es la interpretación inicial del negocio acordada en los requisitos.
Para cambiarlo posteriormente debe ajustarse la regla del caso de uso y sus pruebas.

La hora se vuelve a evaluar después de obtener los bloqueos: una solicitud que estuvo esperando
no puede confirmar un horario que ya pasó mientras esperaba.

La superposición utiliza [inicio, fin): inicioA < finB y finA > inicioB.
10:00–11:00 y 11:00–12:00 son consecutivas válidas.
Una reserva de VOLEY también bloquea ECUAVOLEY en la misma cancha.

### Cancelación

- Solo el propietario puede cancelar.
- Debe realizarse antes del inicio; a la hora exacta de inicio ya se rechaza.
- El registro permanece con estado CANCELADA.
- Se conservan fechaCreacion, creado y los datos de la reserva.
- Las consultas de ocupación y acumulación excluyen canceladas, liberando cancha y horas diarias.
- La operación es idempotente: repetirla sobre una reserva ya cancelada devuelve el mismo estado
  sin guardar otra modificación, incluso si luego transcurrió su hora de inicio.
- Se conserva cualquier detalle histórico que ya exista; la asignación de utilería se desarrollará en Fase 6.

### Consultas y errores

Para clientes, un ID inexistente o ajeno se deniega con 403 sin revelar su existencia.
El ADMIN obtiene 404 si el ID consultado no existe.
La seguridad por ruta puede rechazar un ID inválido antes de llegar al controlador.

| Código HTTP | Situación |
|---|---|
| 400 | JSON, campos, duración, hora, fecha o disciplina inválidos |
| 401 | Falta JWT o token inválido |
| 403 | Acceso ajeno, rol incorrecto o usuario sin autorización |
| 404 | Cancha/horario inexistente; reserva inexistente consultada por ADMIN |
| 409 | Límite diario, ocupación, cierre, cancha inactiva, cancelación tardía o conflicto transaccional |
| 500 | Error inesperado con mensaje genérico, sin SQL ni trazas |

El manejo de errores queda limitado a este módulo. La integración global corresponde a la Fase 7.

## 6. Transacciones y concurrencia

Crear y cancelar usan @Transactional con READ_COMMITTED y tiempo de espera transaccional de 15 segundos.
Los recursos se bloquean con PESSIMISTIC_WRITE en un orden estable:

```text
Horario del día → Usuario responsable → Cancha → Reserva (solo al cancelar)
```

Los bloqueos se conservan hasta commit o rollback.
Las consultas de reservas confirmadas y el INSERT ocurren dentro de la misma transacción,
después de obtener los bloqueos. READ_COMMITTED permite consultar los datos confirmados por
una transacción anterior después de haber esperado.

Cancelar primero obtiene ReferenciaReserva: una proyección de responsable, cancha y fecha.
Así puede adquirir los bloqueos en orden sin cargar una entidad con estado potencialmente antiguo.
La reserva y su estado se leen bajo bloqueo antes de cancelar.

Los bloqueos existentes de administración de canchas y horarios participan del mismo protocolo:

- Si una cancha se desactiva mientras una reserva espera, la reserva ve el estado confirmado y falla.
- Un cambio de horario espera a una creación en curso y revisa la reserva recién confirmada.
- El usuario bloqueado protege tanto superposición personal como acumulación diaria.

**Alcance de rendimiento:** el horario es una fila por día de la semana. Usar bloqueo exclusivo
serializa también solicitudes para distintas fechas que caen en ese mismo día.
Es una decisión simple y conservadora para el alcance académico; no es un bloqueo por franja
ni una solución optimizada para alto volumen. Las transacciones son breves y no realizan llamadas externas.

Una consulta pública de disponibilidad no garantiza que el bloque siga libre.
La confirmación vuelve a validar dentro de la transacción. Un conflicto o excepción revierte la operación.
Los reintentos automáticos y las claves de idempotencia de creación no forman parte de esta fase.

Referencias técnicas:
[aislamiento de InnoDB en MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html?ff=nopfpls)
y [bloqueos de InnoDB](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html).

## 7. Base de datos y verificación

Se reutilizan las relaciones, claves foráneas e índices de reservas de la Fase 2 y las fechas de auditoría.
No se recreó la base ni se ejecutó nuevamente la reinserción del ajuste de auditoría.

Comando de verificación final:

```powershell
$env:RUN_MYSQL_TESTS = 'true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Las opciones offline y de bloqueo aprovechan la caché en este entorno restringido.
En una terminal normal puede usarse .\mvnw.cmd test; sin RUN_MYSQL_TESTS se omiten las pruebas de MySQL.

**Resultado final: 141 pruebas aprobadas; cero fallos, errores u omisiones.**
Incluye 44 pruebas con MySQL real y toda la suite previa.

Las seis pruebas de concurrencia verifican:

1. Dos usuarios reservan el mismo bloque de una cancha compartida: solo uno confirma.
2. Un responsable intenta dos reservas de dos horas en distintas canchas y horas: solo una confirma.
3. Un responsable intenta reservas simultáneas de una hora en dos canchas: solo una confirma.
4. Dos reservas consecutivas de distintos responsables sí confirman.
5. La reserva espera una edición de cancha y observa su desactivación confirmada.
6. El administrador espera una reserva en curso y rechaza un horario que la invalidaría.

Las pruebas normales de MySQL usan rollback.
Las pruebas concurrentes necesitan commits reales en conexiones independientes; crean usuarios y canchas
exclusivos y eliminan al terminar únicamente sus registros por ID.
La prueba de horario restaura exactamente sus valores y fechas de auditoría.
Los contadores AUTO_INCREMENT pueden avanzar durante las pruebas.

Informes: target/surefire-reports y target/fase5-pruebas-mysql.log.

La comprobación posterior confirmó 1 usuario, 17 canchas, 7 horarios, 8 artículos de utilería,
0 reservas y 0 detalles de reserva. No quedaron usuarios de concurrencia.
Los siete horarios conservaron sus horas y sus fechas de auditoría originales.

También se arrancó el servidor con MySQL local y se verificó por HTTP:
login del administrador existente, consulta administrativa (200), consulta de reservas propias (200),
reserva inexistente consultada por ADMIN (404), creación con ADMIN (403), consulta sin JWT (401)
y disponibilidad pública (200).
El servidor temporal se detuvo después de comprobarlo. Log: target/fase5-arranque.log.

## 8. Ejemplos de uso

Arranca con la configuración JWT de FASE_3.md:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

1. Registra un cliente y realiza login.
2. Consulta GET /api/canchas y disponibilidad con una fecha futura.
3. Copia un ID real y el JWT del CLIENTE en docs/fase5.http.
4. Crea una reserva de una o dos horas.
5. Consulta mis-reservas y el detalle.
6. Cancela antes del inicio y comprueba la disponibilidad.
7. Usa un JWT ADMIN para consultar todas las reservas.

Solicitud:

```json
{
  "canchaId": 12,
  "disciplina": "VOLEY",
  "fecha": "2027-01-04",
  "horaInicio": "10:00",
  "duracion": 2
}
```

El ID 12 es ilustrativo: elige uno del catálogo compatible con la disciplina.
La respuesta incluye ID, responsableId, canchaId, disciplina, fecha, horaInicio, horaFin,
duracion, estado y fechas de creación/auditoría. Las horas y fechas se expresan en la zona del complejo.

Los ejemplos manuales sí guardan reservas reales; cancelarlas conserva su historial.

## 9. Pendientes

- Fase 6: implementos opcionales, compatibilidad, disponibilidad temporal y protección concurrente de existencias.
- Fase 7: revisión integral, manejo global de errores y documentación final.

## 10. Próximo paso

La Fase 5 está terminada. Se espera autorización para la Fase 6.
