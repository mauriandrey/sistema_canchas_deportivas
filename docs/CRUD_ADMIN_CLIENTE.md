# CRUD administrativo y registro con perfil Cliente

Actualización: 20 de noviembre de 2026, America/Guayaquil.

## Decisión vigente

La solicitud reemplaza la decisión anterior de mantener Cliente independiente de las cuentas.
Usuario conserva credenciales, rol y estado de acceso. Cliente guarda cédula, nombres,
apellidos, email y teléfono. Las nuevas cuentas se crean siempre con rol CLIENTE y perfil
personal; el administrador del sistema mantiene ADMIN.

Se conservan los patrones de clase: domain/models, entities con creado/modificado,
controladores/DTOs, servicios, repositorios y mappers. El login sigue directamente en
AuthController con AuthenticationManager y JWTServices. No se agregan puertos de autenticación,
aspectos ni application/models. PasswordOut sigue encargándose de BCrypt.

## Registro y cuentas

POST /api/auth/registro es público. POST /api/admin/usuarios exige ADMIN y utiliza el mismo cuerpo:

```json
{
  "username": "cliente.demo",
  "password": "ClaveDeEjemplo2027",
  "cedula": "0100000009",
  "nombres": "Ana",
  "apellidos": "Ejemplo",
  "email": "ana@example.test",
  "telefono": "0991234567"
}
```

Todos estos campos son obligatorios para registrar una cuenta. Se conservan las reglas del
username y contraseña; cédula ecuatoriana de exactamente 10 dígitos, nombres/apellidos hasta 100 cada uno,
email válido hasta 100 y teléfono de 7 a 10 dígitos. Cédula y email son únicos. Se validan
región, tercer dígito y dígito verificador por módulo 10; consultar [VALIDACION_CEDULA.md](VALIDACION_CEDULA.md).
El correo y el username se normalizan a minúsculas.

No se aceptan rol, activo, clienteId ni auditoría en el registro. Tampoco se vincula
automáticamente un perfil existente usando la cédula o email: eso permitiría apropiarse de
un perfil ajeno. Un conflicto devuelve 409 y no deja registros parciales.

Cliente y Usuario se insertan en una transacción. La contraseña se codifica una sola vez.
La respuesta 201 incluye el vínculo, sin contraseña ni hash:

```json
{"id":2,"username":"cliente.demo","rol":"CLIENTE","activo":true,"clienteId":1}
```

El login permanece POST /api/auth/login con username/password. Continúa devolviendo
ActionResponse con result.username, result.rol y result.token. Los JWT consultan la cuenta
activa y su rol actual; eliminar/desactivar la cuenta impide seguir utilizando sus tokens.

PUT /api/admin/usuarios/{id} utiliza:

```json
{"username":"cliente.demo","activo":true,"clienteId":1,"password":"NuevaClaveDeEjemplo2027"}
```

Omitir password o enviarlo null conserva el hash. No acepta rol. Una cuenta ya vinculada
no puede transferirse a otra persona ni perder su perfil. Una cuenta CLIENTE anterior que
aún no tenga perfil puede vincularse explícitamente a un Cliente existente, sin cuenta,
mediante este PUT. La cuenta ADMIN conserva identidad/rol/estado activo y no se elimina;
sí puede actualizar su contraseña omitiendo clienteId o enviándolo null.

El CRUD de Cliente sigue permitiendo crear personas sin cuenta para preparar su vínculo.
Su teléfono continúa siendo opcional en ese catálogo, por compatibilidad; es obligatorio
en el registro de nuevas cuentas. Actualizar Cliente edita los datos personales sin alterar
el username, la contraseña ni el rol.

## Rutas CRUD de ADMIN

Todas las rutas de esta tabla comienzan con /api/admin y exigen JWT de ADMIN.
GET de colección y recurso individual están disponibles en cada fila.

| Entidad | Colección | Identificador del recurso | POST | GET | PUT | DELETE |
|---|---|---|---|---|---|---|
| Cancha | /canchas | /{id} | 201 | 200 | 200 | 204 |
| HorarioAtencion | /horarios | /{dia} | 201 | 200 | 200 | 204 |
| Utileria | /utileria | /{id} | 201 | 200 | 200 | 204 |
| Cliente | /clientes | /{id} | 201 | 200 | 200 | 200 |
| Usuario | /usuarios | /{id} | 201 | 200 | 200 | 204 |
| Reserva | /reservas | /{id} | 201 | 200 | 200 | 204 |
| ReservaUtileria | /reservas/{id}/utileria | /{utileriaId} | 201 | 200 | 200 | 204 |

POST va a la colección y PUT/DELETE al recurso individual. Los detalles se identifican por
la combinación reserva/implemento; no se reciben IDs de detalle ajenos. Cliente conserva
ActionResponse y DELETE 200. Las otras entidades conservan DTO/listas directas; DELETE 204
no contiene JSON. Los cambios de detalles devuelven la reserva actualizada.

Se mantienen PATCH estado de cancha y existencias de utilería. Se añade
PATCH /api/admin/reservas/{id}/cancelar para cancelar una reserva ajena conservando el historial.
Las enumeraciones y utileria_disciplinas son valores/colecciones, no entidades con CRUD propio.

Para crear un horario:

```json
{"dia":"MONDAY","apertura":"08:00","cierre":"16:00"}
```

El día no se reasigna en PUT: se identifica en la ruta. No puede haber dos horarios para
el mismo día. Si se elimina, ese día queda sin atención configurada hasta crearlo nuevamente.

Para crear o actualizar administrativamente una reserva:

```json
{
  "responsableId": 2,
  "canchaId": 5,
  "disciplina": "VOLEY",
  "fecha": "2027-01-04",
  "horaInicio": "10:00",
  "duracion": 1,
  "estado": "CONFIRMADA",
  "utileria": [{"utileriaId":4,"cantidad":1}]
}
```

Sustituir IDs/fecha por registros y disponibilidad reales. responsableId sigue siendo el ID de
la cuenta CLIENTE, no el de Cliente: se conserva el contrato de propiedad de las reservas
existentes. Su perfil se obtiene a través de Usuario.clienteId.

POST crea CONFIRMADA; estado puede omitirse o ser CONFIRMADA. En PUT el estado es obligatorio
y puede ser CONFIRMADA/CANCELADA. El PUT reemplaza todos los datos de la reserva y su lista
de implementos; utileria omitida, null o [] elimina sus detalles. Se conservan ID, creado y
fechaCreacion; los detalles que mantienen el mismo implemento también conservan ID y creado.

POST /reservas/{id}/utileria recibe utileriaId y cantidad; PUT del detalle recibe solo cantidad:

```json
{"cantidad":2}
```

## Permisos y reglas de integridad

CLIENTE conserva el flujo personal: crear una reserva para sí, consultar sus reservas y
cancelarlas antes del inicio. No puede indicar otro responsable ni entrar a /api/admin/**.
Las lecturas públicas del catálogo de canchas y las lecturas autenticadas de horarios/utilería
se mantienen. Los perfiles personales y la lista de cuentas no son públicos.

El administrador sigue respetando duración de 1–2 horas, horas exactas, fecha futura,
compatibilidad de disciplina, atención configurada, cuenta CLIENTE activa, límite de dos
horas por día, cruces de reservas y disponibilidad de implementos. Una edición excluye
la misma reserva al calcular ocupación, límite y stock. Reactivar una cancelada vuelve a
verificar todos esos límites. Editar una reserva o sus detalles exige un intervalo actual
o futuro; cancelar exige que aún no haya comenzado. DELETE permite retirar una reserva
histórica, pero no se ejecuta automáticamente.

Eliminar tiene estas reglas:

- Reserva: elimina físicamente la reserva y únicamente sus propios detalles. Libera sus compromisos.
- ReservaUtileria: elimina solo ese detalle; la reserva se mantiene.
- Cancha/Utileria: 409 si conservan referencias de reservas/detalles. Se pueden desactivar o
  eliminar explícitamente las relaciones antes de borrar el registro.
- Horario: 409 si hay reservas confirmadas pendientes ese día de semana.
- Usuario CLIENTE: 409 si tiene reservas. Eliminar la cuenta conserva el perfil Cliente.
- Cliente: 409 si tiene una cuenta vinculada. Se elimina primero la cuenta cuando sea permitido.
- ADMIN del sistema: no se elimina ni desactiva para conservar el acceso administrativo.

Los cambios de reservas usan transacciones READ_COMMITTED con timeout de 15 segundos.
Adquieren horarios, responsables, canchas e implementos en orden y después bloquean la
reserva. Al moverla se bloquean recursos anteriores y nuevos; si otra operación cambió sus
referencias mientras esperaba, se devuelve 409 para volver a consultar. No hay cascadas
desde Reserva hacia Usuario/Cliente/Cancha/Utileria.

## Migración y datos anteriores

Se añadió usuarios.cliente_id nullable con clave foránea a cliente(id) y unicidad de vínculo.
Los nuevos registros de cuenta CLIENTE siempre llevan un perfil; el valor nullable permite
conservar ADMIN y cuentas históricas que nunca guardaron datos personales.

La migración [sql/vincular_usuario_cliente.sql](sql/vincular_usuario_cliente.sql) es aditiva e
idempotente. Ya se aplicó a la base local. Se compararon huellas de los datos originales de
las siete entidades antes/después y coincidieron. No se inventaron cédulas, nombres o correos,
ni se borraron cuentas o reservas existentes. Evidencia: target/crud-migracion-verificacion.txt.

ADMIN debe completar los perfiles anteriores con datos reales: crear Cliente y vincularlo
mediante PUT de Usuario. No se asignan personas automáticamente por nombres similares.
Las cuentas anteriores conservan su acceso y propiedad de reservas mientras se completa ese proceso.

APP_INITIALIZE_CATALOGS es false por defecto. La carga inicial de catálogos ahora se activa
solo explícitamente, para que reiniciar no restaure horarios o catálogos eliminados por ADMIN.
Activarla solo al preparar una base inicial; las tablas deben existir porque se mantiene
ddl-auto=validate en la configuración actual. El administrador inicial conserva su flujo previo.

## Integración pendiente del frontend

El backend queda implementado; esta solicitud no modifica el proyecto frontend hermano.
Su formulario antiguo enviaba solo username/password y ahora recibirá 400 hasta añadir los
cinco campos personales. También debe incorporar las acciones nuevas de ADMIN y tratar
correctamente DELETE 204. El login y las operaciones de reservas ya existentes conservan sus rutas.
Los prompts históricos del frontend deben leerse junto con este contrato actualizado.

## Verificación

Pruebas: matriz de permisos en 36 rutas/operaciones, validación del perfil, CRUD y relaciones
con MySQL real, conservación de auditoría, eliminación de detalles, registro atómico con fallo
inducido después de insertar Cliente, conflictos y ediciones concurrentes.

La carga de prueba usa datos sintéticos con rollback. Las pruebas de concurrencia hacen commit
y limpian exclusivamente sus fixtures por ID. Los contadores AUTO_INCREMENT pueden avanzar.
Se corrigió la precisión de fechaCreacion a microsegundos para que no cambie tras recargar desde MySQL.

Comando completo:

```powershell
$env:RUN_MYSQL_TESTS='true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' package
Remove-Item Env:RUN_MYSQL_TESTS
```

Resultado: **274 pruebas aprobadas**, sin fallos, errores ni omisiones; 80 con MySQL y
16 de concurrencia incluidas en esas 80. Memoria: [ESTADO_BACKEND.md](../ESTADO_BACKEND.md).
Las pruebas están registradas en target/crud-final.log. Después de descargarse las dependencias
de empaquetado faltantes, target/crud-package.log confirma BUILD SUCCESS y la creación del JAR
ejecutable en target/sistema_canchas_deportivas-0.0.1-SNAPSHOT.jar.
Ejemplos manuales: [crud-admin.http](crud-admin.http). No se ejecutan automáticamente.
