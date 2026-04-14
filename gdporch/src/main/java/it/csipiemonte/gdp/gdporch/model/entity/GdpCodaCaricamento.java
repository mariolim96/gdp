package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "GDP_CODA_CARICAMENTO")
public class GdpCodaCaricamento extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_coda_caricamento")
    @SequenceGenerator(name = "seq_gdp_coda_caricamento", sequenceName = "seq_gdp_coda_caricamento", allocationSize = 1)
    @Column(name = "ID_GDP_CODA_CARICAMENTO")
    public Integer id;

    @Column(name = "DT_INSERIM_IN_CODA", nullable = false)
    public LocalDateTime dataInserimento;

    @Column(name = "FK_GDP_LOG_EDIZIONE", nullable = false)
    public Integer fkGdpLogEdizione;

    @Column(name = "NRO_TENTATIVO", nullable = false)
    public Integer nroTentativo = 0;

    @Column(name = "DT_TENTATIVO")
    public LocalDateTime dataTentativo;

    @Column(name = "SFTP_PATH", nullable = false, length = 256)
    public String sftpPath;

    @Column(name = "PRIORITA")
    public Integer priorita;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATO", length = 64)
    public StatoCodaCaricamento stato;

    @Column(name = "NRO_MAX_TENTATIVI")
    public Integer nroMaxTentativi = 10;
}
