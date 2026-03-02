package no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class ArbeidstakerTjenesteTest {

    private static final PersonIdent TILFELDIG_PERSON_IDENT = PersonIdent.fra("21073926618");

    @Mock
    private ArbeidsforholdTjeneste arbeidsforholdTjenesteMock;

    @Mock
    private AltinnTilgangTjeneste altinnTilgangTjenesteMock;

    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    @BeforeEach
    void setUp() {
        this.arbeidstakerTjeneste = new ArbeidstakerTjeneste(this.arbeidsforholdTjenesteMock, this.altinnTilgangTjenesteMock);
    }

    @Test
    void returnerer_arbeidstakerinfo_om_dette_finnes() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesperiode = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now(), LocalDate.now().plusMonths(2));
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new ArbeidsforholdDto("000000000", "111111111", ansettelsesperiode))
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);
        assertThat(resultat).hasSize(1);

        var arbeidsforhold = resultat.getFirst();
        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("000000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("111111111");
    }

    @Test
    void verifiserer_arbeidsforhold_detaljer() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesPeriode = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(new ArbeidsforholdDto("00000000", "123456789", ansettelsesPeriode)));
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften(any())).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat).hasSize(1);
        var arbeidsforhold = resultat.getFirst();

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000000");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }

    @Test
    void filtrerer_ut_arbeidsforhold_man_ikke_har_tilgang_til() {
        var førsteFraværsdag = LocalDate.now();
        var ansettelsesPeriode = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE);
        var ansettelsesPeriode2 = new ArbeidsforholdDto.Ansettelsesperiode(LocalDate.now().minusYears(1), LocalDate.now().plusMonths(5));
        when(arbeidsforholdTjenesteMock.hentArbeidsforhold(any(), any())).thenReturn(
            List.of(
                new ArbeidsforholdDto("00000000", "123456789", ansettelsesPeriode),
                new ArbeidsforholdDto("00000001", "123456789", ansettelsesPeriode2)
            )
        );
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000000")).thenReturn(false);
        when(altinnTilgangTjenesteMock.harTilgangTilBedriften("00000001")).thenReturn(true);

        var resultat = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(TILFELDIG_PERSON_IDENT, førsteFraværsdag);

        assertThat(resultat).hasSize(1);
        var arbeidsforhold = resultat.getFirst();

        assertThat(arbeidsforhold.organisasjonsnummer()).isEqualTo("00000001");
        assertThat(arbeidsforhold.arbeidsforholdId()).isEqualTo("123456789");
    }

    @Test
    void returnerer_organisasjoner_innsender_har_tilgang_til() {
        var organisasjoner = List.of("000000000", "000000001", "000000002");
        var forventetListe = organisasjoner.stream().map(OrganisasjonsnummerDto::new).toList();

        when(altinnTilgangTjenesteMock.hentBedrifterArbeidsgiverHarTilgangTil()).thenReturn(organisasjoner);

        var resultat = arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil(TILFELDIG_PERSON_IDENT);

        assertThat(resultat)
            .hasSize(3)
            .isEqualTo(forventetListe);
    }
}
