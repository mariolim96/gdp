package it.csipiemonte.gdp.gdporch.model.dto;

import java.time.LocalDate;
import java.util.List;

public class GdpVerifDateAtteseResponse {

    public String esito;
    public List<GdpResultItem> risultati;

    public GdpVerifDateAtteseResponse(String esito, List<GdpResultItem> risultati) {
        this.esito = esito;
        this.risultati = risultati;
    }

    public static class GdpResultItem {
        
        public Integer idTestata;
        public String cartellaTestata;
        public LocalDate dataEdizioneAttesa;
        public Boolean sospesa;

        public GdpResultItem(Integer idTestata, String cartellaTestata,
                LocalDate dataEdizioneAttesa, Boolean sospesa) {
            this.idTestata = idTestata;
            this.cartellaTestata = cartellaTestata;
            this.dataEdizioneAttesa = dataEdizioneAttesa;
            this.sospesa = sospesa;
        }
    }
}