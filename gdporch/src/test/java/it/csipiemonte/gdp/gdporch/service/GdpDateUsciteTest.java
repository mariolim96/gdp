package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class GdpDateUsciteTest {

    @Inject
    GdpDataUscitaService service;

    @Inject
    GdpDataUscitaRepository  repository;

    @Inject
    GdpTestataRepository testataRepository;

    @Inject
    GdpPeriodicitaRepository repositoryPeriodicita;

    private GdpPeriodicita createPeriodicita(Integer mensilita, String ggPeriodicita) {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setMensilita(mensilita);
        p.setGgPeriodicita(ggPeriodicita);
        return p;
    }

    /**
     * TEST CASO A: Uscite multiple nello stesso mese (Separatore ;)
     * Verifica che G01;G15 produca correttamente due date al mese.
     */
    @Test
    void testCasoA_UsciteMultipleMese() {
        GdpPeriodicita p = createPeriodicita(1, "G01;G15");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 30);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        result.forEach(System.out::println);
        assertEquals(6, result.size());
        assertTrue(result.contains(LocalDate.of(2026, 1, 1)));
        assertTrue(result.contains(LocalDate.of(2026, 1, 15)));
    }

    /**
     * TEST CASO A: Valore di default G00
     * Secondo specifica, G00 deve essere interpretato come il primo del mese.
     */
    @Test
    void testCasoA_DefaultG00() {
        GdpPeriodicita p = createPeriodicita(2, "G00");
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 12, 28);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        result.forEach(System.out::println);
        assertEquals(5, result.size());
        assertEquals(LocalDate.of(2026, 3, 1), result.get(0));
    }

    @Test
    void testCasoA_DefaultG01() {
        GdpPeriodicita p = createPeriodicita(1, "G01");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        result.forEach(System.out::println);
        assertEquals(6, result.size());
        assertEquals(LocalDate.of(2026, 1, 1), result.get(0));
    }
    /**
     * TEST CASO A: Gestione mesi corti (Febbraio)
     * Verifica che G31 su Febbraio non causi errori e si attesti all'ultimo giorno utile.
     */
    @Test
    void testCasoA_GiornoInesistenteMese() {
        GdpPeriodicita p = createPeriodicita(1, "G31");
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 2, 28), result.get(0));
    }

    /**
     * TEST CASO B: Uscite multiple settimanali (Separatore ;)
     * Verifica 1WS1;1WS4 (Lunedì e Giovedì di ogni settimana).
     */
    @Test
    void testCasoB_UsciteMultipleSettimana() {
        GdpPeriodicita p = createPeriodicita(0, "1WS1;1WS4");
        // Gennaio 2026: Lunedì sono 5, 12, 19, 26. Giovedì sono 1, 8, 15, 22, 29.
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        result.forEach(System.out::println);
        // 4 lunedì + 5 giovedì = 9 date
        assertEquals(26, result.size());
        assertTrue(result.contains(LocalDate.of(2026, 1, 5))); // Primo lunedì
        assertTrue(result.contains(LocalDate.of(2026, 1, 1))); // Primo giovedì
    }

    /**
     * TEST CASO B: Quotidiano 1WS0
     * Verifica che produca tutti i giorni nel range.
     */
    @Test
    void testCasoB_Quotidiano() {
        GdpPeriodicita p = createPeriodicita(0, "1WS0");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 10);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        assertEquals(10, result.size());
    }

    /**
     * TEST ECCEZIONI: Periodicità nulla o incompleta
     * Verifica che il servizio sollevi IllegalArgumentException.
     */
    @Test
    void testEccezioni_ParametriNulli() {
        GdpPeriodicita pNull = createPeriodicita(null, null);
        assertThrows(IllegalArgumentException.class, () ->
                service.calcolaDateUscite(pNull, LocalDate.now(), LocalDate.now().plusDays(1))
        );

        GdpPeriodicita pBlank = createPeriodicita(1, "  ");
        assertThrows(IllegalArgumentException.class, () ->
                service.calcolaDateUscite(pBlank, LocalDate.now(), LocalDate.now().plusDays(1))
        );
    }

    /**
     * TEST CASO A: GnS0 (Settimana completa del mese)
     * Verifica l'estensione per la prima settimana completa del mese.
     */
    @Test
    void testCasoA_SettimanaCompletaMese() {
        GdpPeriodicita p = createPeriodicita(1, "G1S0");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        // Gennaio 2026 inizia di Giovedì.
        // La logica GnS0 del tuo servizio prende i giorni del mese che appartengono alla settimana del 1° Gennaio.
        // Giovedì 1, Venerdì 2, Sabato 3, Domenica 4.
        assertFalse(result.isEmpty());
        assertTrue(result.contains(LocalDate.of(2026, 1, 1)));
        assertTrue(result.contains(LocalDate.of(2026, 1, 4)));
        assertFalse(result.contains(LocalDate.of(2026, 1, 5))); // Appartiene alla G2S0
    }

    /**
     * TEST RANGE: Date fuori intervallo
     * Verifica che non vengano restituite date precedenti a start o successive a end.
     */
    @Test
    void testRange_LimitiInizioFine() {
        GdpPeriodicita p = createPeriodicita(1, "G15");
        LocalDate start = LocalDate.of(2026, 1, 16); // Salta il 15 Gennaio
        LocalDate end = LocalDate.of(2026, 2, 14);   // Salta il 15 Febbraio
        List<LocalDate> result = service.calcolaDateUscite(p, start, end);
        assertTrue(result.isEmpty(), "Il risultato dovrebbe essere vuoto perché le uscite cadono fuori dal range");
    }

    @Test
    @TestTransaction
    void f01_u01_weeklyWednesday() {
        GdpTestata testa =  new GdpTestata();
        testa.setNomeTestata("Testata_Prova");
        testa.setCartellaTestata("test-mercoledi");
        testa.setProvincia("TO");
        testa.setInvioEdizione(true);
        testa.setStato(1);
        testa.setCodTema(1);
        testataRepository.persist(testa);
        Integer idTestata = testa.getId();
        GdpPeriodicita p =new GdpPeriodicita();
        p.setGgPeriodicita("1WS3");
        p.setMensilita(0);
        p.setFkGdpTestata(idTestata);
        repositoryPeriodicita.persist(p);
        repositoryPeriodicita.flush();
        Integer idValido = p.getId();
        LocalDate inizio= LocalDate.of(2026, 1, 1);
        LocalDate fine = LocalDate.of(2026, 1, 31);

        // ESECUZIONE: Chiamata al servizio F01
        var result = service.calcolaDateUscite(
                p, inizio, fine
        );
        if (result != null) {
            result.forEach(dataUscita -> {
                GdpDataUscita entita = new GdpDataUscita();
                entita.setDataAttesa(dataUscita); // Assumo che il campo si chiami così
                entita.setDataInizio(dataUscita);
                entita.setDataFine(dataUscita);
                entita.setFkGdpPeriodicita(idValido);
                entita.setSospesa(false);
                // Aggiungi altri setter obbligatori se necessario
                repository.persist(entita);
            });
        }
        // VERIFICA ESITO
        //assertThat(result.getEsito()).isEqualTo("MSG00009");
        repository.flush();

        // VERIFICA DATABASE
        // Gennaio 2026 ha mercoledì nei giorni: 7, 14, 21, 28
        var dates = repository.find("fkGdpPeriodicita = ?1 and dataAttesa between ?2 and ?3", idValido, inizio,fine).list();
        dates.forEach(d -> System.out.println("DATA NEL DB: " + d.getDataAttesa()));
        assertThat(dates).hasSize(4);
        assertThat(dates).allMatch(d -> d.getDataAttesa().getDayOfWeek() == DayOfWeek.WEDNESDAY);
        assertThat(dates).allMatch(d -> !d.getSospesa());
    }



}