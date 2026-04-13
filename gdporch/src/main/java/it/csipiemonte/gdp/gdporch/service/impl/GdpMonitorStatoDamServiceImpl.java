package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.service.GdpMonitorStatoDamService;
import it.csipiemonte.gdp.gdporch.service.GdporchRestClient;
import it.csipiemonte.gdp.gdporch.dto.DamStatusResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GdpMonitorStatoDamServiceImpl implements GdpMonitorStatoDamService {

    private static final Logger LOG = LoggerFactory.getLogger(GdpMonitorStatoDamServiceImpl.class);

    @RestClient
    GdporchRestClient gdporchRestClient;

    @Override
    public String recuperaStatoDam(Long idLog) {
        try {
            DamStatusResponse response = gdporchRestClient.getStatoLibra(String.valueOf(idLog));

            if (response != null && response.getStatus() != null) {
                return "MSG00009 Stato edizione " + response.getStatus();
            }

            return "MSG00001 Dato non trovato";
        } catch (Exception e) {
            LOG.error("F20 Failure - idLog: {}, error: {}", idLog, e.getMessage());
            return "MSG00001 Dato non trovato";
        }
    }
}