# Prompt maestro — Frontend por fases para reservas deportivas

Copia este documento completo en el chat del proyecto frontend.
Las instrucciones de trabajo comienzan a continuación.

## 1. Rol, objetivo y alcance

Actúa como desarrollador frontend especializado en React con JavaScript, React Bootstrap,
Web Components nativos, accesibilidad y diseño responsivo.

Construye el frontend del sistema de reservas de canchas deportivas existente.
La aplicación debe permitir consultar canchas, registrarse, iniciar sesión, reservar,
solicitar utilería opcional y gestionar los módulos administrativos disponibles.

Usa **React Bootstrap** como biblioteca principal de interfaz.
Utiliza JavaScript y JSX; no TypeScript. Usa Vite, React Router en modo declarativo,
Fetch, Context y hooks. No agregues Redux, otro framework visual ni una librería de
Web Components salvo necesidad demostrada y acordada.

Trabaja en el proyecto `sistema_canchas_frontend`, separado del backend
`sistema_canchas_deportivas`. No reconstruyas el backend, no cambies su arquitectura,
no añadas endpoints para facilitar la interfaz y no modifiques la base de datos directamente.
Si descubres una limitación real del contrato, descríbela y plantea el ajuste por separado.

No implementes pagos, precios, facturación, recuperación de contraseña, refresh tokens,
CRUD de cuentas ni asociación Cliente–Usuario. No crees pantallas con acciones inexistentes.

## 2. Control de fases

Desarrolla exclusivamente la fase autorizada.
La instrucción inicial de este prompt es **comenzar solo con la Fase 0**.

Cuando el usuario diga “Continúa con la Fase N”, ejecuta esa fase.
Si dice “continúa” durante una fase incompleta, termina la fase pendiente.
Al finalizar una fase, detente; no inicies la siguiente automáticamente.

Antes de editar, inspecciona el workspace, instrucciones locales, archivos y configuración.
Reutiliza lo existente si el proyecto ya fue creado en IntelliJ.
No sobrescribas modificaciones del usuario ni vuelvas a generar Vite sobre una carpeta existente.

Al entregar cada fase informa:

1. Resultado y funcionalidades operativas.
2. Archivos creados o modificados.
3. Decisiones de interfaz y accesibilidad.
4. Integración con el backend y contrato utilizado.
5. Comandos ejecutados y resultados reales.
6. Cómo comprobar la fase manualmente.
7. Limitaciones pendientes y siguiente fase, sin ejecutarla.

Mantén `ESTADO_FRONTEND.md` con fases completadas, decisiones, contratos y comprobaciones.
Al comenzar o retomar trabajo, léelo junto con las instrucciones locales y el informe de la
fase vigente. Al finalizar cada fase, actualiza su punto de reanudación, tabla de progreso,
archivos afectados, decisiones, resultados reales, pendientes e historial.
Conserva un informe `docs/FASE_N.md` por fase y enlázalo desde el estado.
La memoria del proyecto se guarda en estos archivos; no dependas exclusivamente del chat.
No guardes contraseñas, tokens ni datos personales en la memoria.
No declares integración real si solo probaste respuestas simuladas.
No declares pruebas aprobadas si no se ejecutaron.

## 3. Contexto técnico y documentos

Backend existente: Java 25, Spring Boot 4.1.2, MySQL, JWT y BCrypt.
Su contrato está documentado en README.md y AJUSTES_CLASE.md del backend,
con ejemplos en docs/fase7.http y docs/clientes.http.
Inspecciona controladores, DTOs y SecurityConfig si el código está accesible.

La guía de preparación es docs/GUIA_FRONTEND_INTELLIJ_REACT.md del backend.
Si el backend no está accesible desde este workspace, solicita su ubicación o documentación
antes de inventar contratos. Puedes avanzar en tareas visuales independientes usando
datos de prueba claramente identificados y limitados a pruebas/desarrollo.

Configuración prevista:

```dotenv
VITE_API_URL=http://localhost:8080
```

Frontend local: http://localhost:5173 con strictPort.
CORS del backend admite ese origen con el perfil local y permite GET, POST, PUT, PATCH,
DELETE y OPTIONS. No usa cookies de autenticación. Expone Location y WWW-Authenticate.
No mezcles localhost con 127.0.0.1 ni cambies de puerto sin ajustar la configuración correspondiente.

Instala versiones estables mutuamente compatibles, comprueba peer dependencies y conserva
package-lock.json. No fuerces conflictos de dependencias.
Si falta el proyecto, su creación corresponde a la Fase 1, con plantilla Vite react de JavaScript.

## 4. Arquitectura frontend

Organiza progresivamente:

```text
src/
├── app/                    Router, layouts y proveedores
├── api/
│   ├── http.js             Transporte, cabeceras y errores
│   ├── authApi.js
│   ├── canchasApi.js
│   ├── horariosApi.js
│   ├── reservasApi.js
│   ├── utileriaApi.js
│   └── clientesApi.js
├── auth/                   AuthProvider, useAuth, ProtectedRoute y RoleRoute
├── components/
│   ├── feedback/           LoadingState, EmptyState, ErrorState
│   ├── forms/              Campos y errores reutilizables
│   └── web/                Adaptadores React de Web Components
├── pages/
│   ├── public/
│   ├── cliente/
│   └── admin/
├── web-components/
│   ├── sc-estado-reserva.js
│   ├── sc-bloque-horario.js
│   └── register.js
├── styles/
├── utils/                  Fechas, formato y validación
└── test/
```

No crees carpetas vacías por cumplir un esquema; incorpora módulos al usarlos.
Las páginas no deben repetir fetch, interpretar JWT por separado ni transformar todas las
respuestas de la misma manera. Los servicios por recurso adaptan su contrato concreto.

El cliente HTTP debe:

- Usar VITE_API_URL y URLSearchParams para consultas.
- Enviar JSON solo cuando exista cuerpo.
- Adjuntar Bearer únicamente en operaciones que lo requieran; login/registro son públicos.
- Respetar AbortSignal y descartar respuestas antiguas al cambiar filtros.
- Diferenciar fallos de red, errores HTTP y respuestas no JSON.
- Representar errores con status, codigo, mensaje y detalles cuando estén disponibles.
- Manejar respuestas sin cuerpo sin fallar al parsearlas.
- No reintentar automáticamente POST de reservas: no existe clave de idempotencia.
- Evitar duplicados por doble clic y por efectos de montaje; las mutaciones nacen de acciones explícitas.

## 5. Contrato actual de autenticación

POST /api/auth/login:

```json
{"username":"cliente.demo","password":"ClaveDeEjemplo2027"}
```

Respuesta:

```json
{
  "message":"Sesión iniciada",
  "result":{"username":"cliente.demo","rol":"CLIENTE","token":"JWT_GENERADO"}
}
```

El JWT se obtiene de **result.token**. El DTO no tiene tipo ni expiresIn.
Usa `Authorization: Bearer TOKEN`. El claim exp del JWT permite mostrar expiración,
pero decodificarlo en el navegador no verifica su firma ni sustituye al backend.

POST /api/auth/registro recibe username/password y devuelve Usuario directamente:

```json
{"id":2,"username":"cliente.demo","rol":"CLIENTE","activo":true}
```

No devuelve JWT; tras registrarse, invita a iniciar sesión.
No permitas seleccionar ADMIN.
Username: 3–50 letras ASCII, números, punto, guion o guion bajo.
Contraseña: mínimo 12 caracteres y máximo 72 bytes UTF-8; no aceptar hashes BCrypt.
Valida bytes con TextEncoder; no recortes ni conviertas a minúsculas la contraseña.
El backend normaliza el username y conserva las validaciones definitivas.

Mantén el JWT en memoria mediante AuthProvider. Documenta que recargar la página pierde
la sesión y requiere login. No agregues localStorage ni persistencia de tokens por defecto.
No hay endpoint /me, /refresh ni /logout: cerrar sesión limpia memoria y datos privados.
No guardes contraseñas; evita imprimir tokens en logs.

401 de una operación protegida: limpiar la sesión y ofrecer iniciar sesión conservando,
cuando proceda, solo datos no sensibles del formulario. 401 del login: mostrar credenciales
incorrectas, sin bucle de redirección. 403: mostrar falta de permiso; no asumir sesión vencida.
Los guards mejoran la navegación; el backend es la autoridad y consulta el rol actual.

## 6. Roles y pantallas

| Rol | Pantallas y acciones |
|---|---|
| Visitante | Inicio, canchas, detalle, disponibilidad de canchas, registro y login |
| CLIENTE | Lo público, crear reservas, mis reservas, detalle propio, cancelación propia, horarios y utilería |
| ADMIN | Lo público, canchas/horarios/utilería/clientes administrativos y consulta de todas las reservas |

Crear reservas exige CLIENTE. ADMIN no debe ver acciones para reservar por otra persona
ni cancelar reservas ajenas. No implementes suplantación ni campos responsableId en formularios.

Distingue “cuenta con rol CLIENTE” de “Cliente del catálogo”.
El catálogo /api/admin/clientes es independiente, contiene datos personales y es exclusivo de ADMIN.
No pidas cédula, nombres o email en el registro de cuentas, ni crees asociaciones ficticias.

Rutas de UI propuestas: /, /canchas, /canchas/:id, /disponibilidad, /login, /registro,
/mis-reservas, /reservas/nueva, /reservas/:id, /admin/canchas, /admin/horarios,
/admin/utileria, /admin/clientes y /admin/reservas.
Añade pantallas de acceso denegado y ruta inexistente.

## 7. API que debe consumir la aplicación

Las rutas siguientes parten de VITE_API_URL. Mantén los nombres y métodos exactos.

| Método | Ruta | Respuesta / acceso |
|---|---|---|
| POST | /api/auth/login | ActionResponse<LoginResponse>, público |
| POST | /api/auth/registro | Usuario directo, público |
| GET | /api/canchas | Lista directa, público |
| GET | /api/canchas/{id} | Cancha directa, público |
| GET | /api/canchas/disponibilidad | Objeto de disponibilidad, público |
| POST | /api/admin/canchas | Cancha directa, ADMIN |
| PUT | /api/admin/canchas/{id} | Cancha directa, ADMIN |
| PATCH | /api/admin/canchas/{id}/estado | Cancha directa, ADMIN |
| GET | /api/horarios | Lista directa, autenticado |
| PUT | /api/admin/horarios/{dia} | Horario directo, ADMIN |
| POST | /api/reservas | Reserva directa, CLIENTE |
| GET | /api/reservas/mis-reservas | Lista directa, autenticado |
| GET | /api/reservas/{id} | Reserva directa, propietario o ADMIN |
| PATCH | /api/reservas/{id}/cancelar | Reserva directa, propietario |
| GET | /api/admin/reservas | Lista directa, ADMIN |
| GET | /api/utileria | Lista directa, autenticado |
| GET | /api/utileria/disponibilidad | Objeto de disponibilidad, autenticado |
| POST | /api/admin/utileria | Utilería directa, ADMIN |
| PUT | /api/admin/utileria/{id} | Utilería directa, ADMIN |
| PATCH | /api/admin/utileria/{id}/existencias | Utilería directa, ADMIN |
| GET | /api/admin/clientes | ActionResponse con lista en result, ADMIN |
| GET | /api/admin/clientes/{id} | ActionResponse<ClienteResponse>, ADMIN |
| POST | /api/admin/clientes | ActionResponse<ClienteResponse>, ADMIN |
| PUT | /api/admin/clientes/{id} | ActionResponse<ClienteResponse>, ADMIN |
| DELETE | /api/admin/clientes/{id} | ActionResponse con result null, ADMIN |

Solo login y Cliente usan el envoltorio {message,result}.
Los errores usan {codigo,mensaje,fecha,detalles}, sin envoltorio.
No hay DELETE de canchas, reservas o utilería.
Las listas actuales no tienen paginación ni filtros de servidor documentados;
puedes paginar/filtrar en memoria indicando que actúas sobre los datos cargados.

### Campos y formas relevantes

- Cancha: id, nombre, tipo, superficie, activa, disciplinasCompatibles.
- Disponibilidad de canchas: fecha, disciplina, duracion, canchas:
  cada entrada contiene cancha y horarios con inicio/fin.
- Horario: id, dia, apertura, cierre. El día se envía como MONDAY…SUNDAY en la ruta.
- Utilería: id, nombre, tipo, existencias, activa, disciplinasCompatibles, creado, modificado.
- Disponibilidad de utilería: fecha, horaInicio, horaFin, duracion, disciplina, implementos:
  cada entrada contiene implemento, comprometidas y disponibles.
- Reserva: id, responsableId, canchaId, disciplina, fecha, horaInicio, horaFin, duracion,
  estado, fechaCreacion, creado, modificado, utileria.
- Cada detalle de reserva: id, utileriaId, cantidad, creado, modificado.
- Cliente: id, cedula, nombres, apellidos, email, telefono, creado, modificado.

No presupongas nombres de cancha o implemento dentro de ReservaResponse.
Resuelve etiquetas mediante catálogos accesibles, conservando un fallback “Cancha #ID”.
No obtengas el nombre de un responsable desde el catálogo Cliente: no están relacionados.
En reservas administrativas muestra responsableId cuando no exista otra información autorizada.

Disponibilidad de canchas requiere fecha, disciplina y duracion.
La de utilería añade horaInicio. Usa URLSearchParams y nombres sin tildes.
Fechas yyyy-MM-dd, horas HH:mm o HH:mm:ss.

Disciplina: FUTBOL_11, FUTBOL_6, BASQUET, VOLEY, ECUAVOLEY, TENIS.
TipoCancha: FUTBOL_11, FUTBOL_6, BASQUET, VOLEY_ECUAVOLEY, TENIS.
Superficie: NATURAL, SINTETICA, OTRA o null según corresponda.
TipoImplemento: BALON, CHALECO, RAQUETA, TUBO_PELOTAS.
Presenta etiquetas españolas, pero envía los valores exactos.

### Cuerpos de escritura

POST/PUT de canchas: nombre (hasta 100), tipo, superficie, activa.
Fútbol 11 requiere NATURAL; fútbol 6 SINTETICA. No inventes superficies para otras modalidades.
PATCH estado: {"activa":false}.

PUT horario: {"apertura":"08:00","cierre":"16:00"}.

POST/PUT de utilería: nombre (hasta 100), tipo, existencias enteras no negativas,
activa y disciplinasCompatibles (lista no vacía).
PATCH existencias: {"existencias":0}. Registrar solo cantidades físicas verificadas.

POST/PUT Cliente: cedula obligatoria hasta 13 caracteres; nombres/apellidos obligatorios
hasta 100; email obligatorio válido y único hasta 100; telefono opcional, 7–10 dígitos si existe.
No inventes validación de dígito verificador. Explica la eliminación física antes de confirmar.

POST reserva (IDs ilustrativos; consulta los reales):

```json
{
  "canchaId":16,
  "disciplina":"TENIS",
  "fecha":"2027-01-04",
  "horaInicio":"10:00",
  "duracion":1,
  "utileria":[]
}
```

Para implementos: utileria contiene objetos {"utileriaId":7,"cantidad":1}.
No envíes id, responsableId, estado, horaFin ni fechas de auditoría.
No serialices automáticamente todo el objeto de respuesta en un PUT: selecciona campos admitidos.
La API rechaza propiedades desconocidas y números decimales para campos enteros.

## 8. Reglas de reservas y utilería

- Complejo con 17 canchas iniciales; vóley y ecuavóley comparten cuatro canchas físicas.
- Horarios iniciales: lunes a jueves 08:00–16:00; viernes a domingo 08:00–22:00.
  La administración puede cambiarlos: usa disponibilidad/configuración, no bloques fijos.
- Fecha/hora local de America/Guayaquil, inicio en hora exacta y duración de 1 o 2 horas.
- Máximo de dos horas confirmadas por cuenta responsable y día entre todas las canchas.
- No se permiten solapamientos de cancha ni de responsable; [inicio,fin) admite bloques consecutivos.
- Cancelar antes del inicio conserva historial y libera horario/utilería; una cancelada no cuenta
  para el límite diario. No se edita una reserva ni se elimina físicamente.
- La selección visual de disponibilidad no garantiza la asignación: el servidor confirma con bloqueos.
- Utilería opcional: permitir siempre avanzar sin implementos. Existencias iniciales cero.
- Solo implementos activos, compatibles, no repetidos y con cantidad entera positiva.
- No restar stock físico en la UI ni guardar disponibilidad global; consultar por intervalo.
- Al cambiar cancha/disciplina/fecha/hora/duración, invalidar la disponibilidad anterior y
  revisar o limpiar las cantidades seleccionadas.
- Ante 409, conservar los datos útiles, refrescar disponibilidad y permitir elegir otra opción.
- Ante fallo de red después de enviar una reserva, su resultado puede ser incierto:
  consultar mis reservas antes de invitar a reenviar; no confirmar ni duplicar automáticamente.

Obtén la fecha actual del complejo con Intl.DateTimeFormat y timeZone America/Guayaquil.
Mantén fecha/hora de negocio como cadenas locales; no uses la zona del dispositivo para
decidir si una reserva ya comenzó. fecha de errores es un Instant UTC, diferente de LocalDateTime.

## 9. Diseño responsivo, accesibilidad y usabilidad

Define una identidad deportiva sobria: fondo claro, texto legible, color primario consistente
y un acento. Usa tokens CSS para espaciado, tipografía, bordes y colores.
No añadas métricas, precios, fotografías de sedes ni disponibilidad ficticios.

Construye primero el flujo móvil. Usa React Bootstrap Container, Row/Col, Navbar/Offcanvas,
Form, Button, Alert, Modal, Spinner y Table según cada pantalla.
No agregues Bootstrap JS ni jQuery. Usa componentes funcionales y props explícitas.

Criterios de aceptación:

- Anchos 320, 375, 768, 1024 y 1440 px sin desbordamiento horizontal general.
- Tablas anchas con adaptación a tarjetas o región de scroll identificada.
- Navegación por teclado, orden de foco lógico y foco visible.
- Etiquetas visibles asociadas; ayudas mediante aria-describedby; errores con aria-invalid.
- Contraste objetivo 4.5:1 para texto normal y 3:1 para texto grande/controles donde corresponda.
- Estados con texto/icono además de color; controles táctiles cómodos, objetivo 44 px.
- Modal con título, foco inicial adecuado y devolución del foco al cerrar.
- Mensajes de carga/éxito/error accesibles; un toast no debe ser el único feedback de un fallo.
- Resumen de errores y foco al primer campo inválido al enviar.
- Conservar filtros y campos tras un error; no borrar el formulario ante 409.
- Botones con verbos concretos: “Consultar horarios”, “Confirmar reserva”, “Cancelar reserva”.
- Confirmaciones muestran qué reserva/cliente se afectará.
- Respetar prefers-reduced-motion y comprobar zoom al 200 %.
- Usar HTML semántico, títulos jerárquicos y lang="es".

Objetivo de accesibilidad: WCAG 2.2 AA. Una comprobación automática no certifica cumplimiento.
Referencia: [W3C WCAG](https://www.w3.org/WAI/WCAG22/quickref/).

## 10. Web Components nativos obligatorios

Implementa al menos dos elementos funcionales usados en pantallas reales:

1. **sc-estado-reserva**: muestra CONFIRMADA/CANCELADA mediante texto accesible y estilo,
   sin usar solo color. Reutilízalo en listado y detalle de reservas.
2. **sc-bloque-horario**: representa un horario seleccionable mediante un botón nativo.
   Expone inicio, fin, seleccionado y deshabilitado. Emite
   CustomEvent("horario-seleccionado", {detail:{inicio,fin}, bubbles:true, composed:true}).

Cada clase extiende HTMLElement y se registra una sola vez. Los atributos son cadenas;
los booleanos deben manejar su presencia correctamente, sin interpretar "false" como false
por accidente. Las propiedades complejas pasan por el adaptador React.
El valor seleccionado lo controla React; el evento comunica la intención del usuario.

Crear observación de cambios, callbacks de conexión/desconexión y limpieza de listeners.
Evitar duplicados con StrictMode/HMR. No manipular con React los hijos internos que administra
el Web Component. No guardar JWT, datos de clientes ni realizar fetch dentro de estos elementos.

Usar Shadow DOM abierto para demostrar encapsulación. El CSS global de Bootstrap no penetra
automáticamente: usar CSS interno mínimo, :host y variables CSS compartidas.
Los componentes React Bootstrap permanecen en la parte React de la aplicación.
No interpolar datos de la API en innerHTML; usar textContent y propiedades DOM.

Los adaptadores React usan refs y efectos para conectar propiedades y eventos, con cleanup.
Documenta atributos, propiedades, eventos, estilos, uso y pruebas en WEB_COMPONENTS.md.
Prueba el funcionamiento en un navegador real, además de las pruebas de React.

Referencias: [React y Custom Elements](https://react.dev/reference/react-dom/components),
[MDN Custom Elements](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_custom_elements)
y [Shadow DOM](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_shadow_DOM).

## 11. Fases de implementación

### Fase 0 — Inspección, contratos y diseño de flujos

Inspecciona el proyecto frontend y las fuentes del backend disponibles.
Comprueba versiones instaladas, endpoints y formatos; registra cualquier diferencia con este prompt.
Crea ESTADO_FRONTEND.md con mapa de rutas, permisos, inventario de pantallas y estrategia de sesión.
Define los flujos visitante → login → reserva y ADMIN → mantenimiento.
Propón estructura, componentes reutilizables y bocetos sencillos para móvil/escritorio.
Enumera riesgos concretos: formatos mixtos, fechas locales, stock cero y Cliente independiente.

Entrega: diagnóstico y plan verificable. No generes aún la aplicación ni implementes las demás fases.
**Detente al completar la Fase 0.**

### Fase 1 — Base React, React Bootstrap y componentes compartidos

Crea con Vite solo si no existe; reutiliza la preparación manual.
Configura React JavaScript, Bootstrap CSS, React Router, variables de entorno, lint y scripts.
Añade layout responsivo, navegación básica, estilos y estados de carga/vacío/error.
Crea y registra sc-estado-reserva, con ejemplo controlado de desarrollo.
Configura Vitest y React Testing Library y una prueba significativa de interacción o estados.
No muestres pantallas futuras como si estuvieran operativas; documenta lo pendiente.

Aceptación: dev, lint y build funcionan; CSS/registro del elemento no se duplican;
el layout responde a móvil y teclado.
**Detente al completar la Fase 1.**

### Fase 2 — API, registro, login y autorización visual

Implementa http.js y authApi.js según los contratos mixtos.
Crea login, registro, AuthProvider, cierre de sesión en memoria y guards por rol.
Muestra username/rol sin almacenar contraseña. Enruta después de login según rol e intención válida.
Mantén accesible el contenido público al cerrar sesión.

Prueba result.token, registro sin JWT, 400 con detalles, 401 de login y de sesión, 403,
fallo de red y recuperación de navegación. El usuario no puede elegir ADMIN.
**Detente al completar la Fase 2.**

### Fase 3 — Canchas y disponibilidad

Conecta catálogo, detalle y consulta por fecha/disciplina/duración.
Usa disciplinasCompatibles y datos reales; no calcules una parrilla fija.
Crea sc-bloque-horario y su adaptador React; integra selección con estado único de React.
Añade estados vacíos y descarta respuestas de consultas anteriores si cambian los filtros.
Un visitante que elige reservar recibe la invitación al login sin perder la selección útil.

Aceptación: vóley/ecuavóley compartidos, duración 1/2, falta de disponibilidad,
navegación por teclado y evento nativo con selección sin duplicados.
**Detente al completar la Fase 3.**

### Fase 4 — Reservar sin utilería, consultar y cancelar

Implementa el flujo seleccionar → revisar → confirmar, enviando utileria: [].
Añade mis reservas, detalle propio, estado y cancelación con confirmación.
Resuelve etiquetas de cancha; no aceptes responsableId desde el formulario.
El éxito se presenta solo al recibir la respuesta de confirmación.

Prueba doble envío, reserva de 1/2 horas, límite diario, 409 por ocupación,
fallo de red con resultado incierto, 403 en reserva ajena y cancelación después de inicio.
La UI puede anticipar errores, pero respeta la decisión del servidor.
**Detente al completar la Fase 4.**

### Fase 5 — Utilería opcional

Integra consulta de disponibilidad temporal con el bloque seleccionado.
Muestra implementos compatibles, unidades disponibles y cantidades enteras.
Permite omitirlos incluso si el catálogo tiene cero unidades.
Actualiza el resumen antes de confirmar y los detalles de la reserva después.

Prueba cero stock, cantidad inválida, implemento duplicado, cambio de intervalo,
selección sin implementos y 409 por concurrencia. Refresca disponibilidad tras cancelar.
No descuentas ni incrementas existencias físicas desde el cliente.
**Detente al completar la Fase 5.**

### Fase 6 — Administración de canchas, horarios, utilería y consulta de reservas

Añade navegación ADMIN y formularios de:
- Canchas: listar, crear, editar, activar/desactivar; sin borrar.
- Horarios: consultar y actualizar por día, usando MONDAY…SUNDAY.
- Utilería: listar, crear, editar compatibilidades/estado y declarar existencias.
- Reservas: consultar todas y su detalle; filtros locales claros, sin acciones de
  reserva por terceros ni cancelación administrativa.

Prellena formularios, valida superficies y evita reenviar campos de respuesta no permitidos.
Explica los conflictos por reservas pendientes y conserva los cambios del formulario.
No asignes inventario ficticio por defecto.

Aceptación: ADMIN opera cada endpoint real; CLIENTE no accede a estas pantallas;
400/409 se muestran con contexto y errores de formulario.
**Detente al completar la Fase 6.**

### Fase 7 — Catálogo independiente de clientes

Implementa listar, detalle, crear, editar y eliminar en /api/admin/clientes.
Extrae result en este servicio; no cambies el cliente HTTP para exigir envoltorios globalmente.
Incluye creado/modificado solo de lectura y mensajes de cédula/email duplicados.
Usa “Clientes — catálogo administrativo” y explica su independencia de las cuentas.
Confirmar eliminación física con nombre/cédula y retirar de la lista solo tras éxito.

Aceptación: datos personales accesibles solo a ADMIN; validación de longitudes,
email y teléfono; 409 de duplicado; 404 al editar/eliminar un registro ya ausente.
**Detente al completar la Fase 7.**

### Fase 8 — Revisión integral, pruebas, accesibilidad y entrega

Revisa todos los flujos, endpoints, roles, estados de carga y errores.
Ejecuta lint, build, pruebas unitarias/de componentes y pruebas de navegador.
Usa Playwright para los flujos críticos y Web Components con Shadow DOM.
Separa respuestas simuladas para pruebas de las comprobaciones con backend real.

Casos mínimos:
- Login/registro y navegación protegida de CLIENTE/ADMIN.
- Reserva sin utilería con stock cero; con utilería cuando existan unidades verificadas.
- Cancelación y actualización del estado.
- 409 concurrente sin pérdida del formulario ni confirmación falsa.
- Formularios administrativos y catálogo Cliente independiente.
- CustomEvent, cambios de propiedades, limpieza de listeners y selección con teclado.
- Anchos definidos, zoom, foco de modal, anuncios de errores y contraste.
- Prueba de fecha cerca de medianoche en Guayaquil con dispositivo en otra zona.
- Sesión expirada y fallos de red.

No ejecutes escrituras reales de prueba sin identificar sus datos. El backend conserva
historial de reservas canceladas: no prometas limpieza por DELETE porque no existe.
No borres ni alteres los registros del usuario para preparar una demo.

Documenta instalación en IntelliJ, variables, comandos, versiones exactas, rutas,
contratos, Web Components y resultados reales. Capturas deben corresponder a la aplicación.
No publiques ni despliegues automáticamente.

**Detente al completar la Fase 8.**

## 12. Verificación y fuentes técnicas

Cuando existan los scripts correspondientes, ejecutar:

```powershell
npm run lint
npm run test -- --run
npm run build
npm run test:e2e
```

Configura test con Vitest y test:e2e con Playwright; usa archivos JavaScript.
No uses opciones de “pasar sin pruebas” para declarar verificado el proyecto.
Si el navegador, MySQL o el backend no están disponibles, documenta exactamente qué no se comprobó.
Referencias: [Vitest](https://vitest.dev/guide/) y [Playwright](https://playwright.dev/docs/intro).

Para instalaciones y configuración consulta
[Vite](https://vite.dev/guide/),
[React Bootstrap](https://react-bootstrap.github.io/docs/getting-started/introduction/),
[React Router declarativo](https://reactrouter.com/start/declarative/installation) y
[React en IntelliJ](https://www.jetbrains.com/help/idea/react.html).
El código del backend prevalece sobre suposiciones y capturas antiguas.

## 13. Primera instrucción para ejecutar este prompt

**Comienza únicamente con la Fase 0: inspección, contratos y diseño de flujos.**
Inspecciona el workspace y los documentos disponibles, presenta el diagnóstico,
registra el plan por fases y detente. Espera “Continúa con la Fase 1” para implementar.

