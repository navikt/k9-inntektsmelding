package no.nav.familie.inntektsmelding.server;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends ResourceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(InternalApiConfig.class);
    public static final String API_URI = "/internal";

    public InternalApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        registerClasses(getApplicationClasses());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(HealtCheckRest.class, PrometheusRestService.class);
    }

}
