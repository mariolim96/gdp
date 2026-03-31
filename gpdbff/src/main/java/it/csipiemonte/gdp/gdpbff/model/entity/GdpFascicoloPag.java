package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_FASCICOLO_PAG")
public class GdpFascicoloPag extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_FASCICOLO_PAG")
    public Long id;

    @Column(name = "FK_GDP_FASCICOLO", nullable = false)
    public Long fkGdpFascicolo;

    @Column(name = "FK_GDP_PAGINA", nullable = false)
    public Long fkGdpPagina;

    @Column(name = "NOTE_PAGINA", length = 512)
    public String notePagina;

    @Column(name = "POSIZIONE", nullable = false)
    public Integer posizione;

    @Column(name = "DT_MODIFICA")
    public LocalDate dataModifica;

    @Column(name = "DT_ELIMINAZIONE")
    public LocalDate dataEliminazione;
}
