# Ajuste de modelos y entidades al patrón de clase

Actualización posterior: Cliente se incorporó como séptimo modelo y entidad auditable,
con constructor completo, nuevo, actualizar y el mismo listener de persistencia.
Consulta docs/CRUD_ADMIN_CLIENTE.md para el vínculo Usuario–Cliente y el CRUD vigente. AJUSTES_CLASE.md es histórico.

## Análisis de los archivos proporcionados

Se revisaron Cliente.java, Producto.java, Usuario.java y sus entidades del proyecto factura_36651.

El patrón de clase separa tres responsabilidades:

1. El **constructor completo del dominio** recibe todos los atributos, incluidos creado y modificado,
   para reconstruir un registro leído de la base.
2. Las fábricas **nuevo(...)** reciben datos de negocio y dejan ID y fechas sin asignar.
   Las operaciones de actualización conservan la identidad y los metadatos.
3. La **entidad JPA** administra la auditoría durante la persistencia:
   al crear asigna creado y modificado al mismo instante; al actualizar cambia solamente modificado.

Los atributos de auditoría son LocalDateTime. No representan la fecha y hora de uso de una cancha.
No se deben pedir al cliente HTTP ni calcular usando la hora que envíe el navegador.

## Aplicación en el proyecto

Se agregaron creado, modificado y sus getters a estos seis modelos y entidades:

| Dominio | Entidad | Tabla |
|---|---|---|
| Usuario | UsuarioEntity | usuarios |
| Cancha | CanchaEntity | canchas |
| HorarioAtencion | HorarioAtencionEntity | horarios_atencion |
| Utileria | UtileriaEntity | utileria |
| Reserva | ReservaEntity | reservas |
| ReservaUtileria | ReservaUtileriaEntity | reserva_utileria |

Los enums no representan registros y no llevan auditoría.
utileria_disciplinas es la tabla auxiliar de una colección de disciplinas, no una entidad independiente.

### Dominio

Cada modelo tiene un constructor completo con ambos LocalDateTime y un constructor compatible
con las fases anteriores. Se mantienen clases de dominio independientes de JPA y Spring.

En Usuario, Cancha y HorarioAtencion se incorpora la fábrica nuevo(...).
Cancha y HorarioAtencion también tienen actualizar(...) conservando ID y fechas.
Los servicios operativos utilizan esas fábricas y actualizaciones.

Los ejemplos de clase modifican el mismo objeto. Este proyecto mantiene su diseño inmutable:
actualizar(...) devuelve una nueva instancia y la anterior conserva sus valores.
No se incorporan setters públicos para cambiar arbitrariamente la auditoría.

Ejemplo:

```java
Cancha cancha = Cancha.nuevo("Tenis adicional", TipoCancha.TENIS, null, true);
// id, creado y modificado todavía son null.

Cancha persistida = canchaOut.guardar(cancha);
// La persistencia devuelve ID y fechas reales.

Cancha editada = persistida.actualizar("Tenis adicional - norte",
        persistida.getTipo(), persistida.getSuperficie(), true);
Cancha resultado = canchaOut.guardar(editada);
// creado se conserva; modificado cambia si hubo una modificación persistida.
```

### Entidades JPA

En cada entidad se declaran explícitamente:

```java
@Column(nullable = false, updatable = false)
private LocalDateTime creado;

@Column(nullable = false)
private LocalDateTime modificado;
```

Se mantienen los eventos @PrePersist y @PreUpdate del ejemplo de clase.
Están centralizados en AuditoriaListener, registrado mediante @EntityListeners en las seis entidades.
El listener llama a alCrear(ahora) y alModificar(ahora) mediante el contrato RegistroAuditable.

Esta pequeña diferencia permite inyectar el Clock existente del proyecto y respetar
APP_TIMEZONE, cuyo valor predeterminado es America/Guayaquil.
Se evita depender de la zona predeterminada del equipo mediante LocalDateTime.now() sin Clock.
Los instantes se ajustan a microsegundos, compatibles con DATETIME(6) en MySQL.

Los callbacks JPA también pueden declararse en un listener, además de hacerlo directamente
en una entidad, como permiten [PrePersist](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/prepersist)
y [EntityListeners](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/persistence/EntityListeners.html).
La integración del listener con el Clock fue comprobada con Hibernate y MySQL reales.

### Mappers y adaptadores

Los seis mappers copian las fechas desde la entidad al constructor completo del dominio.

Al insertar, la persistencia asigna las fechas: toEntity no copia fechas propuestas por el modelo.
Al editar, el adaptador recupera la auditoría almacenada antes de hacer merge.
Esto evita perder creado cuando se guarda una instancia construida con un constructor antiguo
o sobrescribirlo con un valor inventado. En reservas se conserva también la auditoría de sus detalles.

La columna creado no participa en UPDATE.
Guardar un registro sin cambios no obliga a ejecutar UPDATE ni cambia modificado.
Reinicializar los catálogos tampoco reescribe la auditoría.

Reserva conserva fechaCreacion, ya utilizada en las fases anteriores, como fecha de negocio.
creado representa la inserción del registro y modificado su última actualización persistida.
Se conserva esta distinción para respetar los datos y contratos existentes; las pruebas comprueban
que registrar la auditoría no altera fechaCreacion.

Los DTOs de entrada siguen recibiendo únicamente los datos del caso de uso.
Las fechas quedan disponibles en dominio y persistencia, sin añadir campos de entrada que permitan manipularlas.

## Reinserción solicitada

El usuario solicitó eliminar los registros existentes e ingresarlos nuevamente.

Se ejecutó docs/sql/ajuste_auditoria_reinsercion.sql sobre la base local
sistema_canchas_deportivas el **2026-11-15 a las 18:43:03.628982, hora de Guayaquil**.

Procedimiento:

1. Agregar las columnas de auditoría inicialmente como nullable para permitir la transición.
2. Copiar los registros a tablas temporales de la misma conexión.
3. Dentro de una transacción, eliminar de hijos a padres y reinsertar de padres a hijos.
4. Conservar IDs, datos de negocio, relaciones, cantidades y hash del administrador.
5. Comprobar cantidades y conservación de contraseña/rol antes de confirmar.
6. Establecer ambas columnas como NOT NULL.

Las fechas nuevas indican la reinserción, no la creación histórica anterior.
No se eliminó la base ni se desactivaron las claves foráneas.

Estado final:

| Tabla | Registros |
|---|---:|
| usuarios | 1 |
| canchas | 17 |
| horarios_atencion | 7 |
| utileria | 8 |
| utileria_disciplinas | 9 |
| reservas | 0 |
| reserva_utileria | 0 |

El script es el registro de una operación puntual autorizada; no se ejecuta al arrancar
y no se debe repetir como migración rutinaria. En otros entornos se necesita una migración
controlada adecuada a sus datos existentes antes de arrancar con ddl-auto=validate.

## Archivos

Bajo src/main/java/org/example/sistema_canchas_deportivas/:

- Modificados los seis modelos listados, sus seis entidades y seis mappers.
- Modificados los adaptadores UsuarioAdapter, CanchaAdapter, HorarioAtencionAdapter,
  UtileriaAdapter y ReservaAdapter para conservar auditoría desde la base.
- Modificados UsuarioService, CanchaService y HorarioAtencionService para usar el patrón de dominio.
- Creados infraestructure/auditoria/RegistroAuditable.java y AuditoriaListener.java.

Pruebas nuevas:

- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/auditoria/AuditoriaTest.java
- src/test/java/org/example/sistema_canchas_deportivas/infraestructure/persistence/AuditoriaMysqlTest.java

Documentación y operación local:

- AUDITORIA.md
- docs/sql/ajuste_auditoria_reinsercion.sql

## Verificación

```powershell
$env:RUN_MYSQL_TESTS = 'true'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test
Remove-Item Env:RUN_MYSQL_TESTS
```

Resultado: **110 pruebas aprobadas, cero fallos, errores u omisiones**.
Incluye 30 pruebas con MySQL real y la suite anterior de autenticación, canchas y horarios.
La compilación y las pruebas usan las versiones existentes del POM.

Se comprobó:

- Asignación inicial de ambas fechas y uso de la zona configurada.
- Preservación de creado durante ediciones.
- Actualización de modificado, también en detalles de reserva.
- Lectura de auditoría mediante los seis mappers.
- Conservación de fechas al guardar sin cambios o reinicializar.
- Rechazo efectivo de fechas inventadas en el modelo al guardar.
- Columnas NOT NULL y precisión de microsegundos.
- Login del administrador y funcionamiento de las fases previas.

Los cambios de cada prueba de MySQL se revierten con rollback.
Los informes están en target/surefire-reports y target/auditoria-mysql.log.

La auditoría corresponde a operaciones realizadas mediante el ciclo de vida de JPA.
SQL directo y actualizaciones masivas requieren gestionar explícitamente las fechas;
la reinserción puntual anterior las asignó dentro de la propia operación.
No es un historial completo de cambios ni registra quién realizó cada edición.

La Fase 5 sigue pendiente de autorización.
