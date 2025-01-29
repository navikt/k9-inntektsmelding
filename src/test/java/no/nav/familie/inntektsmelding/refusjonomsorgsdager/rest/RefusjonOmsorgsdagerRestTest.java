package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.InnloggetBrukerTjeneste;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerRestTest {
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjenesteMock;
    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private InnloggetBrukerTjeneste innloggetBrukerTjenesteMock;

    private RefusjonOmsorgsdagerRest rest;

    @BeforeEach
    void setUp() {
        rest = new RefusjonOmsorgsdagerRest(arbeidstakerTjenesteMock, personTjenesteMock, innloggetBrukerTjenesteMock);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var førsteFraværsdag = LocalDate.now();
        var dto = new SlåOppArbeidstakerRequestDto(fnr, Ytelsetype.OMSORGSPENGER);
        var arbeidsforhold = List.of(new ArbeidsforholdDto("999999999", "ARB-1"));
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto(new SlåOppArbeidstakerResponseDto.Personinformasjon("fornavn", "mellomnavn", "etternavn"), arbeidsforhold);

        when(personTjenesteMock.hentPersonFraIdent(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(new PersonInfo("fornavn", "mellomnavn", "etternavn", fnr, null, LocalDate.now(), null));
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fnr, førsteFraværsdag)).thenReturn(arbeidsforhold);

        var response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(arbeidstakerInfo, response.getEntity());
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fnr, førsteFraværsdag);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_not_found_når_arbeidstaker_ikke_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var dto = new SlåOppArbeidstakerRequestDto(fnr, Ytelsetype.OMSORGSPENGER);
        var førsteFraværsdag = LocalDate.now();

        when(personTjenesteMock.hentPersonFraIdent(fnr, Ytelsetype.OMSORGSPENGER)).thenReturn(null);

        var response = rest.slåOppArbeidstaker(dto);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fnr, førsteFraværsdag);
    }

    @Test
    void hent_innlogget_bruker_returnerer_ok() {
        var innloggetBruker = new InnloggetBrukerDto("fornavn", "mellomnavn", "etternavn", "81549300", "123456789", "organisasjonsnavn");

        when(innloggetBrukerTjenesteMock.hentInnloggetBruker(any(), any())).thenReturn(innloggetBruker);

        var response = rest.hentInnloggetBruker(new HentInnloggetBrukerRequestDto(Ytelsetype.OMSORGSPENGER, "123456789"));
        assertEquals(response.getEntity(), innloggetBruker);
    }
}
