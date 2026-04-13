package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "GDP_LOG_EDIZIONE")
public class GdpLogEdizione extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_log_edizione")
    @SequenceGenerator(name = "seq_gdp_log_edizione", sequenceName = "seq_gdp_log_edizione", allocationSize = 1)
    @Column(name = "ID_GDP_LOG_EDIZIONE")
    public Integer id;

    @Column(name = "FK_GDP_LOG", nullable = false)
    public Integer fkGdpLog;

    @Column(name = "NRO_PAG_ACQUISITE")
    public Integer nroPagAcquisite;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_EDIZIONE", length = 64)
    public TipoEdizione tipoEdizione;

    @Column(name = "FK_GDP_EDIZIONE", nullable = true)
    public Integer fkGdpEdizione;

    @Column(name = "PATH_EDIZIONE", length = 256)
    public String pathEdizione;

    @Column(name = "NRO_PAG_VALIDE")
    public Integer nroPagValide;

    @Column(name = "NRO_PAG_ERRATE")
    public Integer nroPagErrate;

    @Column(name = "PRIMA_PAGINA")
    public Boolean primaPagina;

    @Column(name = "FILE_XML")
    public Boolean fileXml;

    @Column(name = "FILE_ZIP")
    public Boolean fileZip;

    @Column(name = "JOB_ID", length = 128)
    public String jobId;

    @Column(name = "DESCRIZIONE", length = 8192)
    public String descrizione;
}
