package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidsforholdTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.K9SakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.Kjønn;
import no.nav.familie.inntektsmelding.typer.dto.MånedsinntektDto;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class GrunnlagTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";

    private static final String ORGNR = "999999999";
    private static final String FNR = "11111111111";
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("9999999999999");
    private static final PersonIdent PERSON_IDENT = new PersonIdent(FNR);
    private static final String NAVN = "Navn";
    private static final String ETTERNAVN = "Navnesen";
    private static final PersonInfo PERSON_INFO = new PersonInfo(NAVN, null, ETTERNAVN, PERSON_IDENT, AKTØR_ID, LocalDate.now(), null, Kjønn.KVINNE);

    private static final String INNSENDER_NAVN = "Ine";
    private static final String INNSENDER_ETTERNAVN = "Sender";
    private static final String INNSENDER_TELEFON = "+4711111111";
    private static final PersonInfo INNSENDER_PERSON_INFO = new PersonInfo(INNSENDER_NAVN, null, INNSENDER_ETTERNAVN, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), INNSENDER_TELEFON, Kjønn.KVINNE);

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    @Mock
    private K9SakTjeneste k9SakTjeneste;


    private GrunnlagTjeneste grunnlagTjeneste;

    @BeforeAll
    static void beforeAll() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest(INNMELDER_UID, "kompakt", IdentType.EksternBruker,
            new OpenIDToken(OpenIDProvider.TOKENX, new TokenString("token")), UUID.randomUUID(), Set.of()));
    }

    @AfterAll
    static void afterAll() {
        KontekstHolder.fjernKontekst();
    }

    @BeforeEach
    void setUp() {
        grunnlagTjeneste = new GrunnlagTjeneste(forespørselBehandlingTjeneste, personTjeneste, organisasjonTjeneste, inntektTjeneste, arbeidstakerTjeneste, arbeidsforholdTjeneste, k9SakTjeneste);
    }

    @Test
    void skal_hente_opplysninger() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR,
            LocalDate.now(),
            AKTØR_ID.getAktørId(),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            null,
            null);

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId())).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(INNSENDER_PERSON_INFO);
        var inntekt1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt2 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 4), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt3 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 5), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        when(inntektTjeneste.hentInntekt(PERSON_INFO, forespørsel.getSkjæringstidspunkt(), LocalDate.now(), forespørsel.getOrganisasjonsnummer(), Ytelsetype.PLEIEPENGER_SYKT_BARN))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of(inntekt1, inntekt2, inntekt3)));

        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo(NAVN);
        assertThat(imDialogDto.person().etternavn()).isEqualTo(ETTERNAVN);

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now());

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(INNSENDER_NAVN);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(INNSENDER_ETTERNAVN);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(INNSENDER_TELEFON);

        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).hasSize(3);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52_000));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new MånedsinntektDto(LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new MånedsinntektDto(LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 4, 30),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new MånedsinntektDto(LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 31),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
    }

    @Test
    void skal_hente_opplysninger_med_første_uttaksdato() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR,
            LocalDate.now(),
            AKTØR_ID.getAktørId(),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            LocalDate.now().plusDays(10),
            null);

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId())).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(INNSENDER_PERSON_INFO);
        when(inntektTjeneste.hentInntekt(PERSON_INFO, forespørsel.getSkjæringstidspunkt(), LocalDate.now(), forespørsel.getOrganisasjonsnummer(), Ytelsetype.PLEIEPENGER_SYKT_BARN))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of()));

        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo(NAVN);
        assertThat(imDialogDto.person().etternavn()).isEqualTo(ETTERNAVN);

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now().plusDays(10));

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(INNSENDER_NAVN);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(INNSENDER_ETTERNAVN);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(INNSENDER_TELEFON);
    }

    @Test
    void skal_hente_arbeidsforhold_gitt_fnr() {
        // Arrange
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesperiode = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now(), LocalDate.now().plusMonths(2));

        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(PERSON_IDENT, førsteFraværsdag, førsteFraværsdag)).thenReturn(List.of(new ArbeidsforholdDto(ORGNR, ansettelsesperiode)));
        when(organisasjonTjeneste.finnOrganisasjon(ORGNR)).thenReturn(new Organisasjon("Bedriften", ORGNR));

        // Act
        var response = grunnlagTjeneste.finnArbeidsforholdForFnr(PERSON_INFO, LocalDate.now()).orElse(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.fornavn()).isEqualTo(NAVN);
        assertThat(response.etternavn()).isEqualTo(ETTERNAVN);
        assertThat(response.arbeidsforhold()).hasSize(1);
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnavn()).isEqualTo("Bedriften");
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnummer()).isEqualTo(ORGNR);
    }

    @Test
    void skal_hente_personinfo_og_organisasjoner_arbeidsgiver_har_tilgang_til_gitt_fnr() {
        // Arrange
        var orgnr1 = new OrganisasjonsnummerDto("123456789");
        var orgnr2 = new OrganisasjonsnummerDto("987654321");
        var navn1 = "Organisasjon 1";
        var navn2 = "Organisasjon 2";

        when(arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil()).thenReturn(List.of(orgnr1, orgnr2));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr1.orgnr())).thenReturn(new Organisasjon(navn1, orgnr1.orgnr()));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr2.orgnr())).thenReturn(new Organisasjon(navn2, orgnr2.orgnr()));
        // Act
        var organisasjoner = grunnlagTjeneste.hentOrganisasjonerSomArbeidsgiverHarTilgangTil();
        var response = grunnlagTjeneste.lagHentArbeidsforholdResponse(PERSON_INFO, organisasjoner);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.fornavn()).isEqualTo(NAVN);
        assertThat(response.etternavn()).isEqualTo(ETTERNAVN);
        assertThat(response.arbeidsforhold()).hasSize(2);
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnavn().equals(navn1));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnavn().equals(navn2));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnummer().equals(orgnr1.orgnr()));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnummer().equals(orgnr2.orgnr()));
        assertThat(response.kjønn()).isEqualTo(Kjønn.KVINNE);
    }

    @Test
    void skal_hente_opplysninger_uten_forespørsel_uuid_hvis_eksisternede_forespøsel_er_utenfor_4_uker() {
        // Arrange
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto(ORGNR);
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR,
            førsteFraværsdag.plusWeeks(4),
            AKTØR_ID.getAktørId(),
            ytelsetype,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            førsteFraværsdag.plusWeeks(1),
            null);

        when(personTjeneste.hentPersonFraIdent(PERSON_IDENT)).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(INNSENDER_PERSON_INFO);
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(AKTØR_ID, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(PERSON_INFO, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(PERSON_IDENT,
            ytelsetype,
            førsteFraværsdag,
            organisasjonsnummer,
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(AKTØR_ID.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo(NAVN);
        assertThat(imDialogDto.person().etternavn()).isEqualTo(ETTERNAVN);
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isNull();
    }

    @Test
    void skal_hente_opplysninger_med_forespørsel_uuid_hvis_eksisternede_forespøsel_er_innenfor_4_uker() {
        // Arrange
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto(ORGNR);
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR, førsteFraværsdag, AKTØR_ID.getAktørId(), ytelsetype, "123", ForespørselType.BESTILT_AV_FAGSYSTEM, førsteFraværsdag, null);

        when(personTjeneste.hentPersonFraIdent(PERSON_IDENT)).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(INNSENDER_PERSON_INFO);
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(AKTØR_ID, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(PERSON_INFO, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(PERSON_IDENT, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(AKTØR_ID.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo(NAVN);
        assertThat(imDialogDto.person().etternavn()).isEqualTo(ETTERNAVN);
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isEqualTo(forespørsel.getUuid());
    }

    @Test
    void skal_ikke_bruke_eksisterende_forespørsel_hvis_den_er_utgått () {
        // Arrange
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto(ORGNR);
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR, førsteFraværsdag, AKTØR_ID.getAktørId(), ytelsetype, "123", ForespørselType.BESTILT_AV_FAGSYSTEM, førsteFraværsdag, null);
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);

        when(personTjeneste.hentPersonFraIdent(PERSON_IDENT)).thenReturn(PERSON_INFO);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(INNSENDER_PERSON_INFO);
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(AKTØR_ID, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(PERSON_INFO, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(PERSON_IDENT, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(AKTØR_ID.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo(NAVN);
        assertThat(imDialogDto.person().etternavn()).isEqualTo(ETTERNAVN);
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isEqualTo(null);
    }
}
