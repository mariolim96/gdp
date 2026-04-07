package it.csipiemonte.gdp.gdporch.exception;

/**
 * Enumeration of standard message codes for the GdP application.
 * Values are used to populate the 'codice' field in GenericProcessResponse.
 */
public enum GdpMessage {

    OK(Codes.MSG00009, "Operazione completata con successo"),
    ERROR_GENERICO(Codes.MSG00001, "Errore generico di elaborazione"),
    ANOMALIA_DATA_EDIZIONE(Codes.MSG00001, "Anomalia DATA EDIZIONE — bloccante"),
    ERRORE_CODA_DAM(Codes.MSG00003, "Anomalia EDIZIONE — creazione pacchetto DAM fallita"),
    ERRORE_IO(Codes.MSG00002, "Errore di Input/Output durante l'elaborazione"),
    NO_PERIODICITA(Codes.MSG00001, "Periodicita non definita per la testata"),
    NO_DATA_USCITA(Codes.AA, "Data edizione non prevista (anomala)");

    private static class Codes {
        private static final String MSG00001 = "MSG00001";
        private static final String MSG00002 = "MSG00002";
        private static final String MSG00003 = "MSG00003";
        private static final String MSG00009 = "MSG00009";
        private static final String AA = "AA";
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
