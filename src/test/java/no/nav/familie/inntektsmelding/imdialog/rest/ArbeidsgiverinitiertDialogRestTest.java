package no.nav.familie.inntektsmelding.imdialog.rest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.tjenester.ArbeidsgiverinitiertDialogRestValiderer;
import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
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
    void hent_opplysninger_uregistrert_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = opplysningerRequest();

        assertThrows(ManglerTilgangException.class, () -> rest.hentOpplysningerUregistrertV2(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste);
    }

    @Test
    void hent_arbeidsforhold_nyansatt_v2_kaster_når_arbeidsgiver_mangler_tilgang() {
        manglerTilgang();
        var request = arbeidsforholdV2Request();

        assertThrows(ManglerTilgangException.class, () -> rest.hentArbeidsforholdNyansattV2(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste, validerer);
    }

    @Test
    void hent_arbeidstaker_er_ikke_implementert() {
        var request = new HentArbeidstakerRequest(FNR);

        assertThrows(NotImplementedException.class, () -> rest.hentArbeidstaker(request));
        verifyNoInteractions(grunnlagTjeneste, personTjeneste, validerer, tilgang);
    }
}
