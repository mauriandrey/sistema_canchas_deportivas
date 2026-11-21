# Fase 6 — Utilería deportiva opcional

## 1. Alcance y resultado

Se completó la Fase 6 sobre el backend existente: administración de utilería, disponibilidad
por intervalo e incorporación opcional a las reservas.
Se conservan Java 25, Spring Boot 4.1.2, arquitectura hexagonal, JWT, MySQL y auditoría.
No se agregaron dependencias, tablas ni cambios de esquema. La Fase 7 no se ha iniciado.

## 2. Endpoints

| Método y ruta | Acceso | Resultado |
|---|---|---|
| GET /api/utileria | CLIENTE o ADMIN | Catálogo con existencias físicas, estado y compatibilidades |
| GET /api/utileria/disponibilidad | CLIENTE o ADMIN | Unidades comprometidas y disponibles durante el intervalo |
| POST /api/admin/utileria | ADMIN | 201, implemento creado |
| PUT /api/admin/utileria/{id} | ADMIN | 200, implemento actualizado |
| PATCH /api/admin/utileria/{id}/existencias | ADMIN | 200, existencias físicas actualizadas |
| POST /api/reservas | CLIENTE | 201, reserva con o sin implementos |

Parámetros de disponibilidad: fecha, horaInicio, duracion y disciplina.
Ejemplo: /api/utileria/disponibilidad?fecha=2027-01-04&horaInicio=10:00&duracion=2&disciplina=FUTBOL_11.

Los campos utileria omitido, null y [] representan una reserva sin implementos.
Los endpoints existentes de consulta y cancelación ahora devuelven también el detalle de utilería.

## 3. Archivos creados o modificados

Bajo src/main/java/org/example/sistema_canchas_deportivas/:

Creados:

- domain/models/CompromisoUtileria.java
- application/models/SolicitudUtileria.java
- application/models/UtileriaDisponible.java
- infraestructure/controllers/dtos/ExistenciasRequest.java
- infraestructure/controllers/dtos/ReservaUtileriaRequest.java
- infraestructure/controllers/dtos/ReservaUtileriaResponse.java
- infraestructure/controllers/dtos/UtileriaDisponibilidadResponse.java
- infraestructure/exceptions/UtileriaExceptionHandler.java

Completados o modificados:

- domain/models/Utileria.java
- domain/models/ReservaUtileria.java
- domain/models/Reserva.java
- application/ports/in/UtileriaIn.java
- application/ports/in/ReservaIn.java
- application/ports/out/UtileriaOut.java
- application/ports/out/ReservaOut.java
- application/services/UtileriaService.java
- application/services/ReservaService.java
- application/services/DatosInicialesService.java
- infraestructure/adapters/UtileriaAdapter.java
- infraestructure/adapters/ReservaAdapter.java
- infraestructure/repositories/UtileriaRepository.java
- infraestructure/repositories/ReservaRepository.java
- infraestructure/mappers/UtileriaMapper.java
- infraestructure/mappers/ReservaMapper.java
- infraestructure/controllers/UtileriaController.java
- infraestructure/controllers/ReservaController.java
- infraestructure/controllers/dtos/UtileriaRequest.java
- infraestructure/controllers/dtos/UtileriaResponse.java
- infraestructure/controllers/dtos/ReservaRequest.java
- infraestructure/controllers/dtos/ReservaResponse.java

Se reutilizan ReservaUtileriaMapper, las entidades JPA, relaciones y restricciones ya implementadas.
También se ajusta src/main/resources/application.yaml para rechazar números decimales
en campos enteros, en lugar de convertirlos silenciosamente.

Pruebas nuevas bajo src/test/java/org/example/sistema_canchas_deportivas/:

- application/services/UtileriaServiceTest.java
- infraestructure/persistence/UtileriaMysqlTest.java
- infraestructure/persistence/UtileriaConcurrenciaMysqlTest.java

Se ajustan ReservaServiceTest al nuevo puerto de utilería y ReservasMysqlTest:
utileria deja de ser un campo desconocido y pasa a ser un campo opcional válido.

Documentación: FASE_6.md y docs/fase6.http.

## 4. Arquitectura y patrón de clase

```text
UtileriaController → UtileriaIn → UtileriaService
  → UtileriaOut → UtileriaAdapter → UtileriaRepository → MySQL

ReservaController → ReservaIn → ReservaService
  → reglas de Reserva / ReservaUtileria / Utileria / CompromisoUtileria
  → bloqueos y consultas por puertos OUT
  → ReservaAdapter → reserva y detalles mediante cascada JPA
```

El dominio conserva constructores completos, fábricas nuevo(...) y métodos actualizar(...)
que devuelven modelos inmutables. Las fechas creado/modificado siguen administradas por infraestructura.
La aplicación no conoce entidades JPA, DTOs HTTP ni repositorios concretos.

SolicitudUtileria transporta solo utileriaId y cantidad entre controlador y caso de uso.
ReservaUtileria.nuevo(...) valida IDs y cantidades positivas.
Reserva.conUtileria(...) conserva identidad y fechas y rechaza implementos repetidos.

CompromisoUtileria contiene únicamente disciplina, fecha, inicio, duración y cantidad:
la disponibilidad no expone responsables ni reservas privadas.

## 5. Catálogo y validaciones

Se mantienen los ocho implementos iniciales:

| Implemento | Disciplinas iniciales |
|---|---|
| Balón fútbol número 5 | FUTBOL_11 |
| Balón fútbol número 4 | FUTBOL_6 |
| Balón básquet número 7 | BASQUET |
| Balón Mikasa V200W | VOLEY |
| Balón Mikasa FT-5 | ECUAVOLEY |
| Chalecos | FUTBOL_11, FUTBOL_6 |
| Raquetas de tenis | TENIS |
| Tubos de 3 pelotas de tenis | TENIS |

Las cantidades físicas continúan en cero hasta que el administrador declare las existencias reales.
Una unidad de TUBO_PELOTAS representa un tubo completo de tres pelotas.

### Administración

- Nombre obligatorio, hasta 100 caracteres y único. Se eliminan espacios externos.
- Tipo válido: BALON, CHALECO, RAQUETA o TUBO_PELOTAS.
- Existencias enteras no negativas, indicadas explícitamente.
- Estado activa obligatorio.
- Al menos una disciplina compatible válida.
- La restricción única de MySQL protege también escrituras simultáneas con nombres repetidos.
- No se puede cambiar el tipo de un implemento con historial, incluso cancelado.
- No se permite desactivar un implemento ni quitar una compatibilidad utilizada por reservas
  confirmadas futuras o en curso.
- No se permite reducir existencias por debajo del máximo simultáneo comprometido pendiente.
- Las reservas finalizadas o canceladas no bloquean la configuración futura.

La carga inicial de utilería ahora se ejecuta solo cuando su catálogo está vacío.
Renombrar un implemento inicial ya no causa que se recree con el nombre anterior al reiniciar.
Un catálogo existente parcial se conserva y se completa mediante administración.

### Solicitud dentro de una reserva

- La lista es opcional; cada elemento presente debe ser no nulo.
- El ID debe existir y ser positivo.
- El implemento debe estar activo y admitir la disciplina de la reserva.
- La cantidad es un entero estrictamente positivo.
- No se puede repetir un implemento, aunque las cantidades sean diferentes.
- Debe haber unidades suficientes durante todo el intervalo.
- Si falla cualquier detalle, no se guarda ninguna parte de la reserva.

La creación mantiene todas las reglas de Fase 5: responsable del JWT, duración, horario,
límite diario, estado de cancha y conflictos de cancha/responsable.

## 6. Disponibilidad y reutilización

La disponibilidad valida fecha actual o futura, inicio en hora exacta, duración de 1 o 2 horas,
disciplina y horario de atención del complejo.

Se muestran implementos activos y compatibles, incluidos los que tienen cero unidades disponibles.
Para cada implemento:

```text
disponibles = existencias físicas − máximo de unidades simultáneamente comprometidas
```

Se consultan únicamente reservas CONFIRMADAS y se trabaja con intervalos [inicio, fin).
Los eventos de entrada y salida se agrupan por instante; una salida a las 11:00 libera la unidad
para una entrada a las 11:00. Se usan sumas long para evitar desbordar un entero en el cálculo.

Ejemplo: con dos balones físicos, una reserva utiliza uno de 10:00 a 11:00 y otra lo utiliza
de 11:00 a 12:00. Durante el intervalo 10:00–12:00 el máximo comprometido es uno, por lo que
todavía puede asignarse el otro balón durante las dos horas.
Sumar indiscriminadamente todas las reservas que tocan el intervalo rechazaría esta asignación válida.

La reserva guarda cantidades en reserva_utileria. No descuenta existencias ni actualiza la
auditoría del catálogo por el simple hecho de usarlo.
Cancelar cambia el estado de la reserva y conserva los detalles históricos; al excluirse
la cancelada de las consultas, sus unidades vuelven a estar disponibles.

La consulta pública de canchas sigue siendo pública. Las consultas de utilería requieren JWT,
conforme a la política de seguridad existente.

## 7. Transacciones, concurrencia y auditoría

Se amplía el orden de bloqueos de Fase 5:

```text
Horario → Responsable → Cancha → Utilería por ID ascendente → Reserva al cancelar
```

Cada implemento se bloquea con PESSIMISTIC_WRITE. La disponibilidad se comprueba después del bloqueo,
y la reserva junto con sus detalles se guarda en la misma transacción.
Cancelar adquiere también los bloqueos de sus implementos antes de modificar el estado.
La edición administrativa bloquea el implemento antes de consultar sus compromisos.

Se mantiene READ_COMMITTED y el límite transaccional de 15 segundos.
El horario semanal sigue usando un bloqueo exclusivo conservador, que serializa solicitudes
para el mismo día de la semana. No se presenta como una optimización para alto volumen.

La auditoría detectó un caso adicional: modificar solo las disciplinas compatibles cambia
una @ElementCollection, sin disparar necesariamente @PreUpdate del implemento.
UtileriaAdapter ahora utiliza el mismo AuditoriaListener para marcar esa modificación cuando
la colección cambia. Se conserva creado, se usa el mismo Clock y guardar sin cambios no
reescribe la auditoría.

Errores: 400 para datos inválidos o incompatibles, 401 sin autenticación, 403 sin permiso,
404 para implemento inexistente y 409 para falta de capacidad, duplicados o cambios incompatibles.
Los errores se devuelven en JSON sin SQL ni trazas.

## 8. Pruebas y datos existentes

Verificación final:

```powershell
$env:RUN_MYSQL_TESTS = 'true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Resultado: **172 pruebas aprobadas; cero fallos, errores u omisiones**, incluidas
63 pruebas con MySQL real. Se mantienen todas las pruebas de las fases anteriores.
La compilación utiliza las versiones existentes del proyecto.

Se añadieron 31 pruebas: 12 de reglas y cálculo, 12 de integración y siete de concurrencia.
Estas últimas comprueban:

1. Solo una solicitud obtiene la última unidad, aunque las canchas sean distintas.
2. Dos solicitudes de dos unidades no comprometen cuatro cuando solo hay tres.
3. Reservas consecutivas reutilizan una misma unidad.
4. Listas de implementos en orden inverso se procesan sin interbloqueo.
5. Una reserva espera una reducción de stock y observa el nuevo valor confirmado.
6. Una reducción de stock espera una reserva en curso y rechaza invalidar su compromiso.
7. La cancelación libera la unidad antes de la nueva asignación.

La primera ejecución de integración detectó el problema de auditoría al cambiar solo disciplinas.
Se corrigió en UtileriaAdapter y la prueba pasó en la ejecución final.

Las pruebas ordinarias usan rollback. Las concurrentes necesitan commits y limpian únicamente
sus registros identificados por ID. Los contadores AUTO_INCREMENT pueden avanzar.
Informes: target/surefire-reports y target/fase6-pruebas-mysql.log.

La comprobación posterior confirmó 1 usuario, 17 canchas, 7 horarios, 8 implementos,
9 compatibilidades, 0 reservas y 0 detalles de reserva.
Los ocho implementos iniciales conservaron sus existencias cero y sus fechas de auditoría.
Se conserva el administrador y no se volvió a ejecutar la reinserción de datos.

Se arrancó además el servidor y se verificó por HTTP el catálogo autenticado (200, ocho implementos),
la disponibilidad de vóley (200, cero unidades), la consulta sin JWT (401), un inicio no exacto (400),
existencias negativas (400), actualización conservando existencias cero (200) y nombre duplicado (409).
El servidor temporal se detuvo al terminar. Log: target/fase6-arranque.log.

## 9. Cómo comprobarlo

Usa JWT_SECRET y el perfil local según FASE_3.md:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

1. Inicia sesión y consulta GET /api/utileria.
2. Como ADMIN, configura únicamente las cantidades físicas que hayas verificado.
3. Como CLIENTE, consulta disponibilidad para fecha, hora, duración y disciplina.
4. Crea una reserva incluyendo utileria, o sin ese campo si no necesitas implementos.
5. Consulta la reserva y comprueba los detalles.
6. Cancela antes del inicio y vuelve a consultar disponibilidad.

Ejemplo de detalle opcional:

```json
{
  "canchaId": 1,
  "disciplina": "FUTBOL_11",
  "fecha": "2027-01-04",
  "horaInicio": "10:00",
  "duracion": 2,
  "utileria": [
    {"utileriaId": 1, "cantidad": 1},
    {"utileriaId": 6, "cantidad": 2}
  ]
}
```

Los IDs son ilustrativos: usa los devueltos por tu catálogo.
Con existencias cero, una solicitud de implementos devuelve 409; una reserva sin implementos sigue siendo válida.
docs/fase6.http contiene solicitudes para administración, disponibilidad, reserva y cancelación.
Los ejemplos manuales guardan datos reales.

## 10. Próximo paso

Fase 6 finalizada. Queda pendiente la Fase 7: revisión integral, validaciones, manejo global
de errores, pruebas y documentación final. Se espera autorización para iniciarla.
