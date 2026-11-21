package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;
import org.example.sistema_canchas_deportivas.domain.models.TipoImplemento;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "utileria",
        uniqueConstraints = @UniqueConstraint(name = "uk_utileria_nombre", columnNames = "nombre"),
        check = @CheckConstraint(name = "ck_utileria_existencias", constraint = "existencias >= 0"))
public class UtileriaEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoImplemento tipo;

    @Column(nullable = false)
    private int existencias;

    @Column(nullable = false)
    private boolean activa;

    @ElementCollection
    @CollectionTable(name = "utileria_disciplinas",
            joinColumns = @JoinColumn(name = "utileria_id", foreignKey = @ForeignKey(name = "fk_disciplinas_utileria")),
            uniqueConstraints = @UniqueConstraint(name = "uk_utileria_disciplina", columnNames = {"utileria_id", "disciplina"}))
    @Enumerated(EnumType.STRING)
    @Column(name = "disciplina", nullable = false, length = 20)
    private Set<Disciplina> disciplinasCompatibles = new HashSet<>();

    @OneToMany(mappedBy = "utileria")
    private List<ReservaUtileriaEntity> reservas = new ArrayList<>();

    protected UtileriaEntity() {
        // Requerido por JPA.
    }

    public UtileriaEntity(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.existencias = existencias;
        this.activa = activa;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoImplemento getTipo() { return tipo; }
    public int getExistencias() { return existencias; }
    public boolean isActiva() { return activa; }
    public Set<Disciplina> getDisciplinasCompatibles() { return Collections.unmodifiableSet(disciplinasCompatibles); }
    public List<ReservaUtileriaEntity> getReservas() { return Collections.unmodifiableList(reservas); }

    public void definirDisciplinas(Set<Disciplina> disciplinas) {
        disciplinasCompatibles.clear();
        disciplinasCompatibles.addAll(disciplinas);
    }
    @Override
    public LocalDateTime getCreado() { return creado; }

    @Override
    public LocalDateTime getModificado() { return modificado; }

    @Override
    public void alCrear(LocalDateTime ahora) {
        this.creado = ahora;
        this.modificado = ahora;
    }

    @Override
    public void alModificar(LocalDateTime ahora) {
        this.modificado = ahora;
    }

    @Override
    public void conservarAuditoriaDe(RegistroAuditable existente) {
        this.creado = existente.getCreado();
        this.modificado = existente.getModificado();
    }
}

