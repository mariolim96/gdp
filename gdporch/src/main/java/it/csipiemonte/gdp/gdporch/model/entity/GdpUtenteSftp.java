package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_UTENTESFTP")
public class GdpUtenteSftp extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_UTENTESFTP")
    public Long id;

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

    @Column(name = "STATO", length = 50)
    public String stato;
}
