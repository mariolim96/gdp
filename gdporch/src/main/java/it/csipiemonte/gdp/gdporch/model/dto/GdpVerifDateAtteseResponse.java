package it.csipiemonte.gdp.gdporch.model.dto;

import java.time.LocalDate;
import java.util.List;

public class GdpVerifDateAtteseResponse {

    private String esito;
    private String message;
    private List<GdpResultItem> risultati;

    public GdpVerifDateAtteseResponse(String esito, String message, List<GdpResultItem> risultati) {
        this.esito = esito;
        this.message = message;
        this.risultati = risultati;
    }

    public GdpVerifDateAtteseResponse(String esito, List<GdpResultItem> risultati) {
        this.esito = esito;
        this.risultati = risultati;
    }

    public String getEsito() {
        return esito;
    }

    public String getMessage() {
        return message;
    }

    public List<GdpResultItem> getRisultati() {
        return risultati;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRisultati(List<GdpResultItem> risultati) {
        this.risultati = risultati;
    }

    // Classe di supporto per rappresentare i singoli risultati della query
    public static class GdpResultItem {

        private Integer idTestata;
        private String cartellaTestata;
        private LocalDate dataEdizioneAttesa;
        private Boolean sospesa;

        public GdpResultItem(Integer idTestata, String cartellaTestata,
                LocalDate dataEdizioneAttesa, Boolean sospesa) {
            this.idTestata = idTestata;
            this.cartellaTestata = cartellaTestata;
            this.dataEdizioneAttesa = dataEdizioneAttesa;
            this.sospesa = sospesa;
        }

        public Integer getIdTestata() {
            return idTestata;
        }

        public String getCartellaTestata() {
            return cartellaTestata;
        }

        public LocalDate getDataEdizioneAttesa() {
            return dataEdizioneAttesa;
        }

        public Boolean getSospesa() {
            return sospesa;
        }

        public void setIdTestata(Integer idTestata) {
            this.idTestata = idTestata;
        }

        public void setCartellaTestata(String cartellaTestata) {
            this.cartellaTestata = cartellaTestata;
        }

        public void setDataEdizioneAttesa(LocalDate dataEdizioneAttesa) {
            this.dataEdizioneAttesa = dataEdizioneAttesa;
        }

        public void setSospesa(Boolean sospesa) {
            this.sospesa = sospesa;
        }

    }
}