package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_DATA_USCITA")
public class GdpDataUscita extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_data_uscita")
    @SequenceGenerator(name = "seq_gdp_data_uscita", sequenceName = "seq_gdp_data_uscita", allocationSize = 1)
    @Column(name = "ID_GDP_DATA_USCITA")
    public Integer id;

    @Column(name = "FK_GDP_PERIODICITA", nullable = false)
    public Integer fkGdpPeriodicita;

    @Column(name = "DT_INIZIO", nullable = false)
    public LocalDate dataInizio;

    @Column(name = "DT_FINE", nullable = false)
    public LocalDate dataFine;

    @Column(name = "DATA_ATTESA", nullable = false)
    public LocalDate dataAttesa;

    @Column(name = "SOSPESA", nullable = false)
    public Boolean sospesa = false;
}
