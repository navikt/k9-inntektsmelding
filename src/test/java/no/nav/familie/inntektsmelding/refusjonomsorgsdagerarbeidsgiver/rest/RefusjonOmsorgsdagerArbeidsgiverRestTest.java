package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.ArbeidstakerTjeneste;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerArbeidsgiverRestTest {
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjenesteMock;

    private RefusjonOmsorgsdagerArbeidsgiverRest rest;

    @BeforeEach
    void setUp() {
        rest = new RefusjonOmsorgsdagerArbeidsgiverRest(arbeidstakerTjenesteMock);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerDto(fnr, Ytelsetype.OMSORGSPENGER);
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto("fornavn", "mellomnavn", "etternavn", null);

        when(arbeidstakerTjenesteMock.slåOppArbeidstaker(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(arbeidstakerInfo);

        Response response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(arbeidstakerInfo, response.getEntity());
        verify(arbeidstakerTjenesteMock).slåOppArbeidstaker(fnr, Ytelsetype.OMSORGSPENGER);
    }

    @Test
    void slå_opp_arbeidstaker_skal_kaste_not_found_exception_når_arbeidstaker_ikke_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerDto(fnr, Ytelsetype.OMSORGSPENGER);

        when(arbeidstakerTjenesteMock.slåOppArbeidstaker(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> rest.slåOppArbeidstaker(dto));
        verify(arbeidstakerTjenesteMock).slåOppArbeidstaker(fnr, Ytelsetype.OMSORGSPENGER);
    }
}
