# Validación de cédula ecuatoriana

Actualización: 20 de noviembre de 2026. Este ajuste complementa
[CRUD ADMIN y perfil Cliente](CRUD_ADMIN_CLIENTE.md) y se implementa únicamente en el backend.

## Regla aplicada

La cédula se recibe como texto con exactamente 10 dígitos ASCII (0–9), conservando ceros
iniciales. No se admiten espacios, guiones, letras, RUC de 13 dígitos ni números Unicode.
Se valida región 01–24 y tercer dígito de 0 a 5. También se admite la serie consular 30,
con tercer dígito 4 o 5. El [Registro Civil](https://www.registrocivil.gob.ec/preguntas-frecuentes-cedulacion/)
describe las series 01–24 y 30; el [reporte técnico de Gob.EC](https://gitlab.gobiernoelectronico.gob.ec/mintel/ge/rutr/gobec/-/issues/597)
documenta el caso consular y su tercer dígito.

Para comprobar el décimo dígito se usa módulo 10:

1. Multiplicar los primeros nueve dígitos por 2, 1, 2, 1, 2, 1, 2, 1, 2.
2. Restar 9 a cada producto mayor que 9.
3. Sumar los resultados.
4. Calcular `(10 - suma % 10) % 10` y comparar con el último dígito.

La cédula de prueba proporcionada pasa el algoritmo: suma 38, verificador 2.
Se utiliza en pruebas locales del algoritmo y HTTP; no se creó una cuenta ni una persona
real con ese número. Esta comprobación es matemática y no consulta existencia, titularidad
o vigencia en el Registro Civil.

## Aplicación en las capas

- `domain/models/CedulaEcuatoriana`: algoritmo compartido, sin Spring ni JPA.
- `Cliente.nuevo` y `Cliente.actualizar`: validan antes de modificar los atributos.
- `@CedulaValida`: validación de los DTOs ClienteRequest y UsuarioRequest; reutiliza el
  algoritmo del dominio y asocia los errores a `cedula`.
- `ClienteEntity`: misma validación al persistir/actualizar por JPA y columna VARCHAR(10).
- Cédula única: se conservan la comprobación del servicio y el índice de MySQL.

Rutas cubiertas: POST/PUT /api/admin/clientes, POST /api/auth/registro y
POST /api/admin/usuarios. El login y los permisos existentes no cambian.

Una cédula inválida enviada a esas rutas produce HTTP 400:

```json
{
  "codigo": "VALIDACION",
  "mensaje": "Revisa los campos de la solicitud.",
  "fecha": "2026-11-20T21:00:00Z",
  "detalles": {
    "cedula": "Ingresa una cédula ecuatoriana válida de 10 dígitos."
  }
}
```

La fecha es ilustrativa. Una cédula válida ya registrada sigue devolviendo 409 por duplicado.
Los servicios validan nuevamente el dominio si se invocan sin pasar por HTTP.

## Base local

Se revisaron los datos antes de ajustar la columna: Cliente estaba vacío, por lo que no hubo
valores que corregir ni truncar. Se aplicó
[sql/cedula_cliente_10_digitos.sql](sql/cedula_cliente_10_digitos.sql), conservando los datos.
Evidencia local: target/cedula-migracion.txt. Para otras bases, revisar las longitudes y corregir
registros históricos con datos reales antes de ejecutar la migración; nunca truncar una cédula.

El script de creación inicial de Cliente también declara VARCHAR(10). No se cambian las
relaciones Usuario–Cliente, los roles, las contraseñas ni las reservas.

## Preparación del frontend

En la siguiente solicitud, actualizar el registro de cuentas y los formularios ADMIN de Cliente:

- Mantener cedula como string; usar campo de texto con inputMode="numeric", máximo 10 caracteres
  y ayuda "Cédula ecuatoriana de 10 dígitos". No convertirla a Number ni eliminar ceros iniciales.
- Replicar las reglas anteriores para respuesta inmediata y seguir mostrando los errores del backend.
- Mostrar `detalles.cedula` junto al campo cuando se reciba 400, y tratar 409 como conflicto de datos.
- Incluir los cinco campos de perfil y las nuevas acciones ADMIN documentadas en CRUD_ADMIN_CLIENTE.md.

El frontend no se modificó durante este ajuste. La memoria vigente está en
[ESTADO_BACKEND.md](../ESTADO_BACKEND.md).

## Resultado de las pruebas

303 pruebas aprobadas, cero fallos, errores u omisiones; 81 con MySQL, incluidas 16 de concurrencia.
Se verificaron el ejemplo del usuario, ceros iniciales, verificador cero, series consulares,
formatos inválidos, códigos no admitidos, tercer dígito incorrecto, rechazo de cada verificador
alternativo, conservación del objeto al fallar, errores HTTP por campo y validación directa JPA.
También pasaron las pruebas previas de CRUD, registro atómico, reservas y permisos.

El JAR se generó con BUILD SUCCESS. Log: target/cedula-final.log; informes: target/surefire-reports.
