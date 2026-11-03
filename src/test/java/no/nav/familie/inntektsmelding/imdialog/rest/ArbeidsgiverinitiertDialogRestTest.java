package no.nav.familie.inntektsmelding.imdialog.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.tjenester.ArbeidsgiverinitiertDialogRestValiderer;
import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.Kjønn;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.ManglerTilgangException;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverinitiertDialogRestTest {

    private static final PersonIdent FNR = PersonIdent.fra("12345678910");
    private static final OrganisasjonsnummerDto ORGNR = new OrganisasjonsnummerDto("999999999");
    private static final LocalDate FØRSTE_FRAVÆRSDAG = LocalDate.now();

    @Mock
    private GrunnlagTjeneste grunnlagTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ArbeidsgiverinitiertDialogRestValiderer validerer;
    @Mock
    private Tilgang tilgang;

    private ArbeidsgiverinitiertDialogRest rest;

    @BeforeEach
    void setUp() {
        rest = new ArbeidsgiverinitiertDialogRest(grunnlagTjeneste, personTjeneste, validerer, tilgang);
    }

    private void manglerTilgang() {
        doThrow(new ManglerTilgangException("IM-00403", "Mangler tilgang")).when(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ORGNR);
    }

    private OpplysningerRequestDto opplysningerRequest() {
        return new OpplysningerRequestDto(FNR, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, FØRSTE_FRAVÆRSDAG, ORGNR);
    }

    private HentArbeidsforholdV2Request arbeidsforholdV2Request() {
        return new HentArbeidsforholdV2Request(FNR, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, FØRSTE_FRAVÆRSDAG, ORGNR);
    }

    @Test
    void hent_opplysninger_nyansatt_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = opplysningerRequest();

        assertThrows(ManglerTilgangException.class, () -> rest.hentOpplysningerNyansatt(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste);
    }

    @Test
    void hent_opplysninger_uregistrert_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = opplysningerRequest();

        assertThrows(ManglerTilgangException.class, () -> rest.hentOpplysningerUregistrert(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste);
    }

    @Test
    void hent_opplysninger_uregistrert_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = opplysningerRequest();

        assertThrows(ManglerTilgangException.class, () -> rest.hentOpplysningerUregistrertV2(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste);
    }

    @Test
    void hent_arbeidstaker_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = new HentArbeidstakerV2Request(FNR, ORGNR);

        assertThrows(ManglerTilgangException.class, () -> rest.hentArbeidstakerV2(request));
        verifyNoInteractions(personTjeneste);
    }

    @Test
    void hent_arbeidstaker_v2_sjekker_tilgang_og_returnerer_arbeidstaker() {
        var personInfo = new PersonInfo("fornavn", null, "etternavn", FNR, new AktørIdEntitet("1234567891234"), LocalDate.now(), null, Kjønn.KVINNE);
        when(personTjeneste.hentPersonFraIdent(FNR)).thenReturn(personInfo);

        var response = rest.hentArbeidstakerV2(new HentArbeidstakerV2Request(FNR, ORGNR));

        verify(tilgang).sjekkAtArbeidsgiverHarTilgangTilBedrift(ORGNR);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new HentArbeidstakerResponse("fornavn", null, "etternavn", Kjønn.KVINNE, "1234567891234"), response.getEntity());
    }

    @Test
    void hent_arbeidsforhold_nyansatt_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = arbeidsforholdV2Request();

        assertThrows(ManglerTilgangException.class, () -> rest.hentArbeidsforholdNyansattV2(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste, validerer);
    }

    @Test
    void hent_arbeidsgivere_uregistrert_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = arbeidsforholdV2Request();

        assertThrows(ManglerTilgangException.class, () -> rest.hentArbeidsgivereUregistrertV2(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste, validerer);
    }
}
