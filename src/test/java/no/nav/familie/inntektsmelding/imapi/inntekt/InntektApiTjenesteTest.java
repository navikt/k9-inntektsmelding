package no.nav.familie.inntektsmelding.imapi.inntekt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.Kjønn;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ExtendWith(MockitoExtension.class)
class InntektApiTjenesteTest {
    private static final String ORGNR = "999999999";
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567890123");
    private static final PersonInfo PERSON_INFO = new PersonInfo("Ola", "Kari","Nordmann", new PersonIdent("12345678901"), new AktørIdEntitet(AKTØR_ID.getAktørId()), LocalDate.of(1990, 1, 1), "99887766", Kjønn.MANN);

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;

    private InntektApiTjeneste inntektApiTjeneste;

    @BeforeEach
    void setUp() {
        inntektApiTjeneste = new InntektApiTjeneste(forespørselBehandlingTjeneste, personTjeneste, inntektTjeneste);
    }

    @Test
    void skal_returnere_tomt_resultat_når_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.empty());

        var resultat = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_hente_inntekt_og_mappe_til_dto() {
        ForespørselEntitet forespørsel = lagForespørsel();

        var forespørselUuid = UUID.randomUUID();
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(PERSON_INFO);

        var måned1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(30000), YearMonth.of(2025, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var måned2 = new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2025, 4), MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT);

        var inntektsopplysninger = new Inntektsopplysninger(BigDecimal.valueOf(30000), ORGNR, List.of(måned1, måned2));
        when(inntektTjeneste.hentInntekt(ArgumentMatchers.eq(PERSON_INFO), ArgumentMatchers.eq(STARTDATO), ArgumentMatchers.any(),
            ArgumentMatchers.eq(ORGNR), ArgumentMatchers.eq(Ytelsetype.PLEIEPENGER_SYKT_BARN))).thenReturn(inntektsopplysninger);

        var resultat = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        assertThat(resultat).isPresent();
        assertThat(resultat.get().gjennomsnitt()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(resultat.get().inntektPerMåned()).containsEntry(YearMonth.of(2025, 3), BigDecimal.valueOf(30000));
        assertThat(resultat.get().inntektPerMåned()).containsEntry(YearMonth.of(2025, 4), null);
    }

    @Test
    void skal_returnere_tomt_resultat_når_inntekt_ikke_er_rapportert_grunnet_nedetid() {
        var forespørselUuid = UUID.randomUUID();
        ForespørselEntitet forespørsel = lagForespørsel();

        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID)).thenReturn(PERSON_INFO);

        var månedUnderNedetid = new Inntektsopplysninger.InntektMåned(null, YearMonth.of(2025, 3), MånedslønnStatus.NEDETID_AINNTEKT);
        var inntektsopplysninger = new Inntektsopplysninger(null, ORGNR, List.of(månedUnderNedetid));
        when(inntektTjeneste.hentInntekt(ArgumentMatchers.eq(PERSON_INFO), ArgumentMatchers.eq(STARTDATO), ArgumentMatchers.any(),
            ArgumentMatchers.eq(ORGNR), ArgumentMatchers.eq(Ytelsetype.PLEIEPENGER_SYKT_BARN))).thenReturn(inntektsopplysninger);

        var resultat = inntektApiTjeneste.hentInntektDto(forespørselUuid);

        assertThat(resultat).isEmpty();
    }

    private ForespørselEntitet lagForespørsel() {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(ORGNR)
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
    }
}
