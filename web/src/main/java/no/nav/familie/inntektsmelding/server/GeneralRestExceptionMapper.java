package no.nav.familie.inntektsmelding.server;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        /* if (exception instanceof ManglerTilgangException manglerTilgangException) {
            return Response.status(manglerTilgangException.getStatusCode())
                .entity(problemDetails(manglerTilgangException))
                .build();
        } */
        LOG.warn("Fikk uventet feil: {}", exception.getMessage(), exception);
        return Response.status(500).build();
    }

    /* static ProblemDetails problemDetails(FpoversiktException exception) {
        return new ProblemDetails(exception.getFeilKode(), exception.getStatusCode().getStatusCode(), exception.getFeilKode().getBeskrivelse());
    }*/
}
