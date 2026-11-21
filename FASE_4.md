# Fase 4 — Gestión de canchas, horarios y disponibilidad

## 1. Fase y resumen

Fase 4 completada sobre el proyecto existente. Se conservan Java 25, Spring Boot 4.1.2,
JJWT, MySQL y la arquitectura hexagonal. No se modificó el POM ni la configuración de seguridad JWT.
La Fase 5 no se ha iniciado.

Se implementan consultas de canchas, creación y edición administrativa, activación/desactivación,
consulta y actualización de horarios semanales y disponibilidad calculada por fecha.

## 2. Endpoints

| Método y ruta | Acceso | Resultado |
|---|---|---|
| GET /api/canchas | Público | 200, catálogo con estado y disciplinas compatibles |
| GET /api/canchas/{id} | Público | 200 o 404 si no existe |
| GET /api/canchas/disponibilidad?fecha=2027-01-04&disciplina=VOLEY&duracion=2 | Público | 200, canchas y bloques disponibles |
| POST /api/admin/canchas | ADMIN | 201 y cabecera Location |
| PUT /api/admin/canchas/{id} | ADMIN | 200, reemplazo de los datos editables |
| PATCH /api/admin/canchas/{id}/estado | ADMIN | 200, cambio de activa |
| GET /api/horarios | CLIENTE o ADMIN | 200, siete días ordenados de lunes a domingo |
| PUT /api/admin/horarios/{dia} | ADMIN | 200, actualización del horario de ese día |

Los días se identifican con MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY y SUNDAY.
La política de GET /api/horarios conserva la seguridad de la Fase 3; la consulta pública de
disponibilidad ya incluye los bloques que necesita un visitante.

No se ofrece DELETE ni se eliminan canchas físicamente.
GET /api/admin/canchas no es una ruta de listado: el catálogo se consulta con GET /api/canchas.

## 3. Archivos creados o modificados

Rutas relativas al proyecto. El código principal está bajo
`src/main/java/org/example/sistema_canchas_deportivas/`.

Creados:

- application/models/OcupacionCancha.java
- application/models/DisponibilidadCancha.java
- infraestructure/controllers/dtos/EstadoCanchaRequest.java
- infraestructure/controllers/dtos/DisponibilidadResponse.java
- infraestructure/exceptions/CanchaExceptionHandler.java

Completados o modificados:

- domain/models/Cancha.java
- domain/models/HorarioAtencion.java
- application/ports/in/CanchaIn.java
- application/ports/in/HorarioAtencionIn.java
- application/ports/out/CanchaOut.java
- application/ports/out/HorarioAtencionOut.java
- application/ports/out/ReservaOut.java
- application/services/CanchaService.java
- application/services/HorarioAtencionService.java
- application/services/DatosInicialesService.java
- infraestructure/adapters/CanchaAdapter.java
- infraestructure/adapters/HorarioAtencionAdapter.java
- infraestructure/adapters/ReservaAdapter.java
- infraestructure/repositories/CanchaRepository.java
- infraestructure/repositories/HorarioAtencionRepository.java
- infraestructure/repositories/ReservaRepository.java
- infraestructure/mappers/CanchaMapper.java
- infraestructure/mappers/HorarioAtencionMapper.java
- infraestructure/controllers/CanchaController.java
- infraestructure/controllers/HorarioAtencionController.java
- infraestructure/controllers/dtos/CanchaRequest.java
- infraestructure/controllers/dtos/CanchaResponse.java
- infraestructure/controllers/dtos/HorarioAtencionRequest.java
- infraestructure/controllers/dtos/HorarioAtencionResponse.java

Pruebas nuevas bajo `src/test/java/org/example/sistema_canchas_deportivas/`:

- application/services/DisponibilidadTest.java
- application/services/GestionCanchasHorariosTest.java
- infraestructure/controllers/CanchasHttpTest.java
- infraestructure/persistence/CanchasMysqlTest.java

También se actualizan DatosInicialesServiceTest y AuthMysqlTest.
La prueba anterior del ADMIN usa ahora la ruta administrativa de reservas pendiente de Fase 5;
el CRUD real de canchas queda cubierto por las nuevas pruebas.

Documentación: FASE_4.md, docs/fase4.http y comentarios/ruta de ejemplo de docs/fase3.http.

## 4. Arquitectura

```text
CanchaController / HorarioAtencionController
  → puertos IN → servicios de aplicación
  → modelos y reglas de dominio
  → puertos OUT → adaptadores → repositorios JPA → MySQL
```

Los controladores validan el formato y convierten DTOs; no consultan repositorios.
Las reglas de superficie, compatibilidad y contención de horarios están en el dominio.
Los servicios coordinan transacciones y restricciones sobre reservas existentes.

ReservaOut se amplía exclusivamente con consultas necesarias para esta fase.
OcupacionCancha es una proyección de las reservas confirmadas que contiene cancha, disciplina,
fecha, inicio y duración. La consulta no carga contraseñas, responsables ni detalles de utilería.
No se han implementado creación o cancelación de reservas mediante HTTP.

## 5. Reglas y decisiones

### Canchas

- Nombre obligatorio, sin espacios externos y de hasta 100 caracteres.
- Nombre único: comprobación previa y restricción única de MySQL ante escrituras simultáneas.
  Se respeta la comparación de la base local, que también rechaza diferencias solo de mayúsculas.
- Tipo obligatorio entre FUTBOL_11, FUTBOL_6, BASQUET, VOLEY_ECUAVOLEY y TENIS.
- Fútbol 11 exige superficie NATURAL; fútbol 6 exige SINTETICA.
- Básquet, vóley/ecuavóley y tenis conservan superficie null si no se proporciona.
  No se asigna automáticamente ninguna superficie desconocida.
  El administrador puede declarar una superficie conocida con los valores del enum existente.
- El estado activa debe enviarse explícitamente; null o ausencia producen 400.
- Las disciplinas se derivan del tipo. VOLEY_ECUAVOLEY admite VOLEY y ECUAVOLEY
  sobre la misma cancha física.
- El catálogo incluye canchas inactivas con su estado; la disponibilidad las excluye.
- Una cancha con historial, incluso cancelado, no puede cambiar de tipo: evita alterar
  el significado de reservas históricas.
- Se rechaza desactivar una cancha con reservas confirmadas futuras o en curso.
  Deben resolverse esas reservas antes de desactivarla. La cancelación será parte de la Fase 5.
- La creación administrativa permite ampliar el catálogo; las 17 canchas corresponden a la carga inicial.

La carga inicial de canchas ahora se ejecuta solo con el catálogo vacío, dentro de una transacción.
Así un reinicio no recrea una cancha con su nombre anterior después de renombrarla.
Un catálogo existente parcial se conserva y puede completarse por administración.
Los horarios y la utilería mantienen su inicialización previa por día/nombre.

### Horarios

- Horario semanal del complejo, con apertura anterior al cierre dentro del mismo día.
- Precisión de minutos; no admite segundos, fracciones ni intervalos nocturnos que crucen medianoche.
- Se actualiza el día existente indicado en la ruta; no se cambia su identidad.
- Se revisan todas las reservas confirmadas futuras y en curso de ese día de la semana.
  Si alguna queda fuera del horario propuesto, se devuelve 409 y no se guarda el cambio.
- Las reservas finalizadas y las canceladas no impiden configurar horarios futuros.
- Terminar exactamente al cierre es válido.

### Disponibilidad

- Fecha actual o futura, disciplina válida y duración de 1 o 2 horas.
- La hora actual se obtiene del Clock configurado en America/Guayaquil.
- Los bloques comienzan en horas exactas, avanzando de hora en hora.
  Con apertura 08:30, el primer inicio posible es 09:00.
- El final nunca supera el cierre. No se muestran inicios ya transcurridos.
- Solo aparecen canchas activas, compatibles y con al menos un bloque libre.
- La ocupación procede exclusivamente de reservas CONFIRMADAS.
- Superposición mediante intervalos [inicio, fin): una reserva que acaba a las 12:00
  permite otra que empiece a las 12:00.
- Una reserva de vóley bloquea también ecuavóley en esa cancha, pero no otras canchas.
- Una respuesta vacía es 200 con canchas=[].
- No existe ninguna bandera global de ocupación.
- La respuesta no expone responsables ni IDs de reservas ajenas.

Esta es una consulta de disponibilidad física, no una garantía de reserva ni una evaluación del
límite diario personal. Crear una reserva y proteger su concurrencia corresponden a la Fase 5.

### Transacciones y siguiente fase

Las ediciones administrativas bloquean la fila de cancha u horario con PESSIMISTIC_WRITE
dentro de una transacción. Esto serializa modificaciones del mismo recurso.

La Fase 5 deberá participar del mismo protocolo: bloquear el horario del día antes de leerlo
y validar reservas, además del responsable y la cancha. Orden propuesto:
horario → responsable → cancha → utilería ordenada por ID cuando se incorpore.
La comprobación y creación deberán ocurrir en la misma transacción.
Los bloqueos actuales no se presentan como una implementación terminada de la concurrencia de reservas.

### Errores

Se mantiene la estructura JSON código/mensaje/fecha/detalles:

- 400: campos, fecha, enum, duración o intervalo inválidos.
- 401: falta autenticación en ruta protegida.
- 403: usuario autenticado sin rol ADMIN en una operación administrativa.
- 404: cancha o día configurado inexistente.
- 409: nombre duplicado o cambio incompatible con reservas/historial.
- 500: error inesperado sin SQL ni trazas internas.

El advice se limita a canchas y horarios; la integración global sigue pendiente de Fase 7.

## 6. Base de datos

No se cambió el esquema ni se agregaron tablas o dependencias.
Se conservaron el administrador, BCrypt, JWT y los catálogos previos.

Después de las pruebas MySQL: 1 usuario, 17 canchas, 7 horarios, 8 artículos de utilería y 0 reservas.
Los horarios iniciales siguen siendo 08:00–16:00 de lunes a jueves y 08:00–22:00 de viernes a domingo.
Las escrituras de prueba se revierten con rollback y no dejan canchas ni reservas de prueba.
El contador AUTO_INCREMENT puede avanzar durante pruebas revertidas; no implica pérdida de registros.

## 7. Compilación y pruebas

Comandos utilizados:

```powershell
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean compile

$env:RUN_MYSQL_TESTS = 'true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Las opciones offline y de bloqueo permiten usar la caché en el entorno restringido.
En una terminal normal también puedes ejecutar `.\mvnw.cmd test`.
Sin RUN_MYSQL_TESTS=true se omiten las 25 pruebas que necesitan MySQL.

Resultado final: **102 pruebas aprobadas, cero fallos, errores u omisiones**, incluidas
25 pruebas con MySQL 8.0.46 real. Se compilaron 102 fuentes principales.
Se mantienen las pruebas de las fases anteriores.

Cobertura agregada: siete días, duraciones, cierre, horas pasadas, zona horaria,
apertura con minutos, compatibilidad, inactivas, intervalos consecutivos y superpuestos,
canceladas, administración con JWT, 400/401/403/404/409, unicidad, conservación del historial,
conflictos al modificar horarios y conservación de ediciones al reinicializar.

Informes: target/surefire-reports y target/fase4-pruebas-mysql.log.
La compilación muestra un aviso de uso de sun.misc.Unsafe por Lombok con Java 25; no impide compilar ni ejecutar las pruebas.

También se arrancó el servidor con MySQL local y un JWT de ADMIN real. Se verificaron
catálogo y detalle públicos (200), ID inexistente (404), duración inválida (400),
horarios con JWT (200), administración sin JWT (401), nombre duplicado (409)
y actualización del lunes conservando sus valores (200).

Para ecuavóley y duración de dos horas, el lunes 2027-01-04 devolvió las cuatro canchas
compartidas con siete bloques por cancha y cierre a las 16:00; el viernes 2027-01-08
devolvió trece bloques por cancha y cierre a las 22:00.
El servidor temporal se detuvo al finalizar. Log de arranque: target/fase4-arranque.log.

## 8. Cómo comprobarlo

Usa la configuración JWT y el comando de arranque de FASE_3.md, con MySQL local activo:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Primero consulta GET /api/canchas y anota un ID real. Consulta disponibilidad con una fecha
actual o futura. Para administrar, inicia sesión en POST /api/auth/login y envía el JWT
como Authorization: Bearer.

Los ejemplos reproducibles están en docs/fase4.http. Incluyen registro de una cancha de ejemplo;
ejecutar esos ejemplos administrativos manualmente sí modifica tus datos.
Los ejemplos no contienen tokens ni secretos reales.

## 9. Pendientes

- Fase 5: creación y cancelación de reservas, límite diario, consultas privadas y concurrencia.
- Fase 6: utilería opcional y disponibilidad por intervalo.
- Fase 7: revisión global, manejo de errores y documentación final.

## 10. Próximo paso

Fase 4 finalizada. Preparado para continuar con la Fase 5 cuando se autorice.
