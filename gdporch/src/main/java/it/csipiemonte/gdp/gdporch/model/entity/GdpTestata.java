package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_TESTATA")
public class GdpTestata extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_TESTATA")
    public Long id;

    @Column(name = "NOME_TESTATA", nullable = false, length = 256)
    public String nomeTestata;

    @Column(name = "CARTELLA_TESTATA", length = 256)
    public String cartellaTestata;

    @Column(name = "INVIO_EDIZIONE", nullable = false)
    public Boolean invioEdizione;

    @Column(name = "STATO", nullable = false)
    public Integer stato;

    @Column(name = "DATA_STATO")
    public LocalDate dataStato;

    @Column(name = "CANCELLAZIONE")
    public LocalDate cancellazione;

    @Column(name = "COD_TEMA", nullable = false)
    public Integer codTema;

    @Column(name = "TEMA", length = 128)
    public String tema;

    @Column(name = "SOC_EDITRICE", length = 256)
    public String socEditrice;

    @Column(name = "ENTE_PROPONENTE", length = 256)
    public String enteProponente;

    @Column(name = "ANNO_FONDAZIONE")
    public Integer annoFondazione;

    @Column(name = "PERIODO_FREQ", length = 128)
    public String periodoFreq;

    @Column(name = "PERIODO_GG", length = 128)
    public String periodoGg;

    @Column(name = "DESCRIZIONE", length = 2048)
    public String descrizione;

    @Column(name = "WWW", length = 128)
    public String www;

    @Column(name = "MAIL", length = 128)
    public String mail;

    @Column(name = "PROVINCIA", nullable = false, length = 2)
    public String provincia;

    @Column(name = "COMUNE", length = 128)
    public String comune;

    @Column(name = "INDIRIZZO", length = 256)
    public String indirizzo;

    @Column(name = "CAP", length = 40)
    public String cap;

    @Column(name = "LONGITUDINE", precision = 12, scale = 9)
    public BigDecimal longitudine;

    @Column(name = "LATITUDINE", precision = 11, scale = 9)
    public BigDecimal latitudine;
}
