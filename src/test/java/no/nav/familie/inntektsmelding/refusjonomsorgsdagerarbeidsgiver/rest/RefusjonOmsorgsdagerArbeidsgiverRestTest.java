package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.InnloggetBrukerTjeneste;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerArbeidsgiverRestTest {
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjenesteMock;
    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private InnloggetBrukerTjeneste innloggetBrukerTjenesteMock;

    private RefusjonOmsorgsdagerArbeidsgiverRest rest;

    @BeforeEach
    void setUp() {
        rest = new RefusjonOmsorgsdagerArbeidsgiverRest(arbeidstakerTjenesteMock, personTjenesteMock);
        rest = new RefusjonOmsorgsdagerArbeidsgiverRest(arbeidstakerTjenesteMock, innloggetBrukerTjenesteMock);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerRequestDto(fnr, Ytelsetype.OMSORGSPENGER);
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto("fornavn", "mellomnavn", "etternavn", null);
        var dto = new SlåOppArbeidstakerDto(fnr, Ytelsetype.OMSORGSPENGER);
        var arbeidsforhold = List.of(new ArbeidsforholdDto("999999999", "ARB-1"));
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto("fornavn", "mellomnavn", "etternavn", arbeidsforhold);

        when(personTjenesteMock.hentPersonFraIdent(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(new PersonInfo("fornavn", "mellomnavn", "etternavn", fnr, null, LocalDate.now(), null));
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fnr)).thenReturn(arbeidsforhold);

        var response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(arbeidstakerInfo, response.getEntity());
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fnr);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_not_found_når_arbeidstaker_ikke_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerRequestDto(fnr, Ytelsetype.OMSORGSPENGER);

        when(personTjenesteMock.hentPersonFraIdent(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(null);

        var response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fnr);
    }

    @Test
    void hent_innlogget_bruker_returnerer_ok() {
        var innloggetBruker = new InnloggetBrukerDto("fornavn", "mellomnavn", "etternavn", "81549300", "123456789", "organisasjonsnavn");

        when(innloggetBrukerTjenesteMock.hentInnloggetBruker(any(), any())).thenReturn(innloggetBruker);

        var response = rest.hentInnloggetBruker(new HentInnloggetBrukerRequestDto(Ytelsetype.OMSORGSPENGER, "123456789"));
        assertEquals(response.getEntity(), innloggetBruker);
    }
}
