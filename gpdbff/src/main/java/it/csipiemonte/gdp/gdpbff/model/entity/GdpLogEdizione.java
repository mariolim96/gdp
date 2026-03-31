package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_LOG_EDIZIONE")
public class GdpLogEdizione extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_LOG_EDIZIONE")
    public Long id;

    @Column(name = "FK_GDP_LOG", nullable = false)
    public Long fkGdpLog;

    @Column(name = "NRO_PAG_ACQUISITE")
    public Integer nroPagAcquisite;

    @Column(name = "TIPO_EDIZIONE", length = 64)
    public String tipoEdizione;

    @Column(name = "FK_GDP_EDIZIONE", nullable = false)
    public Long fkGdpEdizione;

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
