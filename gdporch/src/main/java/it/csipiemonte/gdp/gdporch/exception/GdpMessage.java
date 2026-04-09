package it.csipiemonte.gdp.gdporch.exception;

/**
 * Enumeration of standard message codes for the GdP application.
 * Values are used to populate the 'codice' field in GenericProcessResponse.
 */
public enum GdpMessage {

    F_OK(Codes.MSG00009, "Operazione completata con successo"),
    F_ERROR(Codes.MSG00001, "Errore generico di elaborazione"),
    F_IO_ERROR(Codes.MSG00002, "Errore di Input/Output durante l'elaborazione"),
    F_NOT_FOUND(Codes.MSG00002, "Entita non trovata"),

    // F01 - configDTEdizioneAttesa
    F01_NO_OCCURRENCES(Codes.MSG00001, "Nessuna occorrenza trovata per i parametri inseriti"),
    F01_NO_MENSILITA(Codes.MSG00002, "Testata attiva senza MENSILITA definita"),
    F01_NO_PERIODICITA(Codes.MSG00003, "Testata attiva senza GG_PERIODICITA definita"),

    // F02 - creaCartellaEdizioneAttesa
    F02_OK(Codes.MSG00009, "OK"),
    F02_NO_OCCURRENCES(Codes.MSG00001, "Nessuna edizione attesa per domani"),

    // F03 - checkEdizioneAttesa
    F03_OK(Codes.MSG00009, "OK"),
    F03_NO_NEW_EDITION(Codes.MSG00001, "Nessuna nuova edizione trovata"),
    F03_AMBIGUOUS_TESTATA(Codes.MSG00002, "<E001> Anomalia UNICITA' TESTATA — multiple IDs found for cartella"),
    F03_TESTATA_NOT_FOUND(Codes.MSG00003, "<E001> Anomalia ESISTENZA — cartella testata not found"),

    // F04 - ctrlEdizioneAcquisita
    F04_DATE_ANOMALY(Codes.MSG00001, "<E002> Anomalia DATA EDIZIONE — blocking date anomaly"),
    F04_DB_ERROR(Codes.MSG00002, "<E003> Anomalia EDIZIONE — DB insert failed"),
    F04_DAM_ERROR(Codes.MSG00003, "<E004> Anomalia EDIZIONE — DAM package creation failed"),

    // F05 - sospensioneEdizioneAttesa / F18 - verifDateAttese / F15 - ricerca
    F05_NO_RESULTS(Codes.MSG00001, "Nessun risultato trovato per i parametri inseriti"),

    // F16 - DB.getElencoTestate
    F16_OK(Codes.MSG00009, "OK"),
    F16_INVALID_FILTERS(Codes.MSG00001, "Solo un filtro alla volta è consentito"),

    // F17 - DB.getTestata
    F17_OK(Codes.MSG00009, "OK"),
    F17_NOT_FOUND(Codes.MSG00002, "Testata non trovata"),

    // F06 - checkConsegnaStorico
    F06_OK(Codes.MSG00009, "OK"),
    F06_NO_HISTORICAL(Codes.MSG00001, "Nessuna nuova consegna storica trovata"),
    F06_AMBIGUOUS_TESTATA(Codes.MSG00002, "<E101> Anomalia UNICITA' — multiple testata IDs found"),
    F06_TESTATA_NOT_FOUND(Codes.MSG00003, "<E102> Anomalia ESISTENZA — testata ID not found"),

    // F07 - ctrlEdizioniStoriche
    F07_OK(Codes.MSG00009,
            "<MSG>Elaborazione completata per la Testata %d - %s<MSG>\n\nEdizioni esaminate %d\n%d file PDF di cui %d scartati\n%d file TXT di cui %d scartati\n%d file TIF di cui %d scartati"),
    F07_WRONG_DATE_FORMAT(Codes.MSG00001,
            "<E103>%d - Edizione %s con formato errato <E103>\nspostata in %s/CONS_%s/%s/%s"),
    F07_MOVE_ERRATA(Codes.MSG00002, "Edizione %d spostata in %s/CONS_%s/%s/%s"),
    F07_DB_ERROR(Codes.MSG00003,
            "<E104>Anomalia EDIZIONE - [%s]<E104>\nNon è stato possibile inserire sul DB l’edizione %s della testata %d - %s"),
    F07_DAM_ERROR(Codes.MSG00004,
            "<E105>Anomalia EDIZIONE - [%s]<E105>\nSi è verificato un errore nella creazione del file per la trasmissione al DAM dell’edizione %s della testata %d - %s"),

    // F08 - insEdizione
    F08_INSERT_EDIZIONE_FAILED(Codes.MSG00001, "Error inserting GDP_EDIZIONE"),
    F08_INSERT_PAGINA_FAILED(Codes.MSG00002, "Error inserting GDP_PAGINA"),

    // F09 - creaXMLEdizione
    F09_XML_CREATION_FAILED(Codes.MSG00002, "Error creating XML file"),
    F09_ZIP_CREATION_FAILED(Codes.MSG00003, "Error creating ZIP file"),

    // F10 - inviaEdizione
    F10_UPLOAD_EXECUTED(Codes.MSG00009, "<MSG> DAM upload executed"),
    F10_DAM_TRANSMISSION_FAILED(Codes.MSG00001, "<E005> Anomalia EDIZIONE DAM — transmission failed"),

    // F12 / F13 - monitor
    F12_MONITOR_ERROR(Codes.MSG00001, "Error retrieving monitoring data"),

    // F14 - preparaMAIL / F22 - invioMAIL
    F14_MAIL_NOT_FOUND(Codes.MSG00001, "Email address not found"),
    F22_MAIL_SEND_FAILED(Codes.MSG00001, "Mail send failed"),

    // F20 - statoDAM
    F20_DATO_NON_TROVATO(Codes.MSG00001, "Dato non trovato"),

    // F21 - attivaCODA
    F21_MAX_RETRIES(Codes.MSG00001, "ATTENZIONE! Superato il numero massimo di tentativi ammessi");

    public static class Codes {
        public static final String MSG00001 = "MSG00001";
        public static final String MSG00002 = "MSG00002";
        public static final String MSG00003 = "MSG00003";
        public static final String MSG00004 = "MSG00004";
        public static final String MSG00009 = "MSG00009";
    }

    private final String codice;
    private final String descrizioneDefault;

    GdpMessage(String codice, String descrizioneDefault) {
        this.codice = codice;
        this.descrizioneDefault = descrizioneDefault;
    }

    public String getCodice() {
        return codice;
    }

    public String getDescrizioneDefault() {
        return descrizioneDefault;
    }
}
