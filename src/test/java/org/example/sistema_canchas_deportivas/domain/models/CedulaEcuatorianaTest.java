package org.example.sistema_canchas_deportivas.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;
import static org.junit.jupiter.api.Assertions.*;

class CedulaEcuatorianaTest {
    @ParameterizedTest
    @ValueSource(strings = {"1750397802", "0100000009", "0100000090", "2400000002", "3040000006", "3050000003"})
    void aceptaCasosValidosConCerosInicialesYVerificadorCero(String cedula) {
        assertTrue(CedulaEcuatoriana.esValida(cedula));
        assertEquals(cedula, Cliente.nuevo(cedula, "Nombre", "Prueba", "prueba@example.test", null).getCedula());
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"175039780", "17503978020", "1750397802001", "1750397803", "175039780A",
            " 1750397802", "1750397802 ", "17503 7802", "17503-7802", "0000000000", "2500000001",
            "1760000008", "3030000008", "3100000003", "１７５０３９７８０２", "١٧٥٠٣٩٧٨٠٢", "9999999999"})
    void rechazaFormatoRegionTercerDigitoOVerificadorIncorrectos(String cedula) {
        assertFalse(CedulaEcuatoriana.esValida(cedula));
        assertThrows(DatoInvalidoException.class, () -> Cliente.nuevo(cedula, "Nombre", "Prueba", "prueba@example.test", null));
    }

    @Test void cualquierCambioDelVerificadorDelEjemploEsInvalido() {
        for (int digito = 0; digito <= 9; digito++) {
            assertEquals(digito == 2, CedulaEcuatoriana.esValida("175039780" + digito));
        }
    }

    @Test void noModificaElClienteCuandoLaCedulaNuevaEsInvalida() {
        var cliente = Cliente.nuevo("1750397802", "Nombre", "Prueba", "prueba@example.test", null);
        assertThrows(DatoInvalidoException.class, () -> cliente.actualizar("1750397803", "Cambio", "Cambio", "otro@example.test", "0991234567"));
        assertEquals("1750397802", cliente.getCedula());
        assertEquals("Nombre", cliente.getNombres()); assertEquals("prueba@example.test", cliente.getEmail());
    }
}
