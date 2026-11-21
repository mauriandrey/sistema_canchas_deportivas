package org.example.sistema_canchas_deportivas.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.application.ports.out.PasswordOut;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    private UsuarioOut usuarioOut;
    private PasswordOut passwordOut;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        usuarioOut = mock(UsuarioOut.class);
        passwordOut = mock(PasswordOut.class);
        service = new UsuarioService(usuarioOut, passwordOut, mock(org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn.class), mock(org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut.class), mock(org.example.sistema_canchas_deportivas.application.ports.out.ReservaOut.class));
    }

    @Test
    void usuarioExistenteNoSeSobrescribeAunqueSeInvoqueDirectamenteElServicio() {
        when(usuarioOut.existePorUsername("admin")).thenReturn(true);
        assertFalse(service.crearAdministradorInicial("otra-clave"));
        verify(usuarioOut, never()).guardar(any());
        verifyNoInteractions(passwordOut);
    }

    @Test
    void rechazaClavesNulasOVacias() {
        assertThrows(DatoInvalidoException.class, () -> service.crearAdministradorInicial(null));
        assertThrows(DatoInvalidoException.class, () -> service.crearAdministradorInicial(" "));
        verifyNoInteractions(passwordOut);
        verify(usuarioOut, never()).guardar(any());
    }

    @Test
    void verificaLimiteDeBcryptEnBytesUtf8() {
        assertThrows(DatoInvalidoException.class,
                () -> service.crearAdministradorInicial("á".repeat(37)));
        verifyNoInteractions(passwordOut);
        verify(usuarioOut, never()).guardar(any());
    }

    @Test
    void rechazaHashBcryptComoEntradaParaEvitarDobleCodificacion() {
        String hash = "$2a$10$" + "a".repeat(53);
        assertThrows(DatoInvalidoException.class, () -> service.crearAdministradorInicial(hash));
        verifyNoInteractions(passwordOut);
        verify(usuarioOut, never()).guardar(any());
    }
}

