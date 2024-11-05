package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.SøknadsperiodeEntitet;

import no.nav.familie.inntektsmelding.typer.entitet.IntervallEntitet;

import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class OpplysningerDtoTjenesteTest {
    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;

    private OpplysningerDtoTjeneste opplysningerDtoTjeneste;

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
    void setup() {
        opplysningerDtoTjeneste = new OpplysningerDtoTjeneste(personTjeneste, organisasjonTjeneste, inntektTjeneste);
    }

    @Test
    void skal_lage_dto_ingen_søknadsperidoer() {
        // Arrange
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "123",
            Collections.emptyList());
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer));
        var inntekt1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt2 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 4), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt3 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 5), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of(inntekt1, inntekt2, inntekt3)));

        // Act
        var imDialogDto = opplysningerDtoTjeneste.lagOpplysningerDto(forespørsel);

        // Assert
        assertThat(imDialogDto.startdatoPermisjon()).isEqualTo(forespørsel.getSkjæringstidspunkt());
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);

        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).hasSize(3);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52_000));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
    }

    @Test
    void skal_lage_dto_med_søknadsperidoer() {
        // Arrange
        var søknadsperioder = List.of(new SøknadsperiodeEntitet(IntervallEntitet.fraOgMedTilOgMed(LocalDate.now(),
                LocalDate.now().plusDays(10))),
            new SøknadsperiodeEntitet(IntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(15), LocalDate.now().plusDays(30))));
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "123",
            søknadsperioder);
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer));
        var inntekt1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt2 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 4), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt3 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 5), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer(), List.of(inntekt1, inntekt2, inntekt3)));

        // Act
        var opplysninger = opplysningerDtoTjeneste.lagOpplysningerDto(forespørsel);

        // Assert
        assertThat(opplysninger.søknadsopplysninger()).isNotNull();
        assertThat(opplysninger.søknadsopplysninger().perioder()).hasSize(2);
        assertThat(opplysninger.søknadsopplysninger().førsteFraværsdag()).isEqualTo(LocalDate.now());
        assertThat(opplysninger.søknadsopplysninger().sisteFraværsdag()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(opplysninger.søknadsopplysninger().perioder().stream().anyMatch(p -> p.fom().equals(LocalDate.now()))).isTrue();
        assertThat(opplysninger.søknadsopplysninger().perioder().stream().anyMatch(p -> p.tom().equals(LocalDate.now().plusDays(10)))).isTrue();
        assertThat(opplysninger.søknadsopplysninger().perioder().stream().anyMatch(p -> p.fom().equals(LocalDate.now().plusDays(15)))).isTrue();
        assertThat(opplysninger.søknadsopplysninger().perioder().stream().anyMatch(p -> p.tom().equals(LocalDate.now().plusDays(30)))).isTrue();
    }

}
