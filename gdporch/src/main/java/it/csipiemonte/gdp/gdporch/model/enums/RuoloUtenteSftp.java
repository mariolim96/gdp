package it.csipiemonte.gdp.gdporch.model.enums;

/**
 * SFTP user roles.
 */
public enum RuoloUtenteSftp {
    UTENTE_FTP("utenteFTP"),
    UTENTE_WEB("utenteWEB");

    private final String value;

    RuoloUtenteSftp(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RuoloUtenteSftp fromValue(String value) {
        for (RuoloUtenteSftp r : RuoloUtenteSftp.values()) {
            if (r.value.equalsIgnoreCase(value)) return r;
        }
        return UTENTE_FTP;
    }
}
