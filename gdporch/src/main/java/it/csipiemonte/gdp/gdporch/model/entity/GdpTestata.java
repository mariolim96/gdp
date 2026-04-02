package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_TESTATA")
public class GdpTestata extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_testata")
    @SequenceGenerator(name = "seq_gdp_testata", sequenceName = "seq_gdp_testata", allocationSize = 1)
    @Column(name = "ID_GDP_TESTATA")
    public Integer id;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeTestata() {
        return nomeTestata;
    }

    public void setNomeTestata(String nomeTestata) {
        this.nomeTestata = nomeTestata;
    }

    public String getCartellaTestata() {
        return cartellaTestata;
    }

    public void setCartellaTestata(String cartellaTestata) {
        this.cartellaTestata = cartellaTestata;
    }

    public Boolean getInvioEdizione() {
        return invioEdizione;
    }

    public void setInvioEdizione(Boolean invioEdizione) {
        this.invioEdizione = invioEdizione;
    }

    public Integer getStato() {
        return stato;
    }

    public void setStato(Integer stato) {
        this.stato = stato;
    }

    public LocalDate getDataStato() {
        return dataStato;
    }

    public void setDataStato(LocalDate dataStato) {
        this.dataStato = dataStato;
    }

    public LocalDate getCancellazione() {
        return cancellazione;
    }

    public void setCancellazione(LocalDate cancellazione) {
        this.cancellazione = cancellazione;
    }

    public Integer getCodTema() {
        return codTema;
    }

    public void setCodTema(Integer codTema) {
        this.codTema = codTema;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getSocEditrice() {
        return socEditrice;
    }

    public void setSocEditrice(String socEditrice) {
        this.socEditrice = socEditrice;
    }

    public String getEnteProponente() {
        return enteProponente;
    }

    public void setEnteProponente(String enteProponente) {
        this.enteProponente = enteProponente;
    }

    public Integer getAnnoFondazione() {
        return annoFondazione;
    }

    public void setAnnoFondazione(Integer annoFondazione) {
        this.annoFondazione = annoFondazione;
    }

    public String getPeriodoFreq() {
        return periodoFreq;
    }

    public void setPeriodoFreq(String periodoFreq) {
        this.periodoFreq = periodoFreq;
    }

    public String getPeriodoGg() {
        return periodoGg;
    }

    public void setPeriodoGg(String periodoGg) {
        this.periodoGg = periodoGg;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getWww() {
        return www;
    }

    public void setWww(String www) {
        this.www = www;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getComune() {
        return comune;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public BigDecimal getLongitudine() {
        return longitudine;
    }

    public void setLongitudine(BigDecimal longitudine) {
        this.longitudine = longitudine;
    }

    public BigDecimal getLatitudine() {
        return latitudine;
    }

    public void setLatitudine(BigDecimal latitudine) {
        this.latitudine = latitudine;
    }
}
