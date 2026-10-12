package no.nav.familie.inntektsmelding.server.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamReadException;

class JsonParseExceptionMapperTest {

    @Test
    void skal_mappe_StreamReadException() {
        var mapper = new JsonParseExceptionMapper();
        var resultat = mapper.toResponse(new StreamReadException("Ugyldig json"));
        assertThat(resultat.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        var dto = (FeilDto) resultat.getEntity();
        assertThat(dto.feilmelding()).startsWith("FIM-299955: JSON-parsing feil: Ugyldig json");
    }

}
