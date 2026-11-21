package org.example.sistema_canchas_deportivas.application.services;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import org.example.sistema_canchas_deportivas.infraestructure.adapters.PasswordAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ClienteServiceTest {
    private final ClienteOut out = mock(ClienteOut.class);
    private final ClienteService service = new ClienteService(out, mock(org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut.class));
    private Cliente datos() { return Cliente.nuevo("1750397802", "Ana", "Pérez", "ana@example.test", "0991234567"); }

    @Test void creaConDatosNormalizadosSinAceptarIdNiAuditoriaDelLlamador() {
        var fecha = LocalDateTime.of(2000, 1, 1, 0, 0);
        var entrada = new Cliente(999L, "1750397802", " Ana ", " Pérez ", "ANA@example.test", "", fecha, fecha);
        when(out.guardar(any())).thenAnswer(i -> i.getArgument(0));
        var creado = service.crear(entrada);
        assertNull(creado.getId());
        assertNull(creado.getCreado());
        assertEquals("ana@example.test", creado.getEmail());
        assertEquals("Ana", creado.getNombres());
        assertNull(creado.getTelefono());
        verify(out).existePorCedula("1750397802", null);
    }

    @Test void noGuardaCedulaNiEmailDuplicados() {
        when(out.existePorCedula("1750397802", null)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.crear(datos()));
        verify(out, never()).guardar(any());
        reset(out);
        when(out.existePorEmail("ana@example.test", null)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.crear(datos()));
        verify(out, never()).guardar(any());
    }

    @Test void actualizarUsaElObjetoExistenteYConservaAuditoria() {
        var fecha = LocalDateTime.of(2026, 1, 1, 0, 0);
        var actual = new Cliente(7L, "1750397802", "Anterior", "Pérez", "ana@example.test", null, fecha, fecha);
        when(out.buscarPorId(7L)).thenReturn(Optional.of(actual));
        when(out.guardar(any())).thenAnswer(i -> i.getArgument(0));
        var actualizado = service.actualizar(7L, datos());
        assertSame(actual, actualizado);
        assertEquals(fecha, actualizado.getCreado());
        assertEquals("Ana", actualizado.getNombres());
        verify(out).existePorEmail("ana@example.test", 7L);
        verify(out).existePorCedula("1750397802", 7L);
    }

    @Test void idsInvalidosYAusentesNoEliminanRegistros() {
        assertThrows(DatoInvalidoException.class, () -> service.buscarPorId(0L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(8L));
        verify(out, never()).eliminar(any());
        when(out.buscarPorId(7L)).thenReturn(Optional.of(datos()));
        when(out.bloquearPorId(7L)).thenReturn(Optional.of(datos()));
        service.eliminar(7L);
        verify(out).eliminar(7L);
    }

    @Test void dominioRechazaCamposInvalidosSinMutarElObjeto() {
        var cliente = datos();
        assertThrows(DatoInvalidoException.class, () -> cliente.actualizar("1".repeat(14), "Ana", "Pérez", "ana@example.test", null));
        assertThrows(DatoInvalidoException.class, () -> cliente.actualizar("1750397802", "", "Pérez", "ana@example.test", null));
        assertThrows(DatoInvalidoException.class, () -> cliente.actualizar("1750397802", "Ana", "Pérez", "correo-invalido", null));
        assertThrows(DatoInvalidoException.class, () -> cliente.actualizar("1750397802", "Ana", "Pérez", "ana@example.test", "abc"));
        assertEquals("1750397802", cliente.getCedula());
        assertThrows(DatoInvalidoException.class, () -> service.crear(null));
        verifyNoInteractions(out);
    }

    @Test void passwordOutEncriptaYComparaConBcryptComoEnClase() {
        var password = new PasswordAdapter(new BCryptPasswordEncoder());
        String hash = password.encriptar("ClaveSoloPruebas2027");
        assertNotEquals("ClaveSoloPruebas2027", hash);
        assertTrue(password.coincide("ClaveSoloPruebas2027", hash));
        assertFalse(password.coincide("OtraClave2027", hash));
    }
}
