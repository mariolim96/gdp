package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.BoApi;
import it.csipiemonte.gdp.gdporch.dto.*;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;

public class BoApiDelegate implements BoApi {
    @Override
    public Response getBoAcquisizioneDetail(Long idLog) {
        return null;
    }

    @Override
    public Response getBoAcquisizioneEdizioniStoriche(Long idLog) {
        return null;
    }

    @Override
    public Response getBoAcquisizioneFile(Long idLog) {
        return null;
    }

    @Override
    public Response getBoAcquisizioneStatoDam(Long idLog) {
        return null;
    }

    @Override
    public Response getBoAcquisizioni(LocalDate data, String tipo) {
        return null;
    }

    @Override
    public Response getBoAcquisizioniRicerca(String tipoAcquisizione, LocalDate dataA, TipoEdizione tipoEdizione, Long idTestata, LocalDate dataDA) {
        return null;
    }

    @Override
    public Response getBoDateAttese(LocalDate dataInizio, LocalDate dataFine, Long idTestata) {
        return null;
    }

    @Override
    public Response getBoOblioStatus(Long idPagina) {
        return null;
    }

    @Override
    public Response getBoPeriodicita(Long idTestata) {
        return null;
    }

    @Override
    public Response getBoSospensioni(LocalDate dataInizio, LocalDate dataFine, Long idTestata) {
        return null;
    }

    @Override
    public Response getBoTestataById(Long idTestata) {
        return null;
    }

    @Override
    public Response getBoTestate(Boolean invioEdizione, String prov, Long idTestata) {
        return null;
    }

    @Override
    public Response getBoUtenteSftpById(Long id) {
        return null;
    }

    @Override
    public Response getBoUtentiSftp(String stato) {
        return null;
    }

    @Override
    public Response postBoAcquisizioneMailInvia(Long idLog, MailComposition mailComposition) {
        return null;
    }

    @Override
    public Response postBoAcquisizioneMailPrepara(Long idLog, PostBoAcquisizioneMailPreparaRequest postBoAcquisizioneMailPreparaRequest) {
        return null;
    }

    @Override
    public Response postBoAcquisizioneRetry(Long idLog) {
        return null;
    }

    @Override
    public Response postBoDateAttese(Long idTestata, DateRangeRequest dateRangeRequest) {
        return null;
    }

    @Override
    public Response postBoOblio(Long idPagina, OblioRequest oblioRequest) {
        return null;
    }

    @Override
    public Response postBoSospensioni(Long idTestata, DateRangeRequest dateRangeRequest) {
        return null;
    }

    @Override
    public Response postBoUtenteSftp(UtenteSftpRequest utenteSftpRequest) {
        return null;
    }

    @Override
    public Response putBoPeriodicita(Long idTestata, Periodicita periodicita) {
        return null;
    }

    @Override
    public Response putBoTestata(Long idTestata, TestataDetail testataDetail) {
        return null;
    }

    @Override
    public Response putBoUtenteSftp(Long id, UtenteSftpRequest utenteSftpRequest) {
        return null;
    }
}
