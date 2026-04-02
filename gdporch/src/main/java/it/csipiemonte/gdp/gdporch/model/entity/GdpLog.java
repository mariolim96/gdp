package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "GDP_LOG")
public class GdpLog extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_LOG")
    public Integer id;

    @Column(name = "FK_GDP_UTENTEFTP", nullable = false)
    public Integer fkGdpUtenteFtp;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Integer fkGdpTestata;

    @Column(name = "TIPO_ACQUISIZIONE", nullable = false, length = 16)
    public String tipoAcquisizione; // G = giornaliera S = storica

    @Column(name = "DT_ACQUISIZIONE", nullable = false)
    public LocalDateTime dataAcquisizione;

    @Column(name = "TOTALE_FILE_ACQUISITI")
    public Integer totaleFileAcquisiti;

    @Column(name = "ESITO", length = 1024)
    public String esito;
}
