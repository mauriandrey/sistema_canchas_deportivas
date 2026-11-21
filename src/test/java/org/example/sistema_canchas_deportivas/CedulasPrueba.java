package org.example.sistema_canchas_deportivas;

import java.security.SecureRandom;
import java.util.Locale;

/** Números sintéticos para fixtures; no corresponden a identidades verificadas. */
public final class CedulasPrueba {
    private static final SecureRandom RANDOM = new SecureRandom();
    private CedulasPrueba() {}
    public static String nueva() {
        String base = String.format(Locale.ROOT, "17%07d", RANDOM.nextInt(6_000_000));
        int suma = 0;
        for (int i = 0; i < base.length(); i++) {
            int valor = Character.digit(base.charAt(i), 10) * (i % 2 == 0 ? 2 : 1);
            suma += valor / 10 + valor % 10;
        }
        return base + (10 - suma % 10) % 10;
    }
}
