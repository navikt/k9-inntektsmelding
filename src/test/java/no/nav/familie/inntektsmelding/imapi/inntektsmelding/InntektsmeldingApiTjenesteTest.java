package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.InntektsmeldingStatusDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiTjenesteTest {

    private static final String ORGNUMMER = "974760673";
    private static final String FNR = "11111111111";
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567890123");

    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ForespørselEntitet forespørselEntitet;

    private InntektsmeldingApiTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new InntektsmeldingApiTjeneste(inntektsmeldingRepository, forespørselBehandlingTjeneste, personTjeneste);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ingenAndreFiltre_returnerAlleInntektsmeldinger() {
        var forespørselUuid = UUID.randomUUID();
        var im = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(im));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid, null, null, null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).fnr().fnr()).isEqualTo(FNR);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogFnrFilter_returnerMatchendeInntektsmeldinger() {
        var forespørselUuid = UUID.randomUUID();
        var annenAktørId = new AktørIdEntitet("9999999999999");
        var im1 = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        var im2 = lagInntektsmeldingEntitet(annenAktørId, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(im1, im2));
        when(personTjeneste.finnAktørIdForPersonIdent(FNR)).thenReturn(Optional.of(AKTØR_ID));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), new FødselsnummerDto(FNR), null, forespørselUuid, null, null, null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).fnr().fnr()).isEqualTo(FNR);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogYtelseTypeFilter_returnerMatchendeInntektsmeldinger() {
        var forespørselUuid = UUID.randomUUID();
        var im1 = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        var im2 = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.OMSORGSPENGER, LocalDate.of(2024, 1, 1));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(im1, im2));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, forespørselUuid, null, null, null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).ytelseType()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogPeriodeFilter_returnerMatchendeInntektsmeldinger() {
        var forespørselUuid = UUID.randomUUID();
        var imInnenfor = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 3, 1));
        var imFørPeriode = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        var imEtterPeriode = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 6, 1));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(imInnenfor, imFørPeriode, imEtterPeriode));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid,
            LocalDate.of(2024, 2, 1), LocalDate.of(2024, 4, 30), null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).startDato()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogLoepenrFilter_returnerInntektsmeldingerMedHøyereLoepenr() {
        var forespørselUuid = UUID.randomUUID();
        var imForespørsel = mock(ForespørselEntitet.class);
        when(imForespørsel.getUuid()).thenReturn(forespørselUuid);
        var im1 = mock(InntektsmeldingEntitet.class);
        var im2 = mock(InntektsmeldingEntitet.class);
        when(im1.getArbeidsgiverIdent()).thenReturn(ORGNUMMER);
        when(im2.getArbeidsgiverIdent()).thenReturn(ORGNUMMER);
        when(im1.getLoepenr()).thenReturn(1L);
        when(im2.getLoepenr()).thenReturn(2L);
        when(im2.getUuid()).thenReturn(UUID.randomUUID());
        when(im2.getAktørId()).thenReturn(AKTØR_ID);
        when(im2.getYtelsetype()).thenReturn(Ytelsetype.PLEIEPENGER_SYKT_BARN);
        when(im2.getStartDato()).thenReturn(LocalDate.of(2024, 1, 1));
        when(im2.getForespørsel()).thenReturn(imForespørsel);
        when(im2.getBorfalteNaturalYtelser()).thenReturn(List.of());
        when(im2.getEndringsårsaker()).thenReturn(List.of());
        when(im2.getKontaktperson()).thenReturn(new KontaktpersonEntitet("Kontakt", "99999999"));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(im1, im2));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid, null, null, 1L, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).loepenr()).isEqualTo(2L);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogStatusIkkeGodkjent_returnerTomListe() {
        var forespørselUuid = UUID.randomUUID();

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid, null, null, null, InntektsmeldingStatusDto.UNDER_BEHANDLING);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).isEmpty();
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogStatusGodkjent_returnerInntektsmeldinger() {
        var forespørselUuid = UUID.randomUUID();
        var im = lagInntektsmeldingEntitet(AKTØR_ID, ORGNUMMER, Ytelsetype.PLEIEPENGER_SYKT_BARN, LocalDate.of(2024, 1, 1));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørselEntitet));
        when(forespørselEntitet.getInntektsmeldinger()).thenReturn(List.of(im));
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(new PersonIdent(FNR));

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid, null, null, null, InntektsmeldingStatusDto.GODKJENT);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).hasSize(1);
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_ogFnrFinnesIkke_returnerTomListe() {
        var forespørselUuid = UUID.randomUUID();
        when(personTjeneste.finnAktørIdForPersonIdent(FNR)).thenReturn(Optional.empty());

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), new FødselsnummerDto(FNR), null, forespørselUuid, null, null, null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).isEmpty();
    }

    @Test
    void hentInntektsmeldinger_medForespørselUuid_forespørselFinnesIkke_returnerTomListe() {
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.empty());

        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, forespørselUuid, null, null, null, null);
        var resultat = tjeneste.hentInntektsmeldinger(request);

        assertThat(resultat).isEmpty();
    }

    private InntektsmeldingEntitet lagInntektsmeldingEntitet(AktørIdEntitet aktørId, String orgnr, Ytelsetype ytelsetype, LocalDate startDato) {
        return new InntektsmeldingEntitet.Builder()
            .medAktørId(aktørId)
            .medArbeidsgiverIdent(orgnr)
            .medYtelsetype(ytelsetype)
            .medStartDato(startDato)
            .medMånedInntekt(BigDecimal.valueOf(50000))
            .medKontaktperson(new KontaktpersonEntitet("Kontakt Person", "99999999"))
            .medForespørsel(forespørselEntitet)
            .build();
    }

}
