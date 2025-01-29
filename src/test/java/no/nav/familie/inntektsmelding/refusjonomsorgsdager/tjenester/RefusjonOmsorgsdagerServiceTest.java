package no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.InnloggetBrukerDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.SlåOppArbeidstakerResponseDto;

@ExtendWith(MockitoExtension.class)
class RefusjonOmsorgsdagerServiceTest {
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjenesteMock;
    @Mock
    private PersonTjeneste personTjenesteMock;

    @Mock
    private InntektTjeneste inntektTjenesteMock;

    @Mock
    private InnloggetBrukerTjeneste innloggetBrukerTjenesteMock;

    private RefusjonOmsorgsdagerService service;

    @BeforeEach
    void setUp() {
        service = new RefusjonOmsorgsdagerService(
            arbeidstakerTjenesteMock,
            personTjenesteMock,
            inntektTjenesteMock,
            innloggetBrukerTjenesteMock
        );
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_ok_response_når_arbeidstaker_finnes() {
        var fødselsnummer = PersonIdent.fra("12345678910");
        var førsteFraværsdag = LocalDate.now();
        var arbeidsforhold = List.of(new ArbeidsforholdDto("999999999", "ARB-1"));
        var arbeidstakerInfo = new SlåOppArbeidstakerResponseDto(new SlåOppArbeidstakerResponseDto.Personinformasjon("fornavn", "mellomnavn", "etternavn", "12345678910"), arbeidsforhold);

        when(personTjenesteMock.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER)).thenReturn(new PersonInfo("fornavn", "mellomnavn", "etternavn", fødselsnummer, null, LocalDate.now(), null));
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, førsteFraværsdag)).thenReturn(arbeidsforhold);

        var response = service.hentArbeidstaker(fødselsnummer);

        assertEquals(arbeidstakerInfo, response);
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, førsteFraværsdag);
    }

    @Test
    void slå_opp_arbeidstaker_skal_returnere_null_når_arbeidstaker_ikke_finnes() {
        var fødselsnummer = PersonIdent.fra("12345678910");
        var førsteFraværsdag = LocalDate.now();

        when(personTjenesteMock.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER)).thenReturn(null);

        var response = service.hentArbeidstaker(fødselsnummer);

        assertNull(response);
        verify(arbeidstakerTjenesteMock).finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, førsteFraværsdag);
    }

    @Test
    void hent_innlogget_bruker_returnerer_ok() {
        var innloggetBruker = new InnloggetBrukerDto("fornavn", "mellomnavn", "etternavn", "81549300", "123456789", "organisasjonsnavn");

        when(innloggetBrukerTjenesteMock.hentInnloggetBruker(any(), any())).thenReturn(innloggetBruker);

        var response = service.hentInnloggetBruker("123456789");
        assertEquals(response, innloggetBruker);
    }

    @Test
    void hent_inntektsopplysninger_returnerer_ok() {
        var fødselsnummer = PersonIdent.fra("12345678910");
        var organisasjonsnummer = "999999999";

        when(personTjenesteMock.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER)).thenReturn(new PersonInfo("fornavn",
            "mellomnavn",
            "etternavn",
            fødselsnummer,
            null,
            LocalDate.now(),
            null));
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer,
            LocalDate.now())).thenReturn(List.of(new ArbeidsforholdDto(organisasjonsnummer, "ARB-1")));
        when(inntektTjenesteMock.hentInntekt(any(), any(), any(), any())).thenReturn(new Inntektsopplysninger(new BigDecimal(10000),
            organisasjonsnummer,
            List.of()));

        var response = service.hentInntektsopplysninger(fødselsnummer, "999999999", LocalDate.now());

        assertEquals(new Inntektsopplysninger(new BigDecimal(10000), organisasjonsnummer, List.of()), response);
    }

    @Test
    void hent_inntektsopplysninger_returnerer_null_om_man_ikke_finner_noen_arbeidsforhold() {
        var fødselsnummer = PersonIdent.fra("12345678910");
        var organisasjonsnummer = "999999999";

        when(personTjenesteMock.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER)).thenReturn(new PersonInfo("fornavn",
            "mellomnavn",
            "etternavn",
            fødselsnummer,
            null,
            LocalDate.now(),
            null));
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer,
            LocalDate.now())).thenReturn(List.of());

        var response = service.hentInntektsopplysninger(fødselsnummer, organisasjonsnummer, LocalDate.now());

        assertNull(response);
    }

    @Test
    void hent_inntektsopplysninger_returnerer_null_om_person_ikke_finnes() {
        var fødselsnummer = PersonIdent.fra("12345678910");
        var organisasjonsnummer = "999999999";

        when(personTjenesteMock.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER)).thenReturn(null);
        when(arbeidstakerTjenesteMock.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer,
            LocalDate.now())).thenReturn(List.of(new ArbeidsforholdDto(organisasjonsnummer, "ARB-1")));

        var response = service.hentInntektsopplysninger(fødselsnummer, "999999999", LocalDate.now());

        assertNull(response);
    }
}
