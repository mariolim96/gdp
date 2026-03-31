package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_PAGINA")
public class GdpPagina extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_PAGINA")
    public Long id;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Long fkGdpTestata;

    @Column(name = "FK_GDP_EDIZIONE", nullable = false)
    public Long fkGdpEdizione;

    @Column(name = "NUM_PAGINA", nullable = false)
    public Integer numPagina;

    @Column(name = "FILE_PDF", nullable = false, length = 128)
    public String filePdf;

    @Column(name = "FILE_TXT", nullable = false, length = 128)
    public String fileTxt;

    @Column(name = "FILE_TIF", length = 128)
    public String fileTif;

    @Column(name = "ANNO_EDIZIONE", nullable = false)
    public Integer annoEdizione;

    @Column(name = "STATO", nullable = false)
    public Integer stato;

    @Column(name = "OBLIO", length = 256)
    public String oblio;

    @Column(name = "DATA_OBLIO")
    public LocalDate dataOblio;

    @Column(name = "NOTA_OBLIO", length = 2048)
    public String notaOblio;
}
