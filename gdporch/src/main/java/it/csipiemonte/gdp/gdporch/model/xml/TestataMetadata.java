package it.csipiemonte.gdp.gdporch.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;
import java.util.Date;

@XmlType(propOrder = {
        "idTestata", "nomeTestata", "cartella", "stato", "dataStato", "cancellazione",
        "codTema", "tema", "socEditrice", "enteProponente", "annoFondazione",
        "periodoFreq", "periodoGg", "descrizione", "www", "mail",
        "provincia", "comune", "indirizzo", "cap", "longitudine", "latitudine"
})
public class TestataMetadata {

    private Integer idTestata;
    private String nomeTestata;
    private String cartella;
    private Integer stato;
    private Date dataStato;
    private Date cancellazione;
    private Integer codTema;
    private String tema;
    private String socEditrice;
    private String enteProponente;
    private Integer annoFondazione;
    private String periodoFreq;
    private String periodoGg;
    private String descrizione;
    private String www;
    private String mail;
    private String provincia;
    private String comune;
    private String indirizzo;
    private String cap;
    private BigDecimal longitudine;  // allineato a GdpTestata (era Float)
    private BigDecimal latitudine;   // allineato a GdpTestata (era Float)

    @XmlElement(name = "id_testata")
    public Integer getIdTestata() { return idTestata; }
    public void setIdTestata(Integer v) { this.idTestata = v; }

    @XmlElement(name = "nome_testata")
    public String getNomeTestata() { return nomeTestata; }
    public void setNomeTestata(String v) { this.nomeTestata = v; }

    @XmlElement(name = "cartella_testata")
    public String getCartella() { return cartella; }
    public void setCartella(String v) { this.cartella = v; }

    @XmlElement(name = "stato")
    public Integer getStato() { return stato; }
    public void setStato(Integer v) { this.stato = v; }

    @XmlElement(name = "data_stato")
    public Date getDataStato() { return dataStato; }
    public void setDataStato(Date v) { this.dataStato = v; }

    @XmlElement(name = "cancellazione")
    public Date getCancellazione() { return cancellazione; }
    public void setCancellazione(Date v) { this.cancellazione = v; }

    @XmlElement(name = "cod_tema")
    public Integer getCodTema() { return codTema; }
    public void setCodTema(Integer v) { this.codTema = v; }

    @XmlElement(name = "tema")
    public String getTema() { return tema; }
    public void setTema(String v) { this.tema = v; }

    @XmlElement(name = "soc_editrice")
    public String getSocEditrice() { return socEditrice; }
    public void setSocEditrice(String v) { this.socEditrice = v; }

    @XmlElement(name = "ente_proponente")
    public String getEnteProponente() { return enteProponente; }
    public void setEnteProponente(String v) { this.enteProponente = v; }

    @XmlElement(name = "anno_fondazione")
    public Integer getAnnoFondazione() { return annoFondazione; }
    public void setAnnoFondazione(Integer v) { this.annoFondazione = v; }

    @XmlElement(name = "periodo_freq")
    public String getPeriodoFreq() { return periodoFreq; }
    public void setPeriodoFreq(String v) { this.periodoFreq = v; }

    @XmlElement(name = "periodo_gg")
    public String getPeriodoGg() { return periodoGg; }
    public void setPeriodoGg(String v) { this.periodoGg = v; }

    @XmlElement(name = "descrizione")
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String v) { this.descrizione = v; }

    @XmlElement(name = "www")
    public String getWww() { return www; }
    public void setWww(String v) { this.www = v; }

    @XmlElement(name = "mail")
    public String getMail() { return mail; }
    public void setMail(String v) { this.mail = v; }

    @XmlElement(name = "provincia")
    public String getProvincia() { return provincia; }
    public void setProvincia(String v) { this.provincia = v; }

    @XmlElement(name = "comune")
    public String getComune() { return comune; }
    public void setComune(String v) { this.comune = v; }

    @XmlElement(name = "indirizzo")
    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String v) { this.indirizzo = v; }

    @XmlElement(name = "cap")
    public String getCap() { return cap; }
    public void setCap(String v) { this.cap = v; }

    @XmlElement(name = "longitudine")
    public BigDecimal getLongitudine() { return longitudine; }
    public void setLongitudine(BigDecimal v) { this.longitudine = v; }

    @XmlElement(name = "latitudine")
    public BigDecimal getLatitudine() { return latitudine; }
    public void setLatitudine(BigDecimal v) { this.latitudine = v; }
}