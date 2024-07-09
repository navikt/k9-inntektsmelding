package no.nav.familie.inntektsmelding.server;

<<<<<<< Updated upstream
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

=======
>>>>>>> Stashed changes
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

<<<<<<< Updated upstream
    public static final String API_URI ="/internal";
=======
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends ResourceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(InternalApiConfig.class);
    public static final String API_URI = "/internal";
>>>>>>> Stashed changes

    public InternalApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        registerClasses(getApplicationClasses());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

<<<<<<< Updated upstream
    @Override
    public Set<Class<?>> getClasses() {
=======
    private Set<Class<?>> getApplicationClasses() {
>>>>>>> Stashed changes
        return Set.of(HealtCheckRest.class, PrometheusRestService.class);
    }

}
