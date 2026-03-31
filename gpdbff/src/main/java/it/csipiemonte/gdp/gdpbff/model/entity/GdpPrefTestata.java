package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_PREF_TESTATA")
public class GdpPrefTestata extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_PREF_TESTATA")
    public Long id;

    @Column(name = "FK_GDP_UTENTEWEB", nullable = false)
    public Long fkGdpUtenteWeb;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Long fkGdpTestata;

    @Column(name = "NOME_TESTATA", length = 128)
    public String nomeTestata;

    @Column(name = "DT_CREAZIONE", nullable = false)
    public LocalDate dataCreazione;
}
