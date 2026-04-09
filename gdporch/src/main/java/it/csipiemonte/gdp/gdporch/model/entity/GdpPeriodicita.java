package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "GDP_PERIODICITA")
public class GdpPeriodicita extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_periodicita")
    @SequenceGenerator(name = "seq_gdp_periodicita", sequenceName = "seq_gdp_periodicita", allocationSize = 1)
    @Column(name = "ID_GDP_PERIODICITA")
    public Integer id;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Integer fkGdpTestata;

    @Column(name = "MENSILITA", nullable = false)
    public Integer mensilita;

    @Column(name = "GG_PERIODICITA", nullable = false, length = 128)
    public String ggPeriodicita;

    @Column(name = "DT_FINE_VALIDITA")
    public LocalDate dataFineValidita;

    @Column(name = "INIZIO_SOSPENSIONE")
    public LocalDate inizioSospensione;

    @Column(name = "FINE_SOSPENSIONE")
    public LocalDate fineSospensione;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFkGdpTestata() {
        return fkGdpTestata;
    }

    public void setFkGdpTestata(Integer fkGdpTestata) {
        this.fkGdpTestata = fkGdpTestata;
    }

    public Integer getMensilita() {
        return mensilita;
    }

    public void setMensilita(Integer mensilita) {
        this.mensilita = mensilita;
    }

    public String getGgPeriodicita() {
        return ggPeriodicita;
    }

    public void setGgPeriodicita(String ggPeriodicita) {
        this.ggPeriodicita = ggPeriodicita;
    }

    public LocalDate getDataFineValidita() {
        return dataFineValidita;
    }

    public void setDataFineValidita(LocalDate dataFineValidita) {
        this.dataFineValidita = dataFineValidita;
    }

    public LocalDate getInizioSospensione() {
        return inizioSospensione;
    }

    public void setInizioSospensione(LocalDate inizioSospensione) {
        this.inizioSospensione = inizioSospensione;
    }

    public LocalDate getFineSospensione() {
        return fineSospensione;
    }

    public void setFineSospensione(LocalDate fineSospensione) {
        this.fineSospensione = fineSospensione;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GdpPeriodicita that = (GdpPeriodicita) o;
        return Objects.equals(id, that.id) && Objects.equals(fkGdpTestata, that.fkGdpTestata) && Objects.equals(mensilita, that.mensilita) && Objects.equals(ggPeriodicita, that.ggPeriodicita) && Objects.equals(dataFineValidita, that.dataFineValidita) && Objects.equals(inizioSospensione, that.inizioSospensione) && Objects.equals(fineSospensione, that.fineSospensione);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fkGdpTestata, mensilita, ggPeriodicita, dataFineValidita, inizioSospensione, fineSospensione);
    }

    @Override
    public String toString() {
        return "GdpPeriodicita{" +
                "id=" + id +
                ", fkGdpTestata=" + fkGdpTestata +
                ", mensilita=" + mensilita +
                ", ggPeriodicita='" + ggPeriodicita + '\'' +
                ", dataFineValidita=" + dataFineValidita +
                ", inizioSospensione=" + inizioSospensione +
                ", fineSospensione=" + fineSospensione +
                '}';
    }
}
