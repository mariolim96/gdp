package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import it.csipiemonte.gdp.gdpbff.model.enums.StatoCodaCaricamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GDP_CODA_CARICAMENTO")
public class GdpCodaCaricamento extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_CODA_CARICAMENTO")
    public Long id;

    @Column(name = "DT_INSERIM_IN_CODA", nullable = false)
    public LocalDate dataInserimento;

    @Column(name = "FK_GDP_LOG_EDIZIONE", nullable = false)
    public Long fkGdpLogEdizione;

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
