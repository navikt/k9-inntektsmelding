package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.RefusjonOmsorgsdagerService;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerRestTest {

    @Mock
    private RefusjonOmsorgsdagerService refusjonOmsorgsdagerServiceMock;

    private RefusjonOmsorgsdagerRest rest;

    @BeforeEach
    void setUp() {
        rest = new RefusjonOmsorgsdagerRest(refusjonOmsorgsdagerServiceMock);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var request = new SlåOppArbeidstakerRequest(fnr, Ytelsetype.OMSORGSPENGER);
        var arbeidsforhold = List.of(new SlåOppArbeidstakerResponse.ArbeidsforholdDto("999999999", "Arbeidsgiver AS"));
        var arbeidstakerInfo = new SlåOppArbeidstakerResponse(new SlåOppArbeidstakerResponse.Personinformasjon("fornavn", "mellomnavn", "etternavn", "10107400090", "12345"), arbeidsforhold);

        when(refusjonOmsorgsdagerServiceMock.hentArbeidstaker(fnr)).thenReturn(arbeidstakerInfo);

        var response = rest.slåOppArbeidstaker(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(arbeidstakerInfo, response.getEntity());
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_not_found_når_arbeidstaker_ikke_finnes() {
        var fnr = PersonIdent.fra("12345678910");
        var request = new SlåOppArbeidstakerRequest(fnr, Ytelsetype.OMSORGSPENGER);

        when(refusjonOmsorgsdagerServiceMock.hentArbeidstaker(fnr)).thenReturn(null);

        var response = rest.slåOppArbeidstaker(request);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void hent_innlogget_bruker_returnerer_ok() {
        var innloggetBruker = new HentInnloggetBrukerResponse("fornavn", "mellomnavn", "etternavn", "81549300", "123456789", "organisasjonsnavn");

        when(refusjonOmsorgsdagerServiceMock.hentInnloggetBruker(any())).thenReturn(innloggetBruker);

        var response = rest.hentInnloggetBruker(new HentInnloggetBrukerRequest(Ytelsetype.OMSORGSPENGER, "123456789"));
        assertEquals(response.getEntity(), innloggetBruker);
    }

    @Test
    void hent_inntektsopplysninger_skal_returnere_ok_response_når_service_returnerer_noe() {
        var personIdent = PersonIdent.fra("12345678910");
        var organisasjonsnummer = "999999999";
        var skjæringstidspunkt = LocalDate.parse("2025-01-01");

        var inntektsopplysninger = new HentInntektsopplysningerResponse(
            new BigDecimal(100000),
            List.of(new HentInntektsopplysningerResponse.MånedsinntektDto(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 2, 1),
                new BigDecimal(100000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT
            ))
        );
        when(refusjonOmsorgsdagerServiceMock.hentInntektsopplysninger(personIdent, organisasjonsnummer, skjæringstidspunkt)).thenReturn(inntektsopplysninger);

        var response = rest.hentInntektsopplysninger(new HentInntektsopplysningerRequest(personIdent, organisasjonsnummer, "2025-01-01"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(inntektsopplysninger, response.getEntity());
    }

    @Test
    void hent_inntektsopplysninger_skal_returnere_not_found_når_servicen_returnerer_null() {
        var personIdent = PersonIdent.fra("12345678910");
        var organisasjonsnummer = "999999999";
        var skjæringstidspunkt = LocalDate.parse("2025-01-01");

        when(refusjonOmsorgsdagerServiceMock.hentInntektsopplysninger(personIdent, organisasjonsnummer, skjæringstidspunkt)).thenReturn(null);

        var response = rest.hentInntektsopplysninger(new HentInntektsopplysningerRequest(personIdent, organisasjonsnummer, "2025-01-01"));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getEntity());
    }


}
