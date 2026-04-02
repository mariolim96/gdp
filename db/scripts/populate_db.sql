/* ---------------------------------------------------- */
/*  Script to populate mock data for local development  */
/* ---------------------------------------------------- */

-- 1. Populating GDP_UTENTEWEB
INSERT INTO GDP_UTENTEWEB (ID_GDP_UTENTEWEB, CODICE_FISCALE, COGNOME, NOME, RUOLO, EMAIL, DT_CREAZIONE) VALUES
(1, 'RSSMRA80A01H501U', 'Rossi', 'Mario', 'ADMIN', 'mario.rossi@example.com', CURRENT_DATE),
(2, 'BNCLUI85B12F205E', 'Bianchi', 'Luigi', 'USER', 'luigi.bianchi@example.com', CURRENT_DATE),
(3, 'VRDGVN75C14D304A', 'Verdi', 'Giovanni', 'USER', 'giovanni.verdi@example.com', CURRENT_DATE),
(4, 'NRRANN90D45F205C', 'Neri', 'Anna', 'EDITOR', 'anna.neri@example.com', CURRENT_DATE),
(5, 'GLLSRA88M50F205H', 'Gialli', 'Sara', 'USER', 'sara.gialli@example.com', CURRENT_DATE);

-- 2. Populating GDP_TESTATA
-- Note: COD_TEMA (1=Cronaca Locale, 2=PA, 3=Lavoro, 4=Cinema, 5=Cultura)
-- Note: PROVINCIA (TO, AL, AT, BI, CN, NO, VB, VC)
INSERT INTO GDP_TESTATA (ID_GDP_TESTATA, NOME_TESTATA, CARTELLA_TESTATA, INVIO_EDIZIONE, STATO, COD_TEMA, PROVINCIA, DESCRIZIONE) VALUES
(1, 'Gazzetta del Piemonte', 'gazzetta-del-piemonte', TRUE, 0, 1, 'TO', 'Quotidiano di cronaca locale di Torino e provincia'),
(2, 'Il Corriere di Torino', 'corriere-torino', TRUE, 0, 1, 'TO', 'Storico corriere di informazione piemontese'),
(3, 'Cuneo News', 'cuneo-news', TRUE, 0, 2, 'CN', 'Informazioni sulla pubblica amministrazione del cuneese'),
(4, 'Novara Lavoro', 'novara-lavoro', FALSE, 1, 3, 'NO', 'Edizione storica sulle opportunita lavorative a Novara'),
(5, 'Cinema Astigiano', 'cinema-astigiano', TRUE, 0, 4, 'AT', 'Tutto sul mondo del cinema nella provincia di Asti');

-- 3. Populating GDP_PERIODICITA
INSERT INTO GDP_PERIODICITA (ID_GDP_PERIODICITA, FK_GDP_TESTATA, MENSILITA, GG_PERIODICITA) VALUES
(1, 1, 12, '1,2,3,4,5,6,7'), -- Quotidiano
(2, 2, 12, '1,2,3,4,5,6,7'),
(3, 3, 12, '1,3,5'), -- Lunedi, Mercoledi, Venerdi
(4, 5, 12, '6'); -- Solo sabato

-- 4. Populating GDP_DATA_USCITA
-- Tomorrow depends on current date. We ensure tomorrow exists for testing F02.
INSERT INTO GDP_DATA_USCITA (ID_GDP_DATA_USCITA, FK_GDP_PERIODICITA, DT_INIZIO, DT_FINE, DATA_ATTESA, SOSPESA) VALUES
(1, 1, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '1 day', FALSE),
(2, 2, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '1 day', FALSE),
(3, 3, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '1 day', FALSE),
(4, 4, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '1 day', FALSE);

-- 5. Populating GDP_UTENTESFTP
-- HOME_SFTP adjusted to 'upload' for local Docker testing (atmoz/sftp mapped folder)
INSERT INTO GDP_UTENTESFTP (ID_GDP_UTENTESFTP, USERNAME, PASSWORD, HOME_SFTP, RIF_TESTATA, DIRETTORE, EMAIL, STATO) VALUES
(1, 'sftp_gazzetta', 'pass_gazzetta_123', 'upload', '1', 'Dir. Gazzetta', 'sftp@gazzetta.it', 'ATTIVO'),
(2, 'sftp_corriere', 'pass_corriere_123', 'upload', '2', 'Dir. Corriere', 'sftp@corriere.it', 'ATTIVO'),
(3, 'sftp_cuneo', 'pass_cuneo_123', 'upload', '3', 'Dir. Cuneo', 'sftp@cuneonews.it', 'ATTIVO'),
(4, 'sftp_cinema', 'pass_cinema_123', 'upload', '5', 'Dir. Cinema', 'sftp@cinema.it', 'ATTIVO');

-- 6. Populating GDP_LOG
INSERT INTO GDP_LOG (ID_GDP_LOG, FK_GDP_UTENTEFTP, FK_GDP_TESTATA, TIPO_ACQUISIZIONE, DT_ACQUISIZIONE, TOTALE_FILE_ACQUISITI, ESITO) VALUES
(1, 1, 1, 'G', CURRENT_TIMESTAMP, 2, 'OK'),
(2, 2, 2, 'G', CURRENT_TIMESTAMP, 1, 'OK'),
(3, 3, 3, 'G', CURRENT_TIMESTAMP, 1, 'ERROR');

-- 7. Populating GDP_EDIZIONE
INSERT INTO GDP_EDIZIONE (ID_GDP_EDIZIONE, FK_GDP_TESTATA, DATA_EDIZIONE, DATA_PUBBLICAZIONE, STATO, TOTALE_PAGINE) VALUES
(1, 1, CURRENT_DATE, CURRENT_DATE, 1, 24),
(2, 1, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE - INTERVAL '1 day', 1, 24),
(3, 2, CURRENT_DATE, CURRENT_DATE, 1, 16),
(4, 3, CURRENT_DATE, CURRENT_DATE, 1, 8),
(5, 5, CURRENT_DATE, CURRENT_DATE, 1, 32);

-- 8. Populating GDP_LOG_EDIZIONE
-- Note: TIPO_EDIZIONE (OK, SO, AN, PO, AA, ST, AS)
INSERT INTO GDP_LOG_EDIZIONE (ID_GDP_LOG_EDIZIONE, FK_GDP_LOG, NRO_PAG_ACQUISITE, TIPO_EDIZIONE, FK_GDP_EDIZIONE, PATH_EDIZIONE, NRO_PAG_VALIDE, NRO_PAG_ERRATE) VALUES
(1, 1, 24, 'OK', 1, '/edizioni/gazzetta/oggi/', 24, 0),
(2, 2, 16, 'OK', 3, '/edizioni/corriere/oggi/', 16, 0),
(3, 3, 8, 'AA', 4, '/edizioni/cuneo/oggi/', 7, 1);

-- 9. Populating GDP_CODA_CARICAMENTO
INSERT INTO GDP_CODA_CARICAMENTO (ID_GDP_CODA_CARICAMENTO, DT_INSERIM_IN_CODA, FK_GDP_LOG_EDIZIONE, NRO_TENTATIVO, SFTP_PATH, STATO) VALUES
(1, CURRENT_DATE, 1, 1, '/sftp/gazzetta_piemonte/oggi', 'COMPLETED'),
(2, CURRENT_DATE, 2, 1, '/sftp/corriere_torino/oggi', 'COMPLETED'),
(3, CURRENT_DATE, 3, 2, '/sftp/cuneo_news/oggi', 'FAILED');

-- 10. Populating GDP_PAGINA
INSERT INTO GDP_PAGINA (ID_GDP_PAGINA, FK_GDP_TESTATA, FK_GDP_EDIZIONE, NUM_PAGINA, FILE_PDF, FILE_TXT, ANNO_EDIZIONE, STATO) VALUES
(1, 1, 1, 1, 'gazzetta_pag01.pdf', 'gazzetta_pag01.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(2, 1, 1, 2, 'gazzetta_pag02.pdf', 'gazzetta_pag02.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(3, 1, 1, 3, 'gazzetta_pag03.pdf', 'gazzetta_pag03.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(4, 1, 1, 4, 'gazzetta_pag04.pdf', 'gazzetta_pag04.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(5, 1, 2, 1, 'gazzetta_ieri_pag01.pdf', 'gazzetta_ieri_pag01.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(6, 2, 3, 1, 'corriere_pag01.pdf', 'corriere_pag01.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(7, 2, 3, 2, 'corriere_pag02.pdf', 'corriere_pag02.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(8, 3, 4, 1, 'cuneo_pag01.pdf', 'cuneo_pag01.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0),
(9, 5, 5, 1, 'cinema_pag01.pdf', 'cinema_pag01.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0);

-- 11. Populating GDP_PREF_TESTATA
INSERT INTO GDP_PREF_TESTATA (ID_GDP_PREF_TESTATA, FK_GDP_UTENTEWEB, FK_GDP_TESTATA, NOME_TESTATA, DT_CREAZIONE) VALUES
(1, 2, 1, 'Gazzetta del Piemonte', CURRENT_DATE),
(2, 2, 5, 'Cinema Astigiano', CURRENT_DATE),
(3, 5, 2, 'Il Corriere di Torino', CURRENT_DATE);

-- 12. Populating GDP_PREF_EDIZIONE
INSERT INTO GDP_PREF_EDIZIONE (ID_GDP_PREF_EDIZIONE, FK_GDP_UTENTEWEB, FK_GDP_EDIZIONE, DT_PUBBLICAZIONE, DT_CREAZIONE) VALUES
(1, 2, 1, CURRENT_DATE, CURRENT_DATE),
(2, 5, 3, CURRENT_DATE, CURRENT_DATE);

-- 13. Populating GDP_FASCICOLO
INSERT INTO GDP_FASCICOLO (ID_GDP_FASCICOLO, FK_GDP_UTENTEWEB, TITOLO, NOTE_FASCICOLO, DT_MODIFICA) VALUES
(1, 2, 'Rassegna Stampa Lunedi', 'Articoli interessanti per rassegna settimanale.', CURRENT_DATE),
(2, 5, 'News Pubblica Amministrazione', 'Tutto cio che riguarda i bandi e concorsi.', CURRENT_DATE);

-- 14. Populating GDP_FASCICOLO_PAG
INSERT INTO GDP_FASCICOLO_PAG (ID_GDP_FASCICOLO_PAG, FK_GDP_FASCICOLO, FK_GDP_PAGINA, POSIZIONE, NOTE_PAGINA) VALUES
(1, 1, 1, 1, 'Notizia in prima pagina da salvare'),
(2, 1, 5, 2, 'Editoriale interessante di ieri'),
(3, 2, 8, 1, 'Bando PA pubblicato a Cuneo');


/* ---------------------------------------------------- */
/*  ADVANCED EDGE CASES                                 */
/* ---------------------------------------------------- */

-- 15. Edge Case: Right to Be Forgotten (Oblio)
-- Simulates a page that had PII removed.
INSERT INTO GDP_PAGINA (ID_GDP_PAGINA, FK_GDP_TESTATA, FK_GDP_EDIZIONE, NUM_PAGINA, FILE_PDF, FILE_TXT, ANNO_EDIZIONE, STATO, OBLIO, DATA_OBLIO, NOTA_OBLIO) VALUES
(10, 1, 2, 2, 'gazzetta_ieri_pag02.pdf', 'gazzetta_ieri_pag02.txt', EXTRACT(YEAR FROM CURRENT_DATE), 0, 'Nome e Cognome Oscurati', CURRENT_DATE - INTERVAL '10 days', 'Richiesta di deindicizzazione per articolo in fondo pagina.');

-- 16. Edge Case: Historical Archives (Acquisizione Storica per Testata ID 4)
INSERT INTO GDP_LOG (ID_GDP_LOG, FK_GDP_UTENTEFTP, FK_GDP_TESTATA, TIPO_ACQUISIZIONE, DT_ACQUISIZIONE, TOTALE_FILE_ACQUISITI, ESITO) VALUES
(4, 1, 4, 'S', CURRENT_DATE - INTERVAL '30 days', 100, 'OK - Bulk import archivio storico');

INSERT INTO GDP_EDIZIONE (ID_GDP_EDIZIONE, FK_GDP_TESTATA, DATA_EDIZIONE, DATA_PUBBLICAZIONE, STATO, TOTALE_PAGINE) VALUES
(6, 4, '2005-10-15', '2005-10-15', 1, 12);

INSERT INTO GDP_LOG_EDIZIONE (ID_GDP_LOG_EDIZIONE, FK_GDP_LOG, NRO_PAG_ACQUISITE, TIPO_EDIZIONE, FK_GDP_EDIZIONE, PATH_EDIZIONE, NRO_PAG_VALIDE, NRO_PAG_ERRATE) VALUES
(4, 4, 12, 'ST', 6, '/edizioni/novara_lavoro/storico/2005/', 12, 0);

INSERT INTO GDP_PAGINA (ID_GDP_PAGINA, FK_GDP_TESTATA, FK_GDP_EDIZIONE, NUM_PAGINA, FILE_PDF, FILE_TXT, ANNO_EDIZIONE, STATO) VALUES
(11, 4, 6, 1, 'novara_2005_pag01.pdf', 'novara_2005_pag01.txt', 2005, 1);

-- 17. Edge Case: Publication Suspensions (Sospensione per Testata ID 5 - Cinema Astigiano)
UPDATE GDP_PERIODICITA 
SET INIZIO_SOSPENSIONE = CURRENT_DATE + INTERVAL '10 days',
    FINE_SOSPENSIONE = CURRENT_DATE + INTERVAL '25 days'
WHERE ID_GDP_PERIODICITA = 4;

INSERT INTO GDP_DATA_USCITA (ID_GDP_DATA_USCITA, FK_GDP_PERIODICITA, DT_INIZIO, DT_FINE, DATA_ATTESA, SOSPESA) VALUES
(5, 4, CURRENT_DATE + INTERVAL '10 days', CURRENT_DATE + INTERVAL '25 days', CURRENT_DATE + INTERVAL '15 days', TRUE);

-- 18. Edge Case: Failed / Deactivated Users
-- A canceled web user.
INSERT INTO GDP_UTENTEWEB (ID_GDP_UTENTEWEB, CODICE_FISCALE, COGNOME, NOME, RUOLO, EMAIL, DT_CREAZIONE, DT_ANNULLAMENTO) VALUES
(6, 'BNCMRS80A01H501A', 'Neri', 'Mario', 'USER', 'mario.neri@deactivated.com', CURRENT_DATE - INTERVAL '1 year', CURRENT_DATE - INTERVAL '1 month');

-- A deactivated SFTP user.
INSERT INTO GDP_UTENTESFTP (ID_GDP_UTENTESFTP, USERNAME, PASSWORD, HOME_SFTP, RIF_TESTATA, STATO) VALUES
(5, 'sftp_old_vendor', 'pass_old', 'old_vendor', '1', 'INATTIVO');
