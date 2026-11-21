package org.example.sistema_canchas_deportivas.infraestructure.auditoria;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.stereotype.Component;

/** Mismos eventos del ejemplo de clase, con el Clock y la zona del complejo. */
@Component
public class AuditoriaListener {
    private final Clock clock;

    public AuditoriaListener(Clock clock) { this.clock = clock; }

    @PrePersist
    public void alCrear(Object objeto) {
        ((RegistroAuditable) objeto).alCrear(ahora());
    }

    @PreUpdate
    public void alModificar(Object objeto) {
        ((RegistroAuditable) objeto).alModificar(ahora());
    }

    private LocalDateTime ahora() {
        // DATETIME(6) guarda microsegundos; evita diferencias al recargar desde MySQL.
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
