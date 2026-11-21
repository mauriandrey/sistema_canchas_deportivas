# Crear el frontend en IntelliJ IDEA: React con JavaScript

Guía para el proyecto `sistema_canchas_frontend`. Preparada el 16 de noviembre de 2026.
El backend ya existe; estos pasos crean un proyecto frontend separado.
Esta entrega es documentación: todavía no se han ejecutado estos comandos ni creado la aplicación.

## 1. Tecnologías y resultado esperado

- React con JavaScript y JSX, mediante Vite. No seleccionar TypeScript.
- React Bootstrap para formularios, navegación, rejillas, tablas, modales y avisos.
- React Router en modo declarativo para las pantallas.
- Fetch para la API; Context y hooks para la sesión.
- Web Components nativos para elementos reutilizables concretos, integrados en React.
- CSS propio para identidad visual, espaciado y estados accesibles.

Un componente React y un Web Component nativo son distintos. La aplicación combinará ambos;
no basta con llamar “Web Components” a los componentes JSX.

## 2. Requisitos

Instala una versión LTS mantenida de [Node.js](https://nodejs.org/en/download) compatible con Vite.
La guía oficial de [Vite](https://vite.dev/guide/) indica Node.js 20.19+ o 22.12+ como mínimo;
elige una rama LTS que siga mantenida y respeta requisitos adicionales de la plantilla.

Comprueba en la terminal de IntelliJ:

```powershell
node --version
npm --version
```

Necesitas también MySQL y el backend funcionando en `http://localhost:8080`.
Las versiones del frontend se fijarán en `package-lock.json` cuando se instalen.

En IntelliJ revisa **Settings → Plugins → Installed** y habilita los plugins de JavaScript,
React y Vite disponibles en tu instalación. La documentación de
[React en IntelliJ IDEA](https://www.jetbrains.com/help/idea/react.html) describe estas opciones.
Los nombres y prestaciones pueden variar con la versión/licencia; si no aparece el generador,
usa la alternativa de terminal indicada más abajo.

## 3. Ubicación de los dos proyectos

Estructura sugerida:

```text
PROYECTO-GRUPAL/
├── sistema_canchas_deportivas/    Backend Java existente
└── sistema_canchas_frontend/      Nuevo frontend React
```

No generes Vite dentro de `src/main/java`, ni sustituyas archivos del backend.
Si la carpeta frontend ya existe, inspecciónala antes de generar nada.
Puedes abrir cada proyecto en una ventana de IntelliJ.

## 4. Crear con el asistente de IntelliJ

1. Selecciona **File → New → Project → Vite**.
2. Nombre: `sistema_canchas_frontend`.
3. Ubicación: la carpeta hermana del backend indicada arriba.
4. Selecciona el runtime Node.js instalado.
5. Elige React y la variante JavaScript. Desmarca TypeScript si aparece esa casilla.
6. Usa el generador `create-vite` y termina la creación.
7. Comprueba que existan `package.json`, `vite.config.js`, `src/main.jsx` y `src/App.jsx`.

La integración y el generador están descritos en
[Vite en IntelliJ IDEA](https://www.jetbrains.com/help/idea/vite.html).
No crees un segundo proyecto si el asistente ya generó correctamente el primero.

### Alternativa mediante terminal

Desde la carpeta que contiene el backend, ejecuta:

```powershell
Set-Location 'D:\UNIVERSIDAD\SEPTIMO SEMESTRE\PROGRAMACION AVANZADA\SEGUNDO PARCIAL\PROYECTO-GRUPAL'
npm create vite@latest sistema_canchas_frontend -- --template react
Set-Location sistema_canchas_frontend
npm install
```

Después utiliza **File → Open** y abre la carpeta creada.
La opción `--template react` selecciona JavaScript; no uses `react-ts`.
El procedimiento de creación procede de la [guía de Vite](https://vite.dev/guide/).

Si PowerShell bloquea `npm.ps1`, utiliza `npm.cmd` en estos comandos.
No necesitas cambiar globalmente la política de ejecución.

## 5. Instalar React Bootstrap y las rutas

En la raíz del frontend:

```powershell
npm install react-bootstrap bootstrap react-router
```

No fuerces dependencias incompatibles con `--force` o `--legacy-peer-deps`.
Revisa las versiones compatibles si npm informa un conflicto.
La instalación de React Bootstrap y su hoja CSS se explica en su
[documentación oficial](https://react-bootstrap.github.io/docs/getting-started/introduction/).

Edita `src/main.jsx`:

```jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);
```

Importa cada componente desde su módulo, por ejemplo `react-bootstrap/Button`.
No cargues el JavaScript de Bootstrap ni jQuery: los comportamientos se gestionan con React Bootstrap.
La configuración declarativa de las rutas utiliza
[BrowserRouter de React Router](https://reactrouter.com/start/declarative/installation).

En `src/App.jsx` puedes comprobar la instalación con:

```jsx
import Container from 'react-bootstrap/Container';
import Alert from 'react-bootstrap/Alert';

export default function App() {
  return (
    <Container as="main" className="py-4">
      <h1>Reservas deportivas</h1>
      <Alert variant="info">
        Interfaz inicial. La conexión con el backend se implementará por fases.
      </Alert>
    </Container>
  );
}
```

Sustituye los estilos de demostración de Vite que centran toda la página o limitan `#root`.
Una base para `src/index.css`:

```css
:root {
  font-family: system-ui, sans-serif;
  color: #172b36;
  background: #f5f7fa;
}
body { margin: 0; min-width: 320px; }
#root { min-height: 100dvh; }
:focus-visible { outline: 3px solid #075f9b; outline-offset: 3px; }
```

En `index.html`, establece `<html lang="es">` y un título descriptivo.

## 6. Configurar el puerto y la API

Conserva la configuración de React generada y añade en `vite.config.js`:

```javascript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    port: 5173,
    strictPort: true
  }
});
```

`strictPort` evita cambiar silenciosamente a un puerto que el backend no admita.
Este ejemplo usa peticiones directas al backend y su configuración CORS, sin proxy de Vite.

Crea `.env.example` y `.env.local` en el frontend:

```dotenv
VITE_API_URL=http://localhost:8080
```

Incluye `.env.local` en `.gitignore` y conserva `.env.example`.
Vite carga sus archivos de entorno; reinicia el servidor de desarrollo al cambiarlos.
Las variables `VITE_*` son públicas en el navegador: no copies JWT_SECRET ni credenciales MySQL.
Consulta [variables de entorno de Vite](https://vite.dev/guide/env-and-mode).

En el backend, el perfil local ya permite `http://localhost:5173` y `http://localhost:3000`.
Abre exactamente `http://localhost:5173`; `127.0.0.1` es otro origen.
El archivo `.env.local` del frontend no configura el proceso Java.

## 7. Ejecutar en IntelliJ

Desde la terminal del frontend:

```powershell
npm run dev
```

También puedes crear una configuración **Run → Edit Configurations → npm**:
elige el `package.json` del frontend, comando `run` y script `dev`.
Mantén el backend ejecutándose en otra terminal o configuración.

Para comprobar la preparación:

```powershell
npm run lint
npm run build
```

Si algún script falta, inspecciona `package.json` y configura el equivalente;
no declares que pasó una comprobación no ejecutada.
Para dependencias ya fijadas en otro equipo utiliza `npm ci`.

`npm run preview` sirve para revisar el build, no es un servidor productivo.
Si lo usas contra la API local, arráncalo en un origen admitido, por ejemplo
`npm run preview -- --port 5173 --strictPort`, después de detener `dev`.

## 8. Estructura prevista

Créala progresivamente, cuando corresponda en cada fase:

```text
src/
├── app/                 App, router, layouts y proveedores
├── api/                 Cliente HTTP y servicios por recurso
├── auth/                Contexto, hooks y protección de rutas
├── components/          Componentes React reutilizables
│   └── web/             Adaptadores React para Custom Elements
├── pages/               Pantallas públicas, del usuario y administrativas
├── web-components/      Elementos nativos y registro
├── styles/              Variables visuales y estilos globales
├── utils/               Fechas locales, validación y formatos
└── test/                Configuración y utilidades de pruebas
```

No traslades la arquitectura de puertos del backend al navegador.
El cliente HTTP conoce el transporte; las pantallas coordinan interacción y presentación.

## 9. Web Components junto con React Bootstrap

La interfaz principal usa React Bootstrap. Para demostrar reutilización nativa se implementarán:

| Elemento | Uso | Contrato |
|---|---|---|
| `sc-estado-reserva` | Estado en tarjetas y detalle | Atributo `estado`: CONFIRMADA o CANCELADA |
| `sc-bloque-horario` | Selección de horario | Inicio/fin y selección; evento `horario-seleccionado` con detail |

Cada elemento extiende `HTMLElement` y se registra con `customElements.define`,
comprobando antes `customElements.get` para evitar registro duplicado durante recarga.
Debe actualizarse cuando cambien atributos/propiedades; no basta con dibujarlo una vez.
Los listeners y recursos se limpian al desconectar.
Véase [Custom Elements](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_custom_elements).

Los adaptadores React reciben props y usan una referencia al elemento para propiedades complejas.
Para eventos, añaden el listener en un efecto y lo retiran en su cleanup.
No pases objetos como atributos de texto ni guardes JWT en el elemento.
React documenta los [atributos, propiedades y eventos de elementos personalizados](https://react.dev/reference/react-dom/components).

Si usas Shadow DOM, el CSS global de Bootstrap no estiliza automáticamente su interior.
Define estilos internos pequeños y variables CSS compartidas; mantén los componentes React Bootstrap
fuera de ese árbol. Los eventos que deban salir usarán `bubbles: true` y `composed: true`.
Consulta [Shadow DOM](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_shadow_DOM).

`sc-bloque-horario` debe contener un botón nativo con texto y estado accesible.
React controla la selección; el evento propone el cambio. Comprueba montaje, desmontaje,
cambio de props y StrictMode sin listeners repetidos. Estos elementos se construyen en las fases 1 y 3.

## 10. Contrato del backend que debe respetarse

- Login: `POST /api/auth/login` devuelve `{message, result: {username, rol, token}}`.
- Registro: `POST /api/auth/registro` devuelve Usuario directamente; no contiene JWT.
- Solo login y CRUD de Cliente usan `ActionResponse`. No extraigas `result` de todas las respuestas.
- Consultas públicas: canchas, detalle de cancha y disponibilidad de canchas.
- Horarios, utilería y reservas requieren JWT; administración exige ADMIN.
- Crear una reserva requiere CLIENTE. ADMIN no reserva ni cancela por otros usuarios.
- `/api/admin/clientes` contiene personas independientes de las cuentas.
- Fechas y horas de reservas son locales de America/Guayaquil. No conviertas `fecha` a UTC
  con `toISOString()` ni combines cadenas locales con la zona del dispositivo.
- Los implementos iniciales tienen stock cero; reservar sin implementos debe funcionar.

En la primera integración comprueba `GET http://localhost:8080/api/canchas`.
No se necesita token para esa consulta.

## 11. Criterios de UI/UX y usabilidad

Diseña primero para móvil: navegación clara, formularios de una columna y acciones visibles.
Usa React Bootstrap Container, Row/Col, Navbar/Offcanvas, Form, Alert, Modal y Table donde proceda.
Prueba anchos de 320, 375, 768, 1024 y 1440 px, teclado y zoom.

Los formularios deben tener etiquetas visibles, ayudas, errores junto a campos y conservar
los datos ante un 400/409. Las acciones deben indicar carga, éxito, vacío, error y reintento.
Cancelar reserva y eliminar Cliente necesitan confirmación específica; deshabilita el doble envío.

Adopta como objetivo WCAG 2.2 AA: foco visible, navegación por teclado, contraste suficiente,
información que no dependa solo del color y mensajes de estado comprensibles.
La revisión manual complementa las herramientas automáticas.
Referencia: [guía rápida WCAG](https://www.w3.org/WAI/WCAG22/quickref/).

## 12. Problemas frecuentes

| Síntoma | Comprobación |
|---|---|
| No aparece Vite en IntelliJ | Plugins, versión instalada o creación por terminal |
| Puerto 5173 ocupado | Detener el proceso propio que lo usa o configurar otro origen explícitamente |
| Error CORS | Usar localhost:5173, perfil local y origen exacto |
| 401 | Token expirado, ausente o inválido; iniciar sesión de nuevo |
| 403 | Rol/ruta/propiedad; no equivale a una contraseña incorrecta |
| 409 al reservar | Refrescar disponibilidad y permitir cambiar selección |
| Utilería con cero disponibles | Mostrarlo; permitir continuar sin implementos |
| Estilos Bootstrap ausentes | Revisar import CSS; distinguir Shadow DOM |
| Cambio de día en la reserva | Revisar conversión incorrecta de fecha local a UTC |
| Login correcto sin sesión en pantalla | Leer result.token y result.rol |
| Recargar una ruta desplegada devuelve 404 | Configurar fallback de la SPA a index.html en el alojamiento |

## 13. Continuación por fases

Abre [PROMPT_FRONTEND_POR_FASES.md](PROMPT_FRONTEND_POR_FASES.md).
Copia el contenido completo en el chat del proyecto frontend, dando acceso al README del backend.
La instrucción inicial de ese documento autoriza únicamente la Fase 0.
Si ya creaste la base con esta guía, la fase de estructura debe reutilizarla.

El alcance actual no incluye ejecutar el prompt ni publicar una web.

