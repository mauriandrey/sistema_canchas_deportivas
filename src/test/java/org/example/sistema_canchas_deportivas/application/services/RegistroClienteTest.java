package org.example.sistema_canchas_deportivas.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RegistroClienteTest {
    private UsuarioOut usuarios;
    private PasswordOut passwords;
    private UsuarioService service;
    private org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn perfiles;
    private Cliente perfil() { return Cliente.nuevo("1750397802", "Ana", "Prueba", "ana@example.test", "0999999999"); }

    @BeforeEach void preparar() {
        usuarios = mock(UsuarioOut.class);
        passwords = mock(PasswordOut.class);
        perfiles = mock(org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn.class);
        when(perfiles.crear(any())).thenReturn(new Cliente(7L, "1750397802", "Ana", "Prueba", "ana@example.test", "0999999999", null, null));
        service = new UsuarioService(usuarios, passwords, perfiles, mock(ClienteOut.class), mock(ReservaOut.class));
    }

    @Test void normalizaUsernameFijaClienteYCodificaUnaSolaVez() {
        when(passwords.encriptar("ClaveDePrueba2026")).thenReturn("hash");
        when(usuarios.buscarPorUsername("cliente.nuevo")).thenReturn(
                Optional.of(new Usuario(2L, "cliente.nuevo", "hash", Rol.CLIENTE, true)));
        var registrado = service.registrarCliente("Cliente.Nuevo", "ClaveDePrueba2026", perfil());
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        assertEquals(Rol.CLIENTE, captor.getValue().getRol());
        assertEquals("cliente.nuevo", captor.getValue().getUsername());
        assertEquals("hash", captor.getValue().getPasswordHash());
        assertTrue(captor.getValue().isActivo());
        assertEquals(2L, registrado.getId());
        assertEquals(7L, captor.getValue().getClienteId());
        verify(perfiles).crear(any(Cliente.class));
        verify(passwords, times(1)).encriptar("ClaveDePrueba2026");
    }

    @Test void rechazaUsernameDuplicadoAntesDeCodificar() {
        when(usuarios.existePorUsername("admin")).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.registrarCliente("ADMIN", "ClaveDePrueba2026", perfil()));
        verifyNoInteractions(passwords);
        verify(usuarios, never()).guardar(any());
    }

    @Test void validaUsernameSinDependerDelControlador() {
        for (String username : new String[]{"", "ab", "nombre con espacios", "a".repeat(51)}) {
            assertThrows(DatoInvalidoException.class, () -> service.registrarCliente(username, "ClaveDePrueba2026", perfil()));
        }
        verifyNoInteractions(passwords);
    }

    @Test void rechazaClavesCortasHashesYMasDe72Bytes() {
        for (String password : new String[]{"admin123", " ".repeat(12), "á".repeat(37), "$2a$10$" + "a".repeat(53)}) {
            assertThrows(DatoInvalidoException.class, () -> service.registrarCliente("cliente", password, perfil()));
        }
        verifyNoInteractions(passwords);
        verify(usuarios, never()).guardar(any());
    }
}
