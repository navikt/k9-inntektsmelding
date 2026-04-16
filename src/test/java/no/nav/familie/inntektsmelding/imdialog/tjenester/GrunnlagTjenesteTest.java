package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
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
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.FagsakInfo;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.K9SakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidsforholdTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.Kjønn;
import no.nav.familie.inntektsmelding.typer.dto.MånedsinntektDto;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class GrunnlagTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";

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
        var forespørsel = ForespørselMapper.mapForespørsel("999999999",
            LocalDate.now(),
            "9999999999999",
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            null,
            null);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null, null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer, Kjønn.KVINNE));
        var inntekt1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt2 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 4), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt3 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 5), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(), forespørsel.getOrganisasjonsnummer(), Ytelsetype.PLEIEPENGER_SYKT_BARN))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of(inntekt1, inntekt2, inntekt3)));

        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now());

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);

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
        var forespørsel = ForespørselMapper.mapForespørsel("999999999",
            LocalDate.now(),
            "9999999999999",
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            LocalDate.now().plusDays(10),
            null);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null, null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer, Kjønn.KVINNE));
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(), forespørsel.getOrganisasjonsnummer(), Ytelsetype.PLEIEPENGER_SYKT_BARN))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of()));

        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now().plusDays(10));

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);
    }

    @Test
    void skal_hente_arbeidsforhold_gitt_fnr() {
        // Arrange
        var fnr = new PersonIdent("11111111111");
        var orgnr = "999999999";
        var førsteFraværsdag = LocalDate.now();
        var aktørId = new AktørIdEntitet("9999999999999");
        var ansettelsesperiode = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now(), LocalDate.now().plusMonths(2));
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fnr, aktørId, LocalDate.now(), null, Kjønn.KVINNE);

        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fnr, førsteFraværsdag)).thenReturn(List.of(new ArbeidsforholdDto(orgnr, ansettelsesperiode)));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr)).thenReturn(new Organisasjon("Bedriften", orgnr));

        // Act
        var response = grunnlagTjeneste.finnArbeidsforholdForFnr(personInfo, LocalDate.now()).orElse(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.fornavn()).isEqualTo("Navn");
        assertThat(response.etternavn()).isEqualTo("Navnesen");
        assertThat(response.arbeidsforhold()).hasSize(1);
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnavn()).isEqualTo("Bedriften");
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnummer()).isEqualTo(orgnr);
    }

    @Test
    void skal_hente_personinfo_og_organisasjoner_arbeidsgiver_har_tilgang_til_gitt_fnr() {
        // Arrange
        var fnr = new PersonIdent("11111111111");
        var aktørId = new AktørIdEntitet("9999999999999");
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fnr, aktørId, LocalDate.now(), null, null);
        var orgnr1 = new OrganisasjonsnummerDto("123456789");
        var orgnr2 = new OrganisasjonsnummerDto("987654321");
        var navn1 = "Organisasjon 1";
        var navn2 = "Organisasjon 2";
        when(arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil()).thenReturn(List.of(orgnr1, orgnr2));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr1.orgnr())).thenReturn(new Organisasjon(navn1, orgnr1.orgnr()));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr2.orgnr())).thenReturn(new Organisasjon(navn2, orgnr2.orgnr()));
        // Act
        var organisasjoner = grunnlagTjeneste.hentOrganisasjonerSomArbeidsgiverHarTilgangTil();
        var response = grunnlagTjeneste.lagHentArbeidsforholdResponse(personInfo, organisasjoner);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.fornavn()).isEqualTo("Navn");
        assertThat(response.etternavn()).isEqualTo("Navnesen");
        assertThat(response.arbeidsforhold()).hasSize(2);
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnavn().equals(navn1));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnavn().equals(navn2));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnummer().equals(orgnr1.orgnr()));
        assertThat(response.arbeidsforhold().stream()).anyMatch(o -> o.organisasjonsnummer().equals(orgnr2.orgnr()));
        assertThat(response.kjønn()).isNull();
    }

    @Test
    void skal_hente_opplysninger_uten_forespørsel_uuid_hvis_eksisternede_forespøsel_er_utenfor_4_uker() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = ForespørselMapper.mapForespørsel("999999999",
            førsteFraværsdag.plusWeeks(4),
            aktørId.getAktørId(),
            ytelsetype,
            "123",
            ForespørselType.BESTILT_AV_FAGSYSTEM,
            førsteFraværsdag.plusWeeks(1),
            null);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, Kjønn.KVINNE);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", Kjønn.KVINNE));
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(fødselsnummer,
            ytelsetype,
            førsteFraværsdag,
            organisasjonsnummer,
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(aktørId.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isNull();
    }

    @Test
    void skal_hente_opplysninger_med_forespørsel_uuid_hvis_eksisternede_forespøsel_er_innenfor_4_uker() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = ForespørselMapper.mapForespørsel("999999999", førsteFraværsdag, aktørId.getAktørId(), ytelsetype, "123", ForespørselType.BESTILT_AV_FAGSYSTEM, førsteFraværsdag, null);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, Kjønn.KVINNE);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer)).thenReturn(personInfo);
        when(personTjeneste.hentPersonInfoFraAktørId(aktørId)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", Kjønn.KVINNE));
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(fødselsnummer, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(aktørId.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isEqualTo(forespørsel.getUuid());
    }

    @Test
    void skal_ikke_bruke_eksisterende_forespørsel_hvis_den_er_utgått () {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = ForespørselMapper.mapForespørsel("999999999", førsteFraværsdag, aktørId.getAktørId(), ytelsetype, "123", ForespørselType.BESTILT_AV_FAGSYSTEM, førsteFraværsdag, null);
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, Kjønn.KVINNE);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", Kjønn.KVINNE));
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagTjeneste.hentOpplysninger(fødselsnummer, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(aktørId.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isEqualTo(null);
    }

    @Test
    void skal_hente_etterspurte_perioder_for_omsorgspenger_uregistrert() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.OMSORGSPENGER;
        var førsteFraværsdag = LocalDate.of(2024, 6, 15);
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, Kjønn.KVINNE);

        when(personTjeneste.hentPersonFraIdent(fødselsnummer)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", Kjønn.KVINNE));
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of());
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften", organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));

        var etterspurtPeriode1 = new PeriodeDto(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));
        var etterspurtPeriode2 = new PeriodeDto(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31));
        var annenOrganisasjonsnummer = "888888888";
        var etterspurtPeriodeAnnenOrg = new PeriodeDto(LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30));
        var fagsakInfo = new FagsakInfo(
            new SaksnummerDto("123"),
            ytelsetype,
            new AktørId(aktørId.getAktørId()),
            new PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
            List.of(new PeriodeDto(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30))),
            Map.of(organisasjonsnummer.orgnr(), Set.of(etterspurtPeriode1, etterspurtPeriode2),
                annenOrganisasjonsnummer, Set.of(etterspurtPeriodeAnnenOrg)),
            false);
        when(k9SakTjeneste.hentFagsakInfo(ytelsetype, new AktørId(aktørId.getAktørId()))).thenReturn(List.of(fagsakInfo));

        // Act
        var opplysninger = grunnlagTjeneste.hentOpplysninger(fødselsnummer, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);

        // Assert
        assertThat(opplysninger.forespørselUuid()).isNull();
        assertThat(opplysninger.etterspurtePerioder()).isNotNull();
        assertThat(opplysninger.etterspurtePerioder()).hasSize(2);
        assertThat(opplysninger.etterspurtePerioder()).contains(new PeriodeDto(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30)));
        assertThat(opplysninger.etterspurtePerioder()).contains(new PeriodeDto(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31)));
        assertThat(opplysninger.etterspurtePerioder()).doesNotContain(new PeriodeDto(LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30)));
    }

    @Test
    void skal_returnere_null_etterspurte_perioder_når_søknadsperiode_ikke_inneholder_første_fraværsdag() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.OMSORGSPENGER;
        var førsteFraværsdag = LocalDate.of(2024, 8, 15);
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, Kjønn.KVINNE);

        when(personTjeneste.hentPersonFraIdent(fødselsnummer)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID))).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", Kjønn.KVINNE));
        when(forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, organisasjonsnummer.orgnr())).thenReturn(List.of());
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften", organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId, førsteFraværsdag, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype))
            .thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));

        var fagsakInfo = new FagsakInfo(
            new SaksnummerDto("123"),
            ytelsetype,
            new AktørId(aktørId.getAktørId()),
            new PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
            List.of(new PeriodeDto(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30))),
            Map.of(organisasjonsnummer.orgnr(), Set.of(new PeriodeDto(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30)))),
            false);
        when(k9SakTjeneste.hentFagsakInfo(ytelsetype, new AktørId(aktørId.getAktørId()))).thenReturn(List.of(fagsakInfo));

        // Act
        var opplysninger = grunnlagTjeneste.hentOpplysninger(fødselsnummer, ytelsetype, førsteFraværsdag, organisasjonsnummer, ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);

        // Assert
        assertThat(opplysninger.forespørselUuid()).isNull();
        assertThat(opplysninger.etterspurtePerioder()).isNull();
    }
}
