package it.csipiemonte.gdp.gdporch.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Date;

@XmlType(propOrder = {
        "idEdizione", "fkGdpTestata", "dataEdizione", "dataPubblicazione", "stato", "numeroPagine"
})
public class EdizioneMetadata {

    private Integer idEdizione;
    private Integer fkGdpTestata;
    private Date dataEdizione;
    private Date dataPubblicazione;  // era Integer, corretto in Date (LocalDate su entity)
    private Integer stato;
    private Integer numeroPagine;

    @XmlElement(name = "id_edizione")
    public Integer getIdEdizione() { return idEdizione; }
    public void setIdEdizione(Integer v) { this.idEdizione = v; }

    @XmlElement(name = "fk_gdp_testata")
    public Integer getFkGdpTestata() { return fkGdpTestata; }
    public void setFkGdpTestata(Integer v) { this.fkGdpTestata = v; }

    @XmlElement(name = "data_edizione")
    public Date getDataEdizione() { return dataEdizione; }
    public void setDataEdizione(Date v) { this.dataEdizione = v; }

    @XmlElement(name = "data_pubblicazione")
    public Date getDataPubblicazione() { return dataPubblicazione; }
    public void setDataPubblicazione(Date v) { this.dataPubblicazione = v; }

    @XmlElement(name = "stato")
    public Integer getStato() { return stato; }
    public void setStato(Integer v) { this.stato = v; }

    @XmlElement(name = "numero_pagine")
    public Integer getNumeroPagine() { return numeroPagine; }
    public void setNumeroPagine(Integer v) { this.numeroPagine = v; }
}