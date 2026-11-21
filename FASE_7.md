# Fase 7 — Validaciones, pruebas y documentación final

## 1. Alcance

Revisión integral de las fases 1–6 y preparación del contrato del backend para un futuro React.
Se mantienen Java 25, Spring Boot 4.1.2, Maven, JJWT y MySQL, sin cambios en pom.xml.
No se implementa frontend ni se amplía el alcance a pagos o administración de usuarios.

## 2. Manejo global de errores

GlobalExceptionHandler utiliza @RestControllerAdvice sin limitarse a un controlador.
Reemplaza los cuatro manejadores de autenticación, canchas, reservas y utilería, que repetían
las mismas traducciones de excepciones.

Se conservan las excepciones del dominio y los estados:

| Situación | HTTP / código |
|---|---|
| DatoInvalidoException | 400 DATO_INVALIDO |
| RecursoNoEncontradoException | 404 NO_ENCONTRADO |
| ReglaNegocioException | 409 CONFLICTO |
| Credenciales inválidas / autenticación en MVC | 401 CREDENCIALES_INVALIDAS |
| Acceso denegado de dominio o Spring | 403 ACCESO_DENEGADO |
| Jakarta Validation en DTOs | 400 VALIDACION, detalles por campo |
| JSON desconocido, incompleto o de tipo/formato inválido | 400 JSON_INVALIDO |
| Parámetros ausentes o de formato incorrecto | 400 SOLICITUD_INVALIDA |
| Restricciones de persistencia, concurrencia, tiempo de espera agotado | 409 CONFLICTO |
| Método, contenido o respuesta no soportados | 405, 415 o 406 |
| Recurso MVC no encontrado | 404 NO_ENCONTRADO |
| Excepción inesperada | 500 ERROR_INTERNO |

Los errores HTTP del framework conservan sus cabeceras, por ejemplo Allow.
El manejador anterior genérico podía transformar errores de protocolo en 500.
El nuevo manejador mantiene el estado original y un mensaje español.
Los errores genéricos no devuelven mensajes internos ni SQL; el log registra solo el tipo de excepción.

SecurityErrorHandler sigue siendo necesario para 401/403 de los filtros, antes de MVC.
Utiliza el mismo ErrorResponse: codigo, mensaje, fecha y detalles.
El rechazo de un origen CORS sucede antes de MVC y no asegura un cuerpo legible desde el navegador.

## 3. DTOs y reglas

Se revisaron todos los DTOs de entrada y su @Valid en controladores:
UsuarioRequest, LoginRequest, CanchaRequest, EstadoCanchaRequest, HorarioAtencionRequest,
ReservaRequest, ReservaUtileriaRequest, UtileriaRequest y ExistenciasRequest.

Todos los mensajes Jakarta Validation usados en ellos son explícitos en español, incluso
si el cliente envía Accept-Language: en. Se conservan longitudes, campos obligatorios,
positividad y duración de 1–2 horas, incluidas validaciones de listas anidadas.

La deserialización rechaza campos desconocidos, enteros enviados con decimales y ahora
enumerados enviados como índices numéricos. Las disciplinas y tipos se envían por nombre.
Las fechas y horas se reciben como LocalDate/LocalTime. Los servicios y modelos vuelven
a comprobar reglas de negocio, sin depender del frontend.

La auditoría creado/modificado conserva la implementación y el patrón de clase solicitados.
No se admiten fechas de auditoría en las solicitudes HTTP.

Las reglas de reservas, límite diario, intervalos [inicio, fin), cancelación, compatibilidad,
capacidad temporal de utilería y bloqueos de las fases anteriores se mantienen.

## 4. Revisión de seguridad

- JWTFilter es la única autenticación por Bearer; no hay sesiones ni autenticación por cookies.
- JWTServices exige clave Base64 de al menos 32 bytes, firma HS256, emisor y fechas válidas.
- UsuarioDetailsService carga estado y permisos actuales desde la base.
- BCrypt conserva las contraseñas existentes; el registro nunca acepta elegir ADMIN.
- AutorizacionReserva y los casos de uso verifican propiedad; ADMIN consulta todas las reservas.
- Las rutas administrativas exigen ADMIN; las no declaradas se deniegan.
- CORS permite los orígenes configurados de React; se exponen Location y WWW-Authenticate.
- root/root y admin123 solo son valores predeterminados del perfil local.
- Fuera de local se requieren credenciales propias y un esquema previamente migrado.
- LoginInterceptor mantiene la medición de tiempos de clase, sin duplicar autenticación.

## 5. Archivos creados y modificados

Rutas Java relativas a src/main/java/org/example/sistema_canchas_deportivas:

| Cambio | Archivos |
|---|---|
| Creado | infraestructure/exceptions/GlobalExceptionHandler.java |
| Sustituidos por el global | AuthExceptionHandler.java, CanchaExceptionHandler.java, ReservaExceptionHandler.java, UtileriaExceptionHandler.java |
| Mensajes de validación | infraestructure/controllers/dtos/CanchaRequest.java, EstadoCanchaRequest.java, ExistenciasRequest.java, HorarioAtencionRequest.java, LoginRequest.java, ReservaRequest.java, ReservaUtileriaRequest.java, UtileriaRequest.java |
| CORS | infraestructure/config/WebConfig.java |
| JSON estricto | src/main/resources/application.yaml |
| Pruebas nuevas | src/test/java/.../domain/models/ReglasDominioTest.java; infraestructure/controllers/ValidacionGlobalHttpTest.java |
| Adaptación al manejador global | src/test/java/.../infraestructure/controllers/CanchasHttpTest.java; infraestructure/securities/AuthHttpSecurityTest.java |
| Documentación nueva | README.md, .env.example, docs/fase7.http, FASE_7.md |
| Configuración privada ignorada | .gitignore |

UsuarioRequest ya tenía sus validaciones y mensajes; no necesitó modificación.
No se cambiaron entidades, repositorios, mappers, puertos ni reglas de servicios en esta fase.

## 6. Arquitectura y MySQL

Se mantiene Domain → Application mediante modelos y puertos; Infrastructure implementa
persistencia, REST y seguridad. Los servicios no importan repositorios ni entidades JPA.
El dominio no importa Spring ni Jakarta Persistence.

No hay migración ni reinserción de datos. Se mantienen las siete tablas:
usuarios, canchas, horarios_atencion, reservas, reserva_utileria, utileria y utileria_disciplinas.
Las relaciones, índices y claves existentes permanecen.

CHECKSUM TABLE antes y después de las pruebas y del arranque devolvió los mismos valores
para las siete tablas. Evidencias: docs/fase7-base-antes.txt y docs/fase7-base-despues.txt.
Esto verifica la conservación del contenido, incluidas contraseña y auditoría del administrador,
sin mostrar esos valores. Los contadores AUTO_INCREMENT pueden avanzar por las pruebas.

Estado final: 1 usuario, 17 canchas, 7 horarios, 8 implementos, 9 compatibilidades,
0 reservas y 0 detalles de reserva. Las ocho existencias siguen en cero.

## 7. Compilación y pruebas

Se ejecutó la compilación limpia mediante Maven Wrapper en modo offline, usando las
dependencias ya disponibles y el ajuste de bloqueo del entorno:

```powershell
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean compile
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
$env:RUN_MYSQL_TESTS='true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Resultados reales:

| Ejecución | Resultado |
|---|---|
| clean compile | BUILD SUCCESS, 112 fuentes de producción compiladas |
| Pruebas específicas iniciales | 44 aprobadas |
| Suite sin activar MySQL, después de corregir la configuración | 131 ejecutadas y aprobadas; 61 omitidas, según Surefire |
| Suite completa con RUN_MYSQL_TESTS=true | 194 aprobadas, 0 fallos, 0 errores, 0 omisiones |

La ejecución completa incluye 63 pruebas de MySQL y 13 de concurrencia.
La diferencia de conteo entre ambas suites incluye la expansión de pruebas parametrizadas
cuando se habilita la clase de integración. Se añadieron 22 casos a los 172 de la Fase 6.

Logs: target/fase7-compilacion.log, target/fase7-pruebas-iniciales.log,
target/fase7-pruebas-config-inicial.log (configuración inicial fallida),
target/fase7-pruebas.log y target/fase7-pruebas-mysql.log.
Los informes individuales finales están en target/surefire-reports.

Pruebas nuevas: siete de dominio puro y quince de HTTP/validación global, incluyendo nueve
casos parametrizados de los DTOs. Cubren intervalos, cancelación, auditoría, inmutabilidad,
horarios, superficies, validación en español, JSON estricto, estados 400/401/403/404/405/406/409/415/500
y cabeceras CORS.

Se conserva la cobertura anterior de servicios con mocks, JWT, BCrypt, propietarios,
repositorios, restricciones MySQL, auditoría y concurrencia real.
Las pruebas no cambian reglas de producción para conseguir un resultado favorable.

La compilación emite una advertencia de Lombok sobre sun.misc.Unsafe en Java 25.
No impide compilar; no se modificaron las versiones técnicas fijadas por el proyecto.

Durante la primera suite completa se detectó una configuración de enumerados en la sección
incorrecta de Jackson 3, que impedía cargar el contexto Spring. Se comprobó la API de las
bibliotecas instaladas y se corrigió a spring.jackson.datatype.enum.fail-on-numbers-for-enums.
El fallo y la ejecución posterior quedan diferenciados en los logs.

## 8. Arranque y comprobación HTTP

Se arrancó el backend con perfil local, MySQL real, una clave JWT aleatoria temporal y
puerto HTTP asignado automáticamente. El log confirmó que admin ya existía y conservó
su contraseña y rol, y que los catálogos estaban verificados.

Se aprobaron 18 comprobaciones HTTP reales: catálogo y disponibilidad públicos; login del
ADMIN existente; utilería, horarios y reservas administrativas autenticadas; falta de token;
token inválido; validación en español; rechazo de enums numéricos, cantidades decimales y
campos no permitidos; 405 con Allow; 415; 406; 404; preflight de React y rechazo de origen ajeno.
No se crearon registros durante estas comprobaciones.

Evidencias: target/fase7-arranque.log y target/fase7-http.json.
El servidor temporal se detuvo al terminar.

## 9. Cómo comprobar las funcionalidades

README.md documenta configuración local, variables de entorno, arquitectura, permisos, reglas,
todos los endpoints, ejemplos JSON y futura conexión con React mediante fetch.
.env.example es una referencia: no se importa automáticamente por Spring Boot.

docs/fase7.http permite recorrer catálogo → registro → login → disponibilidad → reserva →
consulta → conflicto → cancelación, además de administración y pruebas de errores.
Los ejemplos requieren fecha futura e IDs reales. Las solicitudes manuales crean datos reales.
Para utilería con stock, configura las cantidades físicas verificadas y utiliza docs/fase6.http.

Ejemplo mínimo de reserva (con cancha de tenis real):

```json
{"canchaId":16,"disciplina":"TENIS","fecha":"2027-01-04","horaInicio":"10:00","duracion":1,"utileria":[]}
```

## 10. Pendientes y siguiente paso

Fase 7 completada: compilación, pruebas unitarias/integración/concurrencia, arranque real,
contrato HTTP, conservación de MySQL y documentación final verificados.
Para utilizar implementos en reservas, el administrador debe registrar sus existencias reales.
El backend está preparado para conectar el frontend conforme al README.

El frontend React queda fuera de estas siete fases. Tampoco se implementaron pagos,
tarifas, facturación, refresh tokens, recuperación de contraseña ni CRUD de usuarios.
Las plantillas UsuarioController, ActionResponse y TimeAspect de la fase inicial no exponen
funciones REST. No hay una Fase 8 definida en el documento proporcionado.
