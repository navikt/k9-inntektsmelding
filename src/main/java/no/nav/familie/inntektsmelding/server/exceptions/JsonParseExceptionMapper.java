package no.nav.familie.inntektsmelding.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.exc.StreamReadException;

public class JsonParseExceptionMapper implements ExceptionMapper<StreamReadException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(StreamReadException exception) {
        var feil = String.format("FIM-299955: JSON-parsing feil: %s", exception.getMessage());
        LOG.warn(feil);
        return Response.status(Response.Status.BAD_REQUEST).entity(new FeilDto(feil)).type(MediaType.APPLICATION_JSON).build();
    }
}
