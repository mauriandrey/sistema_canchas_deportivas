package org.example.sistema_canchas_deportivas.domain.models;

import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

/** Validación local de estructura y dígito verificador; no consulta identidades al Registro Civil. */
public final class CedulaEcuatoriana {
    public static final String MENSAJE = "Ingresa una cédula ecuatoriana válida de 10 dígitos.";

    private CedulaEcuatoriana() {}

    public static boolean esValida(String cedula) {
        if (cedula == null || !cedula.matches("[0-9]{10}")) return false;
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        int tercero = cedula.charAt(2) - '0';
        boolean nacional = provincia >= 1 && provincia <= 24 && tercero < 6;
        // Serie de ecuatorianos registrados en consulados del exterior.
        boolean exterior = provincia == 30 && (tercero == 4 || tercero == 5);
        if (!nacional && !exterior) return false;
        int suma = 0;
        for (int posicion = 0; posicion < 9; posicion++) {
            int producto = (cedula.charAt(posicion) - '0') * (posicion % 2 == 0 ? 2 : 1);
            suma += producto > 9 ? producto - 9 : producto;
        }
        int verificador = (10 - suma % 10) % 10;
        return verificador == cedula.charAt(9) - '0';
    }

    public static void validar(String cedula) {
        if (!esValida(cedula)) throw new DatoInvalidoException(MENSAJE);
    }
}
