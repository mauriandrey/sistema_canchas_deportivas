# Fase 1 — Estructura del backend y administrador inicial

## Diagnóstico de partida

Se revisaron el POM completo, el wrapper, las fuentes y la prueba existente antes de editar.
El proyecto contenía únicamente la clase principal, application.properties y contextLoads.
Se conservaron las coordenadas Maven, Java 25, Spring Boot 4.1.2 y todas las dependencias.
La propiedad jjwt.version ya estaba definida como 0.12.6. No fue necesario modificar pom.xml ni añadir librerías.
No había repositorio Git y no se inicializó uno.

El wrapper ejecutó Maven 3.10.0 sobre Oracle JDK 25.0.5.0.3.
La prueba original fallaba al cargar el contexto por falta de configuración de DataSource.
El acceso TCP a localhost:3306 no estuvo disponible; tampoco se encontraron variables DB_* configuradas.
No se modificaron datos de una base existente.

## A. Estructura completa del código y la configuración

Se conserva la única clase @SpringBootApplication original.
Se omiten únicamente los metadatos locales de .idea y los resultados generados de target.
Las carpetas static y templates ya existían y siguen vacías.

```text
sistema_canchas_deportivas/
├── .gitattributes
├── .gitignore
├── .mvn
│   └── wrapper
│       └── maven-wrapper.properties
├── FASE_1.md
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── org
    │   │       └── example
    │   │           └── sistema_canchas_deportivas
    │   │               ├── SistemaCanchasDeportivasApplication.java
    │   │               ├── application
    │   │               │   ├── ports
    │   │               │   │   ├── in
    │   │               │   │   │   ├── CanchaIn.java
    │   │               │   │   │   ├── HorarioAtencionIn.java
    │   │               │   │   │   ├── ReservaIn.java
    │   │               │   │   │   ├── UsuarioIn.java
    │   │               │   │   │   └── UtileriaIn.java
    │   │               │   │   └── out
    │   │               │   │       ├── CanchaOut.java
    │   │               │   │       ├── HorarioAtencionOut.java
    │   │               │   │       ├── PasswordOut.java
    │   │               │   │       ├── ReservaOut.java
    │   │               │   │       ├── UsuarioOut.java
    │   │               │   │       └── UtileriaOut.java
    │   │               │   └── services
    │   │               │       ├── CanchaService.java
    │   │               │       ├── HorarioAtencionService.java
    │   │               │       ├── ReservaService.java
    │   │               │       ├── UsuarioService.java
    │   │               │       └── UtileriaService.java
    │   │               ├── domain
    │   │               │   ├── exception
    │   │               │   │   ├── DatoInvalidoException.java
    │   │               │   │   ├── RecursoNoEncontradoException.java
    │   │               │   │   └── ReglaNegocioException.java
    │   │               │   └── models
    │   │               │       ├── Cancha.java
    │   │               │       ├── Disciplina.java
    │   │               │       ├── EstadoReserva.java
    │   │               │       ├── HorarioAtencion.java
    │   │               │       ├── Reserva.java
    │   │               │       ├── ReservaUtileria.java
    │   │               │       ├── Rol.java
    │   │               │       ├── Superficie.java
    │   │               │       ├── TipoCancha.java
    │   │               │       ├── Usuario.java
    │   │               │       └── Utileria.java
    │   │               └── infraestructure
    │   │                   ├── adapters
    │   │                   │   ├── CanchaAdapter.java
    │   │                   │   ├── HorarioAtencionAdapter.java
    │   │                   │   ├── PasswordAdapter.java
    │   │                   │   ├── ReservaAdapter.java
    │   │                   │   ├── UsuarioAdapter.java
    │   │                   │   └── UtileriaAdapter.java
    │   │                   ├── aspects
    │   │                   │   └── TimeAspect.java
    │   │                   ├── config
    │   │                   │   ├── SecurityConfig.java
    │   │                   │   ├── UsuarioInicial.java
    │   │                   │   └── WebConfig.java
    │   │                   ├── controllers
    │   │                   │   ├── AuthController.java
    │   │                   │   ├── CanchaController.java
    │   │                   │   ├── HorarioAtencionController.java
    │   │                   │   ├── ReservaController.java
    │   │                   │   ├── UsuarioController.java
    │   │                   │   ├── UtileriaController.java
    │   │                   │   └── dtos
    │   │                   │       ├── ActionResponse.java
    │   │                   │       ├── CanchaRequest.java
    │   │                   │       ├── CanchaResponse.java
    │   │                   │       ├── HorarioAtencionRequest.java
    │   │                   │       ├── HorarioAtencionResponse.java
    │   │                   │       ├── LoginRequest.java
    │   │                   │       ├── LoginResponse.java
    │   │                   │       ├── ReservaRequest.java
    │   │                   │       ├── ReservaResponse.java
    │   │                   │       ├── UsuarioRequest.java
    │   │                   │       ├── UsuarioResponse.java
    │   │                   │       ├── UtileriaRequest.java
    │   │                   │       └── UtileriaResponse.java
    │   │                   ├── entities
    │   │                   │   ├── CanchaEntity.java
    │   │                   │   ├── HorarioAtencionEntity.java
    │   │                   │   ├── ReservaEntity.java
    │   │                   │   ├── ReservaUtileriaEntity.java
    │   │                   │   ├── UsuarioEntity.java
    │   │                   │   └── UtileriaEntity.java
    │   │                   ├── mappers
    │   │                   │   ├── CanchaMapper.java
    │   │                   │   ├── HorarioAtencionMapper.java
    │   │                   │   ├── ReservaMapper.java
    │   │                   │   ├── UsuarioMapper.java
    │   │                   │   └── UtileriaMapper.java
    │   │                   ├── repositories
    │   │                   │   ├── CanchaRepository.java
    │   │                   │   ├── HorarioAtencionRepository.java
    │   │                   │   ├── ReservaRepository.java
    │   │                   │   ├── UsuarioRepository.java
    │   │                   │   └── UtileriaRepository.java
    │   │                   └── securities
    │   │                       ├── JWTFilter.java
    │   │                       ├── JWTServices.java
    │   │                       ├── LoginInterceptor.java
    │   │                       └── UsuarioDetailsService.java
    │   └── resources
    │       ├── application-mysql.yaml
    │       ├── application.yaml
    │       ├── static
    │       └── templates
    └── test
        ├── java
        │   └── org
        │       └── example
        │           └── sistema_canchas_deportivas
        │               ├── SistemaCanchasDeportivasApplicationTests.java
        │               ├── application
        │               │   └── services
        │               │       └── UsuarioServiceTest.java
        │               └── infraestructure
        │                   └── config
        │                       └── UsuarioInicialTest.java
        └── resources
            └── mockito-extensions
                ├── org.mockito.plugins.MemberAccessor
                └── org.mockito.plugins.MockMaker
```

Los paquetes conservan las grafías académicas solicitadas: infraestructure y securities.
Las clases de fases posteriores son estructuras compilables con comentarios que indican su estado.
Los repositorios futuros todavía no extienden JpaRepository y las entidades futuras todavía no llevan @Entity:
esto impide generar tablas ajenas al alcance de la fase 1.
Los servicios futuros reciben su puerto OUT por constructor, pero todavía no son beans de Spring.
Los DTOs están reservados sin inventar contratos HTTP antes de implementar sus casos de uso.

## B. Archivos funcionales

| Archivo | Responsabilidad |
|---|---|
| UsuarioInicial.java | Configuración con CommandLineRunner, consulta por username, validación de la configuración inicial y logs sin contraseñas. |
| Usuario.java | Modelo Java independiente de JPA con id, username, passwordHash, rol y activo; no guarda la clave original. |
| UsuarioIn.java | Contrato crearAdministradorInicial; recibe una clave original y devuelve si se creó el usuario. |
| UsuarioOut.java | Contrato de persistencia: existePorUsername y guardar. |
| PasswordOut.java | Puerto de salida para codificar la contraseña sin acoplar el servicio a BCrypt. |
| UsuarioService.java | Único caso de uso funcional, transaccional; evita sobrescribir admin, valida la clave, solicita un único hash y guarda el modelo con rol ADMIN. |
| PasswordAdapter.java | Implementación adicional mínima de PasswordOut que delega en el PasswordEncoder configurado. |
| UsuarioAdapter.java | Implementa UsuarioOut y delega en el mapper y el repositorio; guarda y fuerza el envío a la base dentro de la transacción. |
| UsuarioEntity.java | Única entidad JPA: tabla usuarios, id autoincremental, username único, password_hash de 60 caracteres, rol como texto y activo. |
| UsuarioMapper.java | Convierte dominio y entidad conservando el hash; nunca lo vuelve a codificar. |
| UsuarioRepository.java | Repositorio Spring Data JPA con existsByUsername. |
| SecurityConfig.java | Beans BCryptPasswordEncoder y SecurityFilterChain; todas las rutas permanecen denegadas en esta fase. |
| application.yaml | Nombre del proyecto, perfiles, JPA, zona horaria y contraseña inicial por entorno. |
| application-mysql.yaml | Conexión MySQL mediante variables de entorno. |

application.properties se sustituyó por application.yaml para evitar dos fuentes de configuración con distinta prioridad.
No se cambió el nombre de la clase principal ni la configuración Maven.

## C. Creación automática del administrador

1. Spring configura MySQL y JPA. Con ddl-auto: update solo se crea o ajusta la tabla usuarios cuando sea necesario.
2. Spring ejecuta el CommandLineRunner de UsuarioInicial.
3. UsuarioInicial consulta UsuarioOut.existePorUsername("admin"), que llega a UsuarioRepository mediante UsuarioAdapter.
4. Si existe ese username, termina sin actualizar su contraseña ni su rol. La presencia de otros clientes no impide crear admin.
5. Si no existe, obtiene app.admin.initial-password y comprueba la política del entorno.
6. Invoca UsuarioIn, implementado por UsuarioService. El servicio vuelve a comprobar la existencia y solicita el hash una sola vez mediante PasswordOut.
7. PasswordAdapter usa BCryptPasswordEncoder. UsuarioService construye Usuario con rol ADMIN y activo=true.
8. UsuarioAdapter convierte con UsuarioMapper y persiste mediante UsuarioRepository dentro de la transacción del servicio.
9. Tras completar el caso de uso, UsuarioInicial registra que se creó el administrador. Si la persistencia falla, el error se propaga.
10. En un reinicio, la consulta por username evita una segunda inserción y conserva el hash existente.

La restricción única de username también impide duplicados en la base. Si dos instancias intentan hacer el primer arranque simultáneamente, una puede fallar por esa restricción; la fase no implementa reintentos distribuidos.
Si ya existe un CLIENTE con username admin, se conserva sin promoverlo automáticamente y el log informa que ese usuario ya existe.

### Contraseñas y perfiles

- Perfil local explícito: usa ADMIN_INITIAL_PASSWORD si está configurada; en su ausencia admite admin123 como referencia académica.
- Perfil mysql predeterminado: exige ADMIN_INITIAL_PASSWORD al crear admin, con al menos 12 caracteres.
- Cualquier perfil sin local aplica la misma exigencia. Use una clave larga, única y segura fuera del desarrollo local.
- Se rechazan claves vacías, claves mayores de 72 bytes UTF-8 y hashes BCrypt recibidos como si fueran claves originales.
- Si admin ya existe, la contraseña de inicialización deja de ser necesaria y no sirve para restablecerlo.
- Los perfiles de despliegue distintos de local deben incluir mysql para cargar la conexión, por ejemplo SPRING_PROFILES_ACTIVE=prod,mysql.

Solo UsuarioService solicita la codificación, una única vez. Mapper, entidad y repositorio reciben el hash.
No hay endpoint de login todavía: crear admin no habilita autenticación HTTP en esta fase.
No se registran los filtros JWT ni los interceptores vacíos.

### Arranque local en PowerShell

Con MySQL 8.x iniciado, configura las credenciales de tu instalación en esta terminal:

```powershell
$env:DB_USERNAME = Read-Host 'Usuario MySQL'
$env:DB_PASSWORD = Read-Host 'Contraseña MySQL' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run
```

El perfil local activa también mysql. Si no defines ADMIN_INITIAL_PASSWORD, la clave inicial será admin123.
Puedes reemplazarla antes del primer arranque con:

```powershell
$env:ADMIN_INITIAL_PASSWORD = Read-Host 'Contraseña inicial del administrador' -MaskInput
```

Fuera de local, usa SPRING_PROFILES_ACTIVE=mysql y define explícitamente ADMIN_INITIAL_PASSWORD.
DB_URL es opcional; su valor predeterminado apunta a jdbc:mysql://localhost:3306/sistema_canchas_deportivas?createDatabaseIfNotExist=true.
El usuario MySQL debe tener los permisos necesarios para crear la base si aún no existe y para ajustar la tabla usuarios.
También puede usarse una base previamente creada, sin eliminar ni reiniciar sus datos.

La fase usa ddl-auto=update para el esquema mínimo con las dependencias existentes. No hay migraciones versionadas todavía.
Antes de ampliar el esquema se debe decidir la herramienta de migraciones y pasar a validar el esquema gestionado por ellas.

### Comprobación manual con MySQL

Tras arrancar la aplicación, ejecuta estas consultas de solo lectura en el cliente MySQL:

```sql
USE sistema_canchas_deportivas;
SELECT DATABASE();
SELECT COUNT(*) AS administradores_por_username
FROM usuarios WHERE username = 'admin';
SELECT id, username, rol, activo,
       CHAR_LENGTH(password_hash) = 60
       AND LEFT(password_hash, 4) IN ('$2a$', '$2b$', '$2y$') AS formato_bcrypt
FROM usuarios WHERE username = 'admin';
```

Debe existir una sola fila admin con rol ADMIN, activo=true y formato_bcrypt=1.
La comprobación SQL verifica el formato; las pruebas Java verifican que BCrypt.matches reconoce la clave original.
Reinicia con los mismos datos y repite la consulta: el contador debe seguir en 1.
Para comprobar que el hash se conserva sin imprimirlo, guarda antes del reinicio en la misma sesión SQL:

```sql
SET @hash_anterior = (SELECT password_hash FROM usuarios WHERE username = 'admin');
-- Reiniciar la aplicación y ejecutar en esta misma conexión SQL:
SELECT password_hash = @hash_anterior AS hash_sin_cambios
FROM usuarios WHERE username = 'admin';
```

## D. Verificación

Comandos ejecutados sobre el proyecto:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean compile
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
```

- Maven 3.10.0 / Oracle JDK 25.0.5.0.3.
- Compilación: BUILD SUCCESS, 80 fuentes principales con release 25.
- Pruebas finales: BUILD SUCCESS, 12 ejecutadas, 0 fallos, 0 errores y 0 omitidas.
- UsuarioServiceTest: 4 pruebas de conservación del usuario, claves vacías, límite UTF-8 y rechazo de un hash como entrada.
- UsuarioInicialTest: 7 pruebas del flujo, incluidas dos inicializaciones con una sola escritura, BCrypt verificable, cliente preexistente, política de contraseña y propagación de errores de persistencia.
- SistemaCanchasDeportivasApplicationTests: 1 prueba que carga el contexto de Spring, ejecuta el inicializador y verifica 403 en rutas de administración y autenticación.
- Solo las pruebas sustituyen UsuarioRepository con Mockito y excluyen DataSource; ninguna de estas pruebas prueba una conexión real o una transacción MySQL.

El primer intento normal de Maven no pudo escribir los archivos de bloqueo en la caché de usuario restringida.
La opción -o usa dependencias ya disponibles y rwlock-local usa bloqueos en memoria; así no fue necesario modificar archivos fuera del workspace.
En una terminal normal con acceso a la caché puedes ejecutar .\mvnw.cmd clean compile y .\mvnw.cmd test.

Los primeros intentos de pruebas se detuvieron por el acceso denegado a la tubería de conexión del agente dinámico de Mockito.
Se configuraron mock-maker-subclass y member-accessor-reflection en src/test/resources/mockito-extensions.
Las pruebas usan interfaces y clases no finales, por lo que no necesitan un agente JVM ni modificar pom.xml.
Tras ese ajuste, las 12 pruebas finalizaron correctamente. Lombok emite un aviso sobre sun.misc.Unsafe; no impide compilar ni ejecutar las pruebas.

La configuración también excluye la creación automática del usuario en memoria de Spring Boot para que no se genere otra contraseña en los logs.
Spring Security y la cadena que deniega las rutas permanecen activos.

MySQL no estuvo accesible en localhost:3306 y no había credenciales DB_* configuradas.
No se inició la aplicación contra una base real ni se afirma haber insertado admin en MySQL.
La comprobación real de la tabla, el reinicio y la conservación del hash queda descrita en la sección C.

## E. Pendientes de siguientes fases

- CRUD de canchas, horarios, usuarios y utilería, con DTOs y endpoints.
- Reglas de reservas y cancelación, disponibilidad y transacciones con bloqueo para concurrencia.
- Relaciones y restricciones JPA restantes, migraciones y pruebas de integración sobre MySQL.
- Autenticación, JWT, BCrypt en el futuro registro de clientes, permisos por rol, CORS y errores HTTP.
- Datos iniciales idempotentes: 17 canchas, horarios y catálogo de utilería.
- Frontend React.

Se conserva como decisión de negocio pendiente de implementar el límite de 2 horas acumuladas por responsable y día, excluyendo canceladas.
El tipo VOLEY_ECUAVOLEY representa una cancha física compartida; Disciplina mantiene VOLEY y ECUAVOLEY separados.
No se inventan superficies desconocidas ni existencias: los modelos admiten null para datos aún no especificados.
Las futuras reglas temporales deberán usar app.timezone (America/Guayaquil por defecto), no la zona del servidor.
No se implementaron las reglas de negocio en esta fase.

## Referencias técnicas consultadas

La configuración usa las API documentadas de [autorización HTTP de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) y [PasswordEncoder/BCrypt](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
La compilación con las dependencias existentes verifica los imports usados; el comportamiento JWT queda pendiente de su fase.

