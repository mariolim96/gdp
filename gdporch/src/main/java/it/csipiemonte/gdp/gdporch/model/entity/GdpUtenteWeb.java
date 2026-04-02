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

@Entity
@Table(name = "GDP_UTENTEWEB")
public class GdpUtenteWeb extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_utenteweb")
    @SequenceGenerator(name = "seq_gdp_utenteweb", sequenceName = "seq_gdp_utenteweb", allocationSize = 1)
    @Column(name = "ID_GDP_UTENTEWEB")
    public Integer id;

    @Column(name = "CODICE_FISCALE", nullable = false, length = 32)
    public String codiceFiscale;

    @Column(name = "COGNOME", length = 64)
    public String cognome;

    @Column(name = "NOME", length = 64)
    public String nome;

    @Column(name = "RUOLO", length = 64)
    public String ruolo;

    @Column(name = "EMAIL", nullable = false, length = 128)
    public String email;

    @Column(name = "DT_CREAZIONE", nullable = false)
    public LocalDate dataCreazione;

    @Column(name = "DT_ANNULLAMENTO")
    public LocalDate dataAnnullamento;
}
