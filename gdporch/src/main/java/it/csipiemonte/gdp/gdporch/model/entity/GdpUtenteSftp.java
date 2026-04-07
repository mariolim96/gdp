package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import it.csipiemonte.gdp.gdporch.model.enums.StatoUtenteSftp;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "GDP_UTENTESFTP")
public class GdpUtenteSftp extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_utentesftp")
    @SequenceGenerator(name = "seq_gdp_utentesftp", sequenceName = "seq_gdp_utentesftp", allocationSize = 1)
    @Column(name = "ID_GDP_UTENTESFTP")
    public Integer id;

    @Column(name = "USERNAME", nullable = false, length = 128)
    public String username;

    @Column(name = "PASSWORD", nullable = false, length = 128)
    public String password;

    @Column(name = "RIF_TESTATA", length = 128)
    public String rifTestata;

    @Column(name = "HOME_SFTP", nullable = false, length = 256)
    public String homeSftp;

    @Column(name = "DIRETTORE", length = 50)
    public String direttore;

    @Column(name = "REFERENTE_SFTP", length = 50)
    public String referenteSftp;

    @Column(name = "EMAIL", length = 50)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATO", length = 50)
    public StatoUtenteSftp stato;
}
