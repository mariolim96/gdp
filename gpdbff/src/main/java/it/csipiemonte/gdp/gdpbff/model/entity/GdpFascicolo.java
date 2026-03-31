package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_FASCICOLO")
public class GdpFascicolo extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_FASCICOLO")
    public Long id;

    @Column(name = "FK_GDP_UTENTEWEB", nullable = false)
    public Long fkGdpUtenteWeb;

    @Column(name = "TITOLO", length = 128)
    public String titolo;

    @Column(name = "NOTE_FASCICOLO", length = 512)
    public String noteFascicolo;

    @Column(name = "DT_MODIFICA")
    public LocalDate dataModifica;

    @Column(name = "DT_ELIMINAZIONE")
    public LocalDate dataEliminazione;
}
