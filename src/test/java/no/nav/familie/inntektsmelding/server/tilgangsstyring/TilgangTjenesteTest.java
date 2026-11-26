package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.pip.ForespørselTilgangInfo;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class TilgangTjenesteTest {

    private static final OrganisasjonsnummerDto ORGNR = new OrganisasjonsnummerDto("123456789");
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567891234");
    private static final PersonIdent FNR = new PersonIdent("12345678910");
    private static final LocalDate DATO = LocalDate.of(2025, 6, 1);

    private Tilgang tilgangTjeneste;

    @Mock
    private PipTjeneste pipTjeneste;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    @Mock
    private SifAbacPdpKlient sifAbacPdpKlient;

    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @Mock
    private InntektTjeneste inntektTjeneste;

    @Mock
    private PersonTjeneste personTjeneste;

    @BeforeEach
    void setUp() {
        tilgangTjeneste = new TilgangTjeneste(pipTjeneste, altinnTilgangTjeneste, sifAbacPdpKlient, arbeidstakerTjeneste, inntektTjeneste, personTjeneste);
    }

    @AfterEach
    void fjernKontekst() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void test_borgen_inisjert_kall_uten_request_kontekts_nok() {
        KontekstHolder.setKontekst(RequestKontekst.ikkeAutentisertRequest("testConsument"));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_uten_riktig_token_type_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Kun borger kall støttes.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_forespørsel_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var ex = assertThrows(ManglerTilgangException.class,
            () -> {
                tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);
            });
        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

        verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);

        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_forespørsel_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentOrganisasjonsnummerFor(forespørselUuid)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);

    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_forespørsel_tilgang_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var forespørselUuid = UUID.randomUUID();
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentOrganisasjonsnummerFor(forespørselUuid)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid));

        verify(pipTjeneste).hentOrganisasjonsnummerFor(forespørselUuid);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borger_initiert_kall_fra_organisasjon_ikke_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var fakeOrgNr = "123456789";

        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(fakeOrgNr)));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borger_initiert_kall_fra_organisasjon_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var okOrgNr = "123456789";

        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(okOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(okOrgNr)));

        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(okOrgNr);
    }


    @Test
    void test_borgen_inisjert_kall_mangler_informasjon_om_bedrift_fra_pip_inntektsmelding_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");

        verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void test_borgen_inisjert_kall_mangler_tilgang_til_tjenesten_fra_pip_inntektsmelding_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentOrganisasjonsnummerFor(inntektsmeldingId)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(true);

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));
        assertThat(ex.getMessage()).contains("Bruker mangler tilgang til bedriften i Altinn.");

        verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }

    @Test
    void test_borgen_inisjert_kall_fra_pip_inntektsmelding_tilgang_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var inntektsmeldingId = 1L;
        var fakeOrgNr = "123456789";
        when(pipTjeneste.hentOrganisasjonsnummerFor(inntektsmeldingId)).thenReturn(new OrganisasjonsnummerDto(fakeOrgNr));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(fakeOrgNr)).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId));

        verify(pipTjeneste).hentOrganisasjonsnummerFor(inntektsmeldingId);
        verify(altinnTilgangTjeneste).manglerTilgangTilBedriften(fakeOrgNr);
    }


    @Test
    void forespørsel_null_uuid_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilForespørsel(null, AKTØR_ID, ORGNR));

        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void bedrift_null_uuid_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));

        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilBedrift((UUID) null));

        assertThat(ex.getMessage()).contains("Mangler informasjon om bedrift.");
        verifyNoInteractions(pipTjeneste, altinnTilgangTjeneste);
    }

    @Test
    void forespørsel_med_samme_aktør_og_orgnr_gir_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var uuid = UUID.randomUUID();
        when(pipTjeneste.hentForespørselTilgangInfo(uuid)).thenReturn(Optional.of(new ForespørselTilgangInfo(ORGNR, AKTØR_ID)));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilForespørsel(uuid, AKTØR_ID, ORGNR));
    }

    @Test
    void forespørsel_med_annet_orgnr_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var uuid = UUID.randomUUID();
        when(pipTjeneste.hentForespørselTilgangInfo(uuid)).thenReturn(Optional.of(new ForespørselTilgangInfo(ORGNR, AKTØR_ID)));

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilForespørsel(uuid, AKTØR_ID, new OrganisasjonsnummerDto("987654321")));

        assertThat(ex.getMessage()).contains("Organisasjonsnummer stemmer ikke med forespørselen.");
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void forespørsel_med_annen_aktør_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var uuid = UUID.randomUUID();
        when(pipTjeneste.hentForespørselTilgangInfo(uuid)).thenReturn(Optional.of(new ForespørselTilgangInfo(ORGNR, AKTØR_ID)));

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilForespørsel(uuid, new AktørIdEntitet("9999999999999"), ORGNR));

        assertThat(ex.getMessage()).contains("Arbeidstaker stemmer ikke med forespørselen.");
        verifyNoInteractions(altinnTilgangTjeneste);
    }

    @Test
    void arbeidstaker_med_aktivt_arbeidsforhold_i_bedriften_gir_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(FNR, DATO, DATO))
            .thenReturn(List.of(new ArbeidsforholdDto(ORGNR.orgnr(), new ArbeidsforholdDto.Ansettelsesperiode(DATO.minusYears(1), Tid.TIDENES_ENDE))));

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(FNR, ORGNR, DATO));
    }

    @Test
    void arbeidstaker_med_aktørId_slår_opp_fnr() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(personTjeneste.finnPersonIdentForAktørId(AKTØR_ID)).thenReturn(FNR);
        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(FNR, DATO, DATO))
            .thenReturn(List.of(new ArbeidsforholdDto(ORGNR.orgnr(), new ArbeidsforholdDto.Ansettelsesperiode(DATO, DATO))));

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(AKTØR_ID, ORGNR, DATO));
    }

    @Test
    void arbeidstaker_uten_arbeidsforhold_i_bedriften_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(FNR, DATO, DATO))
            .thenReturn(List.of(new ArbeidsforholdDto("987654321", new ArbeidsforholdDto.Ansettelsesperiode(DATO.minusYears(1), Tid.TIDENES_ENDE))));

        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(FNR, ORGNR, DATO));

        assertThat(ex.getMessage()).contains("Arbeidstaker har ikke arbeidsforhold i bedriften.");
    }

    @Test
    void arbeidstaker_med_avsluttet_arbeidsforhold_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(FNR, DATO, DATO))
            .thenReturn(List.of(new ArbeidsforholdDto(ORGNR.orgnr(), new ArbeidsforholdDto.Ansettelsesperiode(DATO.minusYears(1), DATO.minusDays(1)))));

        assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(FNR, ORGNR, DATO));
    }

    @Test
    void arbeidstaker_uten_altinn_tilgang_sjekker_ikke_aareg() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(true);

        assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(AKTØR_ID, ORGNR, DATO));

        verifyNoInteractions(arbeidstakerTjeneste, personTjeneste);
    }

    @Test
    void arbeidstaker_med_inntekt_gir_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var personInfo = lagPersonInfo();
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(personInfo);
        when(inntektTjeneste.hentInntekt(personInfo, DATO, LocalDate.now(), ORGNR.orgnr(), Ytelsetype.OMSORGSPENGER))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(50000), ORGNR.orgnr(),
                List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(50000), YearMonth.from(DATO.minusMonths(1)), MånedslønnStatus.BRUKT_I_GJENNOMSNITT))));

        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstakerMedInntekt(AKTØR_ID, ORGNR, DATO, Ytelsetype.OMSORGSPENGER));
    }

    @Test
    void arbeidstaker_med_inntekt_0_gir_ikke_tilgang() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var personInfo = lagPersonInfo();
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(personInfo);
        when(inntektTjeneste.hentInntekt(personInfo, DATO, LocalDate.now(), ORGNR.orgnr(), Ytelsetype.OMSORGSPENGER))
            .thenReturn(new Inntektsopplysninger(BigDecimal.ZERO, ORGNR.orgnr(),
                List.of(new Inntektsopplysninger.InntektMåned(BigDecimal.ZERO, YearMonth.from(DATO.minusMonths(1)), MånedslønnStatus.BRUKT_I_GJENNOMSNITT))));

        var ex = assertThrows(ManglerTilgangException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstakerMedInntekt(AKTØR_ID, ORGNR, DATO, Ytelsetype.OMSORGSPENGER));

        assertThat(ex.getMessage()).contains("Arbeidstaker har ikke inntekt i bedriften.");
    }

    @Test
    void arbeidstaker_med_nedetid_i_a_inntekt_gir_A_INNTEKT_NEDE() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var personInfo = lagPersonInfo();
        when(altinnTilgangTjeneste.manglerTilgangTilBedriften(ORGNR.orgnr())).thenReturn(false);
        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(personInfo);
        when(inntektTjeneste.hentInntekt(personInfo, DATO, LocalDate.now(), ORGNR.orgnr(), Ytelsetype.OMSORGSPENGER))
            .thenReturn(new Inntektsopplysninger(null, ORGNR.orgnr(),
                List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.from(DATO.minusMonths(1)), MånedslønnStatus.NEDETID_AINNTEKT))));

        var ex = assertThrows(FunksjonellException.class,
            () -> tilgangTjeneste.sjekkAtArbeidsgiverHarTilgangTilArbeidstakerMedInntekt(AKTØR_ID, ORGNR, DATO, Ytelsetype.OMSORGSPENGER));

        assertThat(ex.getMessage()).contains("A_INNTEKT_NEDE");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_ikke_saksbehandler_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_mangler_rollen_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_feil_rolle_nok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker, Set.of(AnsattGruppe.SAKSBEHANDLER, AnsattGruppe.VEILEDER)));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
        assertThat(ex.getMessage()).contains("Ansatt mangler en rolle.");
    }

    @Test
    void test_sjekk_om_ansatt_har_rollen_drift_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker, Set.of(AnsattGruppe.DRIFT)));
        assertDoesNotThrow(() -> tilgangTjeneste.sjekkAtAnsattHarRollenDrift());
    }

    @Test
    void test_sjekk_om_systembruker_kall_nok_pga_internbruker() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.InternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkErSystembruker());
        assertThat(ex.getMessage()).contains("Kun systemkall støttes.");
    }

    @Test
    void test_sjekk_om_systembruker_kall_nok_pga_eksternbruker() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.EksternBruker));
        var ex = assertThrows(ManglerTilgangException.class, () -> tilgangTjeneste.sjekkErSystembruker());
        assertThat(ex.getMessage()).contains("Kun systemkall støttes.");
    }

    @Test
    void test_sjekk_om_systembruker_kall_ok() {
        KontekstHolder.setKontekst(fakeRequestKontekts(IdentType.Systemressurs));
        assertDoesNotThrow(() -> tilgangTjeneste.sjekkErSystembruker());
    }

    private static PersonInfo lagPersonInfo() {
        return new PersonInfo("Fornavn", null, "Etternavn", FNR, AKTØR_ID, LocalDate.of(1990, 1, 1), null);
    }

    private Kontekst fakeRequestKontekts(IdentType identType) {
        return fakeRequestKontekts(identType, Set.of());
    }

    private Kontekst fakeRequestKontekts(IdentType identType, Set<AnsattGruppe> groups) {
        return RequestKontekst.forRequest("brukerUid", "brukerUid", identType, fakeOidcToken(OpenIDProvider.TOKENX), UUID.randomUUID(), groups);
    }

    private OpenIDToken fakeOidcToken(OpenIDProvider provider) {
        return new OpenIDToken(provider, new TokenString("fakeTokenString"));
    }
}
