package it.csipiemonte.gdp.gdporch.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlRootElement(name = "edizione")
@XmlType(propOrder = {"testata", "edizione", "pagine"})
public class EdizioneXml {

    private TestataMetadata testata;
    private EdizioneMetadata edizione;
    private List<PaginaMetadata> pagine;

    @XmlElement(name = "testata")
    public TestataMetadata getTestata() { return testata; }

    @XmlElement(name = "edizione")
    public EdizioneMetadata getEdizione() { return edizione; }

    @XmlElement(name = "pagina")
    public List<PaginaMetadata> getPagine() { return pagine; }

    public void setTestata(TestataMetadata testata) {
        this.testata = testata;
    }

    public void setPagine(List<PaginaMetadata> pagine) {
        this.pagine = pagine;
    }

    public void setEdizione(EdizioneMetadata edizione) {
        this.edizione = edizione;
    }
}
