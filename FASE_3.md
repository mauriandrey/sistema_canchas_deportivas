# Fase 3 — Autenticación, JWT y seguridad

## 1. Alcance

Se implementó únicamente la fase 3 sobre el backend existente.
Se mantienen Java 25, Spring Boot 4.1.2, JJWT 0.12.6, MySQL, el POM y la arquitectura hexagonal.
No se modificó el esquema ni la inicialización del administrador o de los catálogos.
Las fases 4 a 7 permanecen pendientes.

## 2. Funcionalidades

- POST /api/auth/registro: registra un CLIENTE activo y devuelve HTTP 201.
- POST /api/auth/login: autentica con BCrypt y devuelve un JWT firmado.
- JWT HS256: firma, algoritmo, emisor, expiración, fecha de emisión e ID del usuario validados.
- Autenticación sin estado mediante Authorization: Bearer; sin cookies de autenticación ni sesiones HTTP.
- Consulta del estado y rol actuales del usuario en cada solicitud autenticada.
- Autorización de rutas administrativas y por propietario en futuras rutas de reservas.
- CORS configurable para React local.
- LoginInterceptor mide la duración de registro y login siguiendo el patrón del proyecto de clase.
- Errores de autenticación y autorización en JSON; validación de solicitudes y conflictos de registro.

## 3. Archivos

### Creados en el código principal

- src/main/java/org/example/sistema_canchas_deportivas/domain/exception/CredencialesInvalidasException.java
- src/main/java/org/example/sistema_canchas_deportivas/application/models/SesionAutenticada.java
- src/main/java/org/example/sistema_canchas_deportivas/application/ports/in/AuthIn.java
- src/main/java/org/example/sistema_canchas_deportivas/application/ports/out/AutenticacionOut.java
- src/main/java/org/example/sistema_canchas_deportivas/application/ports/out/TokenOut.java
- src/main/java/org/example/sistema_canchas_deportivas/application/services/AuthService.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/UsuarioPrincipal.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/adapters/AutenticacionAdapter.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/dtos/ErrorResponse.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/SecurityErrorHandler.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/AutorizacionReserva.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/exceptions/AuthExceptionHandler.java

### Modificados en el código principal

- src/main/java/org/example/sistema_canchas_deportivas/application/ports/in/UsuarioIn.java
- src/main/java/org/example/sistema_canchas_deportivas/application/services/UsuarioService.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/UsuarioDetailsService.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/JWTServices.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/JWTFilter.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/config/SecurityConfig.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/config/WebConfig.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/securities/LoginInterceptor.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/dtos/UsuarioRequest.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/dtos/LoginRequest.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/dtos/UsuarioResponse.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/dtos/LoginResponse.java
- src/main/java/org/example/sistema_canchas_deportivas/infraestructure/controllers/AuthController.java
- src/main/resources/application.yaml

### Pruebas y documentación

Nuevos:

- src/test/java/org/example/sistema_canchas_deportivas/application/services/RegistroClienteTest.java
- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/securities/JWTServicesTest.java
- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/securities/AuthHttpSecurityTest.java
- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/persistence/AuthMysqlTest.java
- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/securities/LoginInterceptorTest.java
- src/test/resources/application-test.yaml
- docs/fase3.env.example
- docs/fase3.http
- FASE_3.md

Modificados:

- SistemaCanchasDeportivasApplicationTests.java: comprueba 401 cuando falta autenticación.
- PersistenciaMysqlTest.java: activa también el perfil de pruebas, cuya clave JWT no se empaqueta en producción.

## 4. Arquitectura

Registro:

```text
AuthController → UsuarioIn → UsuarioService
  → PasswordOut → PasswordAdapter → BCryptPasswordEncoder
  → UsuarioOut → UsuarioAdapter → UsuarioRepository → MySQL
```

Login:

```text
AuthController → AuthIn → AuthService
  → AutenticacionOut → AutenticacionAdapter
  → AuthenticationManager / DaoAuthenticationProvider
  → UsuarioDetailsService → UsuarioOut → MySQL
  → TokenOut → JWTServices → SesionAutenticada → LoginResponse
```

Solicitudes autenticadas:

```text
JWTFilter → JWTServices (validación de firma y claims)
  → UsuarioDetailsService (ID, estado y rol actuales)
  → SecurityContext con UsuarioPrincipal
  → autorización por ruta y, cuando corresponde, propietario
```

Los servicios de aplicación no acceden a repositorios JPA ni utilizan clases de infraestructura.
Los modelos de dominio permanecen independientes de JPA y Spring.
Los DTOs de respuesta omiten la contraseña y su hash.

JWTFilter solo se instancia dentro de SecurityFilterChain. No es un bean de filtro de servlet,
por lo que no se registra dos veces. LoginInterceptor sí se registra mediante WebMvcConfigurer
exclusivamente para /api/auth/registro y /api/auth/login: mide tiempos y no realiza autenticación.
Así se utiliza el interceptor visto en clase sin duplicar el control de seguridad.

### Adaptación de los cuatro archivos de factura_36651

Los archivos proporcionados se leyeron como referencia de programación; no se modificó el proyecto de facturación.

| Archivo | Lógica de clase conservada | Adaptación al sistema de canchas |
|---|---|---|
| JWTServices | Keys.hmacShaKeyFor + Decoders.BASE64, generar con Jwts.builder, subject=username, claim rol, Date y expiración en milisegundos; leer devuelve Claims verificados. | Agrega usuarioId, emisor, algoritmo permitido y comprobaciones de identidad/fechas. generar(Usuario) adapta el resultado al puerto OUT; generar(username, rol, usuarioId) construye el token. |
| JWTFilter | OncePerRequestFilter, HttpHeaders.AUTHORIZATION, Bearer, jwtServices.leer, UsernamePasswordAuthenticationToken y SecurityContextHolder. | Consulta el usuario actual, comprueba que su ID coincide con el claim y utiliza su rol vigente. Los tokens inválidos producen 401 JSON. |
| UsuarioDetailsService | Implementa UserDetailsService, consulta UsuarioOut y construye User.withUsername(...).password(...).roles(...).disabled(...).build(). | Utiliza passwordHash del modelo actual y envuelve UserDetails en UsuarioPrincipal para conservar el ID necesario al comprobar reservas propias. |
| LoginInterceptor | HandlerInterceptor con preHandle, atributo de inicio y afterCompletion para registrar duración. | Usa System.nanoTime para medir tiempo transcurrido y registra método, ruta, estado y duración; excluye cuerpos, query strings, cabeceras, tokens y contraseñas. |

El rol incluido en el JWT mantiene el formato visto en clase. Para que un usuario desactivado o
con permisos modificados no conserve privilegios antiguos, la autorización utiliza el rol vigente
de MySQL. El claim usuarioId impide que un token antiguo se vincule a una cuenta distinta que
reutilice el mismo username.

## 5. Reglas y permisos

### Registro

- Username obligatorio: 3–50 caracteres ASCII entre letras, números, punto, guion y guion bajo.
- Se almacena en minúsculas, de forma consistente con el login y la unicidad de MySQL.
- Contraseña obligatoria: al menos 12 caracteres y como máximo 72 bytes UTF-8.
- No se aceptan hashes BCrypt como contraseñas de entrada.
- El rol siempre es CLIENTE y el estado inicial siempre es activo.
- Campos adicionales como rol, id o activo se rechazan con 400.
- Un username ya registrado devuelve 409. La restricción única de MySQL cubre además registros simultáneos.
- Se calcula un único hash BCrypt; el registro se realiza dentro de una transacción.

La clave académica admin123 conserva su uso exclusivo en la inicialización local.
El login no exige al administrador antiguo cumplir la nueva longitud mínima del registro.
El inicializador no cambia su clave ni su rol.

### JWT

El subject contiene el username, como en el proyecto de clase. El claim usuarioId contiene el ID
persistido, y rol conserva el nombre del rol. El filtro comprueba la identidad y obtiene los permisos
actuales de la base; no confía en un rol desactualizado del JWT.
Un usuario eliminado o desactivado deja de poder usar su token.

Se exige firma HS256, emisor correcto, username no vacío, usuarioId numérico positivo, rol válido y fechas de emisión/expiración válidas.
Se rechazan tokens sin firma, alterados, vencidos o con un algoritmo distinto.
No se incluyen contraseñas ni hashes en los claims.
La propiedad jwt.expiration usa milisegundos, como en clase: 3600000 por defecto (una hora),
con un rango de 1000 a 86400000. La respuesta expiresIn continúa expresándose en segundos.

El JWT es un token de acceso. Esta fase no incorpora refresh tokens ni un endpoint de logout.
Cambiar la clave de firma invalida los tokens anteriores.

### Matriz de rutas

| Ruta / método | Política |
|---|---|
| POST /api/auth/registro | Pública |
| POST /api/auth/login | Pública |
| GET /api/canchas y subrutas | Pública |
| /api/admin/** | ADMIN |
| GET /api/reservas/mis-reservas | CLIENTE o ADMIN autenticado |
| GET /api/reservas/{id} | Propietario o ADMIN |
| PATCH /api/reservas/{id}/cancelar | Propietario |
| POST /api/reservas | CLIENTE |
| GET /api/horarios, /api/utileria y subrutas de utilería | CLIENTE o ADMIN |
| Resto de rutas | Denegadas |

Sin autenticación se devuelve 401; con autenticación pero sin permiso, 403.
Para un cliente, una reserva inexistente o ajena se deniega con 403, sin revelar su existencia.
La fase 5 deberá usar también el usuario autenticado dentro de sus casos de uso y transacciones.

Las rutas de canchas, horarios, reservas y utilería todavía no tienen sus controladores REST.
Que la seguridad permita una ruta no significa que su funcionalidad esté implementada:
una solicitud autorizada puede recibir 404 hasta la fase correspondiente.
Los controladores auxiliares utilizados para probar esta matriz existen únicamente en src/test.

### CORS y errores

El perfil local admite por defecto http://localhost:5173 y http://localhost:3000.
CORS_ALLOWED_ORIGINS permite indicar una lista separada por comas. Fuera de local no se permiten
orígenes cruzados salvo que se configuren explícitamente.
Se permiten Authorization, Content-Type y Accept; no se habilitan credenciales mediante cookies.

CSRF está desactivado porque la autenticación solo utiliza un Bearer explícito y no cookies ni sesiones.
Login por formulario y HTTP Basic también están desactivados.

AuthExceptionHandler se limita al módulo de autenticación; el manejo completo de otros módulos
se ampliará en la fase 7. Los errores devuelven código, mensaje, fecha y detalles de validación,
sin valores de contraseñas, SQL interno ni trazas.
El rechazo de CORS lo gestiona Spring antes de los controladores.

## 6. Configuración

| Variable | Uso |
|---|---|
| JWT_SECRET | Obligatoria en cualquier perfil de ejecución. Base64 de al menos 32 bytes aleatorios. |
| JWT_EXPIRATION | Duración en milisegundos; 3600000 por defecto, mediante jwt.expiration. |
| JWT_ISSUER | Emisor; sistema_canchas_deportivas por defecto. |
| CORS_ALLOWED_ORIGINS | Orígenes React permitidos, separados por comas. |
| DB_URL, DB_USERNAME, DB_PASSWORD | Configuración MySQL de la fase 2. root/root solo tiene valores predeterminados en local. |
| ADMIN_INITIAL_PASSWORD | Solo se utiliza si aún no existe admin. |
| RUN_MYSQL_TESTS | Habilita las pruebas de integración cuando vale true. |
| ADMIN_TEST_PASSWORD | Clave actual del administrador para las pruebas de login; admin123 por defecto en esas pruebas locales. |

No hay una clave JWT fija de desarrollo en los recursos principales.
La clave predecible de application-test.yaml se usa solo en pruebas y no se empaqueta en la aplicación.
No actives el perfil test como configuración de despliegue.

Genera una clave de sesión sin imprimirla y arranca desde PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$generador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generador.GetBytes($jwtBytes)
$generador.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)

.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Genera la clave una vez y conserva la misma variable al reiniciar en esa terminal para mantener
válidos los tokens emitidos. En un despliegue, configura una clave persistente fuera del repositorio.
docs/fase3.env.example documenta las variables; Spring no carga automáticamente ese archivo.

## 7. Pruebas

Comandos ejecutados:

```powershell
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean compile
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test

$env:RUN_MYSQL_TESTS = 'true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Las opciones adicionales aprovechan la caché de dependencias y evitan archivos de bloqueo
en directorios restringidos. En una terminal normal pueden omitirse:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

Resultados:

- Compilación correcta con Java 25; 97 fuentes principales.
- Sin habilitar MySQL: 47 pruebas aprobadas y 15 de integración omitidas; cero fallos.
- Con MySQL 8.0.46 local: 62 pruebas aprobadas, cero fallos, errores u omisiones.
- Se conservan todas las pruebas de las fases anteriores.
- Nuevas verificaciones: validación del registro, rol fijo, BCrypt, duplicados, login de admin,
  credenciales incorrectas, cuentas desactivadas, firma/expiración/emisor/algoritmo del JWT,
  permisos por rol y propietario, ausencia de sesión, filtro único, CORS, identidad ligada al username
  y medición de tiempos sin modificar la autenticación ni registrar secretos.
- Las pruebas MySQL de registro se ejecutan con rollback; no dejan clientes de prueba almacenados.

Durante la verificación se corrigió la firma genérica de autorización requerida por Spring Security 7.
JJWT 0.12.6 no permite vaciar temporalmente su registro de algoritmos con clear();
se conservó HS256 eliminando los demás algoritmos de forma individual.
La prueba web importa explícitamente sus rutas auxiliares, sin añadirlas al código principal.
Ninguno de estos problemas permanece en la suite final.

Los informes se encuentran en target/surefire-reports y target/fase3-pruebas-mysql.log.

Se arrancó además el servidor con el perfil local, MySQL real y una clave JWT aleatoria.
Las solicitudes HTTP comprobaron login ADMIN (200, Bearer con 3600 segundos de vigencia),
credenciales incorrectas (401), acceso administrativo sin token (401), token inválido (401),
inyección de rol en registro (400), username duplicado (409) y preflight CORS de React (200).
Las consultas a canchas públicas y a canchas administrativas con JWT válido devolvieron 404,
como corresponde a controladores aún pendientes de la fase 4.
Se verificaron los registros de duración de LoginInterceptor para login y registro.
El servidor temporal se detuvo después de las comprobaciones.

La comparación de MySQL antes y después confirmó que el administrador conservó su ID,
username, rol, estado y contraseña. Permanecen 1 usuario, 17 canchas, 7 horarios, 8 artículos
de utilería y 0 reservas. No quedaron clientes de prueba almacenados.

## 8. Ejemplos HTTP

Los ejemplos completos están en docs/fase3.http y se pueden reproducir en Postman.

Registro:

```http
POST /api/auth/registro
Content-Type: application/json

{
  "username": "cliente.demo",
  "password": "ClaveLocalDemostracion2026!"
}
```

Respuesta 201: id generado, username normalizado, rol CLIENTE y activo=true.
El ID depende de la base; no se incluye el hash.

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "cliente.demo",
  "password": "ClaveLocalDemostracion2026!"
}
```

Respuesta 200: token, tipo Bearer, expiresIn en segundos, username y rol.
Copia el token recibido en Authorization: Bearer <token> para las solicitudes protegidas.
No lo incluyas en la URL.

Ejemplo de error por credenciales incorrectas (la fecha varía):

```json
{
  "codigo": "CREDENCIALES_INVALIDAS",
  "mensaje": "Credenciales inválidas o usuario inactivo.",
  "fecha": "2026-11-15T15:00:00Z",
  "detalles": {}
}
```

## 9. Pendientes

- Fase 4: endpoints y reglas de canchas, horarios y disponibilidad.
- Fase 5: creación, consulta y cancelación de reservas, límite diario y bloqueos de concurrencia.
- Fase 6: utilería opcional y disponibilidad temporal.
- Fase 7: validaciones y manejo de errores del sistema completo y documentación general.

## 10. Próximo paso

Fase 4 — Gestión de canchas y horarios. No se ha comenzado.

Referencias consultadas: [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html),
[CORS en Spring Security](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html) y
[JJWT](https://github.com/jwtk/jjwt).

