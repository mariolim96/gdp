package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class MockBeansTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of();
    }
}
