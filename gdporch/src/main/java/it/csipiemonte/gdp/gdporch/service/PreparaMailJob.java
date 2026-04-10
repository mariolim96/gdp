package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.MailComposition;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpMail;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpMailRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PreparaMailJob {

    private final GdpLogRepository logRepository;
    private final GdpMailRepository mailRepository;

    public PreparaMailJob(GdpLogRepository logRepository, GdpMailRepository mailRepository) {
        this.logRepository = logRepository;
        this.mailRepository = mailRepository;
    }

    public MailComposition preparaMail(Integer idLog, String tipoMail) {
        MailComposition output = new MailComposition();

        // --- 1. CERCO IL DESTINATARIO ---
        // Vado a prendermi la mail dell'utente partendo dal log
        String email = logRepository.findEmailByLogId(idLog);

        if (email == null || email.isBlank()) {
            // Se non c'è la mail, non posso scrivere a nessuno. Chiudo qui.
            output.setEsito(GdpMessage.F14_MAIL_NOT_FOUND.getCodice()); // MSG00001
            return output;
        }
        output.setTo(email);

        // --- 2. RECUPERO I DATI DEL GIORNALE ---
        // Mi serve il nome della testata e la data dell'edizione per riempire la mail
        String nomeTestata = logRepository.findNomeTestataByLogId(idLog);
        String dataEdizioneStr = "";

        // Se non è un invio "storico" (ST), cerco la data dell'edizione
        if (tipoMail != null && !tipoMail.startsWith("ST")) {
            LocalDate dataEd = logRepository.findDataEdizioneByLogId(idLog);
            if (dataEd != null) {
                dataEdizioneStr = dataEd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        }

        // --- 3. PRENDO IL MODELLO DELLA MAIL ---
        // Cerco il template (oggetto, testo standard, server smtp) nel database
        GdpMail gdpMail = mailRepository.findById(tipoMail);

        if (gdpMail == null) {
            // Se non trovo nemmeno il modello della mail, l'invio è fallito in partenza
            output.setEsito(GdpMessage.F14_SEND_FAILED.getCodice()); // MSG00002
            return output;
        }

        // Riempio i dati del server e l'oggetto presi dal database
        output.setFrom(gdpMail.mittente);
        output.setHost(gdpMail.smtpMailHost);
        output.setPorta(gdpMail.smtpMailPorta);
        output.setOggetto(gdpMail.testoOggetto);

        // --- 4. ASSEMBLO IL TESTO FINALE ---
        // Prendo il testo con i "buchi" e ci metto dentro la data e il nome della testata
        String testoCorpo = gdpMail.testoMail;
        if (testoCorpo != null) {
            testoCorpo = testoCorpo.replace("<[dataED]>", dataEdizioneStr)
                    .replace("<[nomeTestata]>", nomeTestata != null ? nomeTestata : "");
        }
        output.setTesto(testoCorpo);

        // --- 5. TUTTO OK ---
        // Se sono arrivato qui, l'email è pronta per essere spedita
        output.setEsito(GdpMessage.F14_OK.getCodice()); // MSG00009

        return output;
    }
}