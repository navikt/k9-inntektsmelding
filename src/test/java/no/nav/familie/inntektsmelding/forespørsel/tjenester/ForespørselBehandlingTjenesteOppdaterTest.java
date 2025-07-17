package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith({JpaExtension.class, MockitoExtension.class})
class ForespørselBehandlingTjenesteOppdaterTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate STP = LocalDate.now().minusWeeks(4);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.OMSORGSPENGER;

    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjeneste(
            forespørselTjeneste,
            arbeidsgiverNotifikasjon,
            personTjeneste,
            prosessTaskTjeneste,
            organisasjonTjeneste
        );
    }

    @Test
    void skal_finne_forespørsel_som_skal_oppdateres_når_etterspurtePerioder_er_forskjellige() {
        List<PeriodeDto> eksisterendePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));
        List<PeriodeDto> nyePerioder = List.of(new PeriodeDto(STP, STP.plusDays(10)));

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);

        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).hasSize(1);
        assertThat(forespørslerSomSkalOppdateres.getFirst().oppdaterDto().etterspurtePerioder()).isEqualTo(nyePerioder);
    }

    @Test
    void skal_ikke_finne_forespørsel_som_skal_oppdateres_når_etterspurtePerioder_er_like() {
        List<PeriodeDto> perioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));

        var eksisterendeForespørsel = lagForespørselMedPerioder(perioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(perioder);

        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).isEmpty();
    }

    @Test
    void skal_ikke_finne_forespørsel_som_skal_oppdateres_når_kun_rekkefølgen_av_perioder_er_forskjellig() {
        List<PeriodeDto> eksisterendePerioder = List.of(
            new PeriodeDto(STP, STP.plusDays(5)),
            new PeriodeDto(STP.plusWeeks(2), STP.plusWeeks(3))
        );

        List<PeriodeDto> nyePerioder = List.of(
            new PeriodeDto(STP.plusWeeks(2), STP.plusWeeks(3)),
            new PeriodeDto(STP, STP.plusDays(5))
        );

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);

        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto),List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).isEmpty();
    }

    @Test
    void skal_finne_forespørsel_som_skal_oppdateres_når_det_er_flere_perioder_enn_før() {
        List<PeriodeDto> eksisterendePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));

        List<PeriodeDto> nyePerioder = List.of(
            new PeriodeDto(STP.plusWeeks(2), STP.plusWeeks(3)),
            new PeriodeDto(STP, STP.plusDays(5))
        );

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);

        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).hasSize(1);
    }

    @Test
    void skal_finne_forespørsel_som_skal_oppdateres_når_det_er_færre_perioder_enn_før() {
        List<PeriodeDto> eksisterendePerioder = List.of(
            new PeriodeDto(STP.plusWeeks(2), STP.plusWeeks(3)),
            new PeriodeDto(STP, STP.plusDays(5))
        );

        List<PeriodeDto> nyePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);
        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).hasSize(1);
        assertThat(forespørslerSomSkalOppdateres.getFirst().oppdaterDto().etterspurtePerioder()).isEqualTo(nyePerioder);
    }

    @Test
    void skal_finne_forespørsel_som_skal_oppdateres_når_eksisterende_har_perioder_men_ny_har_ingen() {
        List<PeriodeDto> eksisterendePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));
        List<PeriodeDto> nyePerioder = Collections.emptyList();

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);
        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).hasSize(1);
        assertThat(forespørslerSomSkalOppdateres.getFirst().oppdaterDto().etterspurtePerioder()).isEqualTo(nyePerioder);
    }

    @Test
    void skal_finne_forespørsel_som_skal_oppdateres_når_eksisterende_har_ingen_perioder_men_ny_har_perioder() {
        List<PeriodeDto> eksisterendePerioder = Collections.emptyList();
        List<PeriodeDto> nyePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);
        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).hasSize(1);
        assertThat(forespørslerSomSkalOppdateres.getFirst().oppdaterDto().etterspurtePerioder()).isEqualTo(nyePerioder);
    }

    @Test
    void skal_ikke_finne_forespørsel_som_skal_oppdateres_med_ugyldig_status() {
        List<PeriodeDto> eksisterendePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));
        List<PeriodeDto> nyePerioder = List.of(new PeriodeDto(STP, STP.plusDays(10)));

        // Sett status til FERDIG, som ikke skal gi treff
        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);
        eksisterendeForespørsel.setStatus(ForespørselStatus.FERDIG);

        var forespørslerDto = lagForespørselDtoMedStatusOpprett(nyePerioder);
        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).isEmpty();
    }

    @Test
    void skal_ikke_finne_forespørsel_som_skal_oppdateres_med_annen_aksjon_enn_opprett() {
        List<PeriodeDto> eksisterendePerioder = List.of(new PeriodeDto(STP, STP.plusDays(5)));
        List<PeriodeDto> nyePerioder = List.of(new PeriodeDto(STP, STP.plusDays(10)));

        var eksisterendeForespørsel = lagForespørselMedPerioder(eksisterendePerioder);

        // Bruk UTGÅTT som aksjon, som ikke skal gi treff
        var forespørslerDto = new OppdaterForespørselDto(STP,
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            ForespørselAksjon.UTGÅTT,
            nyePerioder);

        var forespørslerSomSkalOppdateres = forespørselBehandlingTjeneste.utledForespørslerSomSkalOppdateres(List.of(forespørslerDto), List.of(eksisterendeForespørsel));

        assertThat(forespørslerSomSkalOppdateres).isEmpty();
    }

    private ForespørselEntitet lagForespørselMedPerioder(List<PeriodeDto> perioder) {
        var forespørsel = ForespørselMapper.mapForespørsel(
            BRREG_ORGNUMMER,
            STP,
            AKTØR_ID,
            YTELSETYPE,
            SAKSNUMMMER,
            STP,
            perioder
        );
        forespørsel.setStatus(ForespørselStatus.UNDER_BEHANDLING);
        return forespørsel;
    }

    private static OppdaterForespørselDto lagForespørselDtoMedStatusOpprett(List<PeriodeDto> nyePerioder) {
        return new OppdaterForespørselDto(STP,
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            ForespørselAksjon.OPPRETT,
            nyePerioder
        );
    }
}
