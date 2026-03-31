package it.csipiemonte.gdp.gdpbff.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_PREF_EDIZIONE")
public class GdpPrefEdizione extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_PREF_EDIZIONE")
    public Long id;

    @Column(name = "FK_GDP_UTENTEWEB", nullable = false)
    public Long fkGdpUtenteWeb;

    @Column(name = "FK_GDP_EDIZIONE", nullable = false)
    public Long fkGdpEdizione;

    @Column(name = "DT_PUBBLICAZIONE", nullable = false)
    public LocalDate dataPubblicazione;

    @Column(name = "DT_CREAZIONE", nullable = false)
    public LocalDate dataCreazione;
}
