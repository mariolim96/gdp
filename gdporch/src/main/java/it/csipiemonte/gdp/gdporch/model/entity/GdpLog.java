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
import java.time.LocalDateTime;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "GDP_LOG")
public class GdpLog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_log")
    @SequenceGenerator(name = "seq_gdp_log", sequenceName = "seq_gdp_log", allocationSize = 1)
    @Column(name = "ID_GDP_LOG")
    public Integer id;

    @Column(name = "FK_GDP_UTENTEFTP", nullable = false)
    public Integer fkGdpUtenteFtp;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Integer fkGdpTestata;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ACQUISIZIONE", nullable = false, length = 16)
    public TipoAcquisizione tipoAcquisizione; // G = giornaliera S = storica

    @Column(name = "DT_ACQUISIZIONE", nullable = false)
    public LocalDate dataAcquisizione;

    @Column(name = "TOTALE_FILE_ACQUISITI")
    public Integer totaleFileAcquisiti;

    @Column(name = "ESITO", length = 1024)
    public String esito;
}
