package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_MAIL")
public class GdpMail extends PanacheEntityBase {

    @Id
    @Column(name = "COD_MAIL", length = 5)
    public String id; // COD_MAIL is the PK

    @Column(name = "SMTP_MAIL_HOST", nullable = false, length = 128)
    public String smtpMailHost;

    @Column(name = "SMTP_MAIL_PORTA", nullable = false)
    public Integer smtpMailPorta;

    @Column(name = "MITTENTE", nullable = false, length = 128)
    public String mittente;

    @Column(name = "TESTO_OGGETTO", nullable = false, length = 1024)
    public String testoOggetto;

    @Column(name = "TESTO_MAIL", nullable = false, length = 4096)
    public String testoMail;
}
