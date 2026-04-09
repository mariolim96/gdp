package it.csipiemonte.gdp.gdporch.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Date;

@XmlType(propOrder = {
        "idPagina", "fkGdpTestata", "fkGdpEdizione", "numPagina",
        "filePdf", "fileTxt", "fileTif",
        "annoEdizione", "stato", "oblio", "dataOblio", "notaOblio"
})
public class PaginaMetadata {

    private Integer idPagina;
    private Integer fkGdpTestata;
    private Integer fkGdpEdizione;
    private Integer numPagina;
    private String filePdf;
    private String fileTxt;
    private String fileTif;
    private Integer annoEdizione;
    private Integer stato;
    private String oblio;
    private Date dataOblio;
    private String notaOblio;

    @XmlElement(name = "id_pagina")
    public Integer getIdPagina() { return idPagina; }
    public void setIdPagina(Integer v) { this.idPagina = v; }

    @XmlElement(name = "fk_gdp_testata")
    public Integer getFkGdpTestata() { return fkGdpTestata; }
    public void setFkGdpTestata(Integer v) { this.fkGdpTestata = v; }

    @XmlElement(name = "fk_gdp_edizione")
    public Integer getFkGdpEdizione() { return fkGdpEdizione; }
    public void setFkGdpEdizione(Integer v) { this.fkGdpEdizione = v; }

    @XmlElement(name = "num_pagina")
    public Integer getNumPagina() { return numPagina; }
    public void setNumPagina(Integer v) { this.numPagina = v; }

    @XmlElement(name = "file_pdf")
    public String getFilePdf() { return filePdf; }
    public void setFilePdf(String v) { this.filePdf = v; }

    @XmlElement(name = "file_txt")
    public String getFileTxt() { return fileTxt; }
    public void setFileTxt(String v) { this.fileTxt = v; }

    @XmlElement(name = "file_tif")
    public String getFileTif() { return fileTif; }
    public void setFileTif(String v) { this.fileTif = v; }

    @XmlElement(name = "anno_edizione")
    public Integer getAnnoEdizione() { return annoEdizione; }
    public void setAnnoEdizione(Integer v) { this.annoEdizione = v; }

    @XmlElement(name = "stato")
    public Integer getStato() { return stato; }
    public void setStato(Integer v) { this.stato = v; }

    @XmlElement(name = "oblio")
    public String getOblio() { return oblio; }
    public void setOblio(String v) { this.oblio = v; }

    @XmlElement(name = "data_oblio")
    public Date getDataOblio() { return dataOblio; }
    public void setDataOblio(Date v) { this.dataOblio = v; }

    @XmlElement(name = "nota_oblio")
    public String getNotaOblio() { return notaOblio; }
    public void setNotaOblio(String v) { this.notaOblio = v; }
}