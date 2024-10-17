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

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;
    @Mock
    private FpDokgenTjeneste fpDokgenTjeneste;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

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
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository, personTjeneste,
            organisasjonTjeneste, inntektTjeneste, fpDokgenTjeneste, prosessTaskTjeneste);
    }

    @Test
    void skal_lage_dto() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER, "123");
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer));
        var inntekt1 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 3), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        var inntekt2 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 4), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        var inntekt3 = new InntektTjeneste.Månedsinntekt(YearMonth.of(2024, 5), BigDecimal.valueOf(52000), forespørsel.getOrganisasjonsnummer());
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(List.of(inntekt1, inntekt2, inntekt3));

        // Act
        var imDialogDto = inntektsmeldingTjeneste.lagDialogDto(uuid);

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

        assertThat(imDialogDto.inntekter()).hasSize(3);

        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
        assertThat(imDialogDto.inntekter()).contains(
            new InntektsmeldingDialogDto.MånedsinntektResponsDto(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), BigDecimal.valueOf(52000),
                forespørsel.getOrganisasjonsnummer()));
    }

    @Test
    void skal_lage_tomme_innteker() {
        var organisasjonsnummer = "999999999";
        var i1 = InntektTjeneste.fyllInnTommeInntekter(LocalDate.of(2024, 2, 15), organisasjonsnummer);

        assertThat(i1.size()).isEqualTo(3);
        assertThat(i1.get(0).beløp()).isNull();
        assertThat(i1.get(0).organisasjonsnummer()).isEqualTo(organisasjonsnummer);
        assertThat(i1.get(0).måned()).isEqualTo(YearMonth.of(2023, 11));
        assertThat(i1.get(1).måned()).isEqualTo(YearMonth.of(2023, 12));
        assertThat(i1.get(2).måned()).isEqualTo(YearMonth.of(2024, 1));
    }

}
