# Adaptación al código de clase y catálogo Cliente

Fecha: 16 de noviembre de 2026.

> Documento histórico. La decisión de Cliente independiente y el alcance limitado de ADMIN
> fueron reemplazados por la solicitud del 20 de noviembre. El contrato vigente está en
> [CRUD_ADMIN_CLIENTE.md](docs/CRUD_ADMIN_CLIENTE.md); se conservan los patrones de clase de este informe.

## Decisiones solicitadas

Se revisaron los archivos aportados del proyecto factura_36651 y sus componentes relacionados.
Cliente se implementa como catálogo independiente de Usuario, según la opción elegida:
no crea cuentas, no se enlaza automáticamente con reservas y sus datos personales son accesibles
solo para ADMIN.

## Autenticación como en clase

AuthController recibe AuthenticationManager y JWTServices por constructor.
El login autentica mediante UsernamePasswordAuthenticationToken, obtiene username, rol e ID
del principal autenticado y genera el JWT directamente.

Se eliminaron AuthIn, AutenticacionOut, TokenOut, AuthService, AutenticacionAdapter y
SesionAutenticada. No existe una capa de puertos específica para el login.
PasswordOut utiliza los métodos de clase encriptar y coincide, implementados mediante BCrypt.

UsuarioDetailsService conserva UsuarioOut para recuperar las cuentas; el mismo archivo de clase
usa esa dependencia de persistencia. Los puertos del resto del negocio permanecen.
El registro pasó a UsuarioController manteniendo POST /api/auth/registro y su respuesta previa.

LoginRequest conserva el record username/password, Jakarta Validation y protección del password
en serialización y toString. Se mantienen normalización del username, límite de 72 bytes UTF-8,
usuarios activos, BCrypt y el manejo genérico de credenciales incorrectas.

ActionResponse ahora es record ActionResponse<T>(String message, T result).
LoginResponse es record LoginResponse(String username, String rol, String token).
El token permanece protegido en toString. La respuesta de POST /api/auth/login es:

```json
{
  "message": "Sesión iniciada",
  "result": {
    "username": "admin",
    "rol": "ADMIN",
    "token": "JWT_GENERADO"
  }
}
```

**Cambio del contrato HTTP:** el token se obtiene desde result.token, no desde token en la raíz.
Los campos tipo y expiresIn del antiguo DTO se retiraron para seguir el record de clase.
Se sigue enviando Authorization: Bearer TOKEN. JWT_EXPIRATION configura la vigencia en
milisegundos y el token mantiene su claim exp. JWTServices conserva firma, emisor, fechas
y usuarioId para impedir usar un token de otra identidad con el mismo username.

## Sin aspectos ni modelos de aplicación

Se retiraron TimeAspect, su directorio y spring-boot-starter-aspectj del pom.xml.
Se conservan las transacciones Spring y su infraestructura necesaria; no hay aspectos propios.
LoginInterceptor sigue midiendo tiempos como interceptor MVC, sin autenticar.

DisponibilidadCancha, OcupacionCancha, ReferenciaReserva, SolicitudUtileria y UtileriaDisponible
se trasladaron de application/models a domain/models. Son valores del negocio sin dependencias
de Spring, JPA o HTTP. Se ajustaron imports, proyecciones JPQL y pruebas.
La carpeta application/models ya no existe.

## Implementación de Cliente

Archivos nuevos:

- domain/models/Cliente.java: constructor completo, fábrica nuevo, actualización del mismo
  objeto y getters, incluidos creado/modificado, siguiendo la clase.
- application/ports/in/ClienteIn.java y ports/out/ClienteOut.java.
- application/services/ClienteService.java: casos de uso y unicidad.
- infraestructure/entities/ClienteEntity.java: tabla cliente, campos privados, getters y setters
  de datos de negocio con Lombok.
- infraestructure/repositories/ClienteRepository.java: búsquedas de cédula/email ignorando
  mayúsculas y excluyendo el mismo ID al actualizar.
- infraestructure/adapters/ClienteAdapter.java: carga la entidad existente y actualiza sus
  datos sin copiar ID ni auditoría desde la solicitud.
- infraestructure/mappers/ClienteMapper.java: métodos estáticos como en clase, incluidas
  las conversiones toRequest, toResponse y toEntity sobre una entidad existente.
- infraestructure/controllers/ClienteController.java y DTOs ClienteRequest/ClienteResponse.

Las rutas son GET/POST /api/admin/clientes y GET/PUT/DELETE /api/admin/clientes/{id}.
Todas exigen ADMIN y devuelven ActionResponse. Crear devuelve 201 y Location.
El resto del sistema conserva sus rutas y sus respuestas anteriores.
CORS permite DELETE para el catálogo desde los orígenes autorizados.

Campos y reglas:

| Campo | Regla |
|---|---|
| cedula | Obligatoria, hasta 13 caracteres, única; sin validar dígito verificador |
| nombres y apellidos | Obligatorios, hasta 100 caracteres cada uno |
| email | Obligatorio, formato de correo, hasta 100 caracteres, único y normalizado a minúsculas |
| telefono | Opcional; entre 7 y 10 dígitos si se proporciona; vacío se guarda como null |
| creado | Asignado al insertar; se conserva al actualizar |
| modificado | Asignado al insertar y actualizado al modificar |

El dominio valida antes de modificar sus atributos. Las solicitudes no aceptan id, creado,
modificado ni campos desconocidos. La unicidad se verifica en el servicio y también en MySQL,
de modo que una carrera no pueda insertar duplicados.

La auditoría usa los mismos eventos @PrePersist/@PreUpdate del ejemplo, centralizados
en AuditoriaListener para conservar Clock, America/Guayaquil y precisión de microsegundos.
El listener no es un aspecto. Ahora cubre siete entidades.

DELETE elimina físicamente el registro del catálogo independiente, como en clase;
no elimina Usuario ni Reserva. Las reglas existentes que conservan el historial de reservas
y canchas no cambian.

## Base de datos

Se añade únicamente la tabla cliente, con ID autoincremental, índices únicos de cédula/email
y auditoría DATETIME(6) NOT NULL. No se inventan personas ni se cargan clientes automáticamente.
El perfil local crea la tabla mediante ddl-auto=update.
Para entornos con validate se incluye docs/sql/agregar_cliente.sql como migración aditiva de referencia.

La tabla cliente quedó creada y vacía. Se verificaron sus ocho columnas, longitudes,
restricciones NOT NULL y fechas DATETIME(6).
Las siete tablas preexistentes conservaron exactamente sus checksums antes y después:
docs/ajustes-clase-base-antes.txt y docs/ajustes-clase-base-despues.txt.
No se modificaron el administrador, los catálogos ni las reservas existentes.
Las pruebas concurrentes pueden avanzar los contadores AUTO_INCREMENT.

## Verificación

```powershell
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean test
$env:RUN_MYSQL_TESTS='true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

La compilación limpia y las pruebas sin MySQL terminaron con BUILD SUCCESS:
144 ejecutadas y aprobadas; 65 omitidas explícitamente.
La suite completa terminó con **211 aprobadas, cero fallos, errores u omisiones**:
67 pruebas MySQL y 13 de concurrencia.
La diferencia de conteo incluye casos parametrizados que solo se expanden al habilitar MySQL.

Pruebas nuevas: ClienteServiceTest (6), ClientesHttpTest (5), ClientesMysqlTest (4)
y dos casos adicionales de autenticación. Se actualizaron las pruebas del JWT, login,
auditoría, registro, puertos PasswordOut y las importaciones de modelos trasladados.
Se conserva toda la verificación anterior de reservas, utilería, concurrencia y propiedad.

Logs: target/ajustes-clase-pruebas.log y target/ajustes-clase-mysql.log.
Los informes individuales están en target/surefire-reports.
La advertencia de Lombok sobre sun.misc.Unsafe en Java 25 no impidió compilar.

El backend arrancó con MySQL real y perfil local. Se comprobaron seis casos HTTP:
login ADMIN 200 con result.token; catálogo Cliente 200 vacío; acceso sin JWT 401;
cliente inválido 400 sin insertar; catálogo público con las 17 canchas; preflight DELETE
de React 200. El log confirmó la conservación de admin.
Evidencias: target/ajustes-clase-arranque.log y target/ajustes-clase-http.txt.
El servidor temporal se detuvo después de verificar.

## Cómo usarlo

README.md documenta el contrato actualizado. docs/clientes.http incluye login, creación,
consulta, actualización y eliminación de un registro de ejemplo, ejecutables uno por uno.
Los archivos HTTP de las fases anteriores también indican que el JWT está en result.token.
No se inició frontend ni se modificaron las reglas de reservas o existencias.
