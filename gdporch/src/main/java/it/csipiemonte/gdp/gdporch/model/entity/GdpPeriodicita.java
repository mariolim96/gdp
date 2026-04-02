package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_PERIODICITA")
public class GdpPeriodicita extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_PERIODICITA")
    public Integer id;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Integer fkGdpTestata;

    @Column(name = "MENSILITA", nullable = false)
    public Integer mensilita;

    @Column(name = "GG_PERIODICITA", nullable = false, length = 128)
    public String ggPeriodicita;

    @Column(name = "DT_FINE_VALIDITA")
    public LocalDate dataFineValidita;

    @Column(name = "INIZIO_SOSPENSIONE")
    public LocalDate inizioSospensione;

    @Column(name = "FINE_SOSPENSIONE")
    public LocalDate fineSospensione;
}
