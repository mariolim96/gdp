package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

@ApplicationScoped
public class VerifDateAtteseService {


    public DateAtteseList recuperaDateAttese(Integer idTestata, LocalDate dataInizio, LocalDate dataFine) {
        return  new DateAtteseList();
    }
}
