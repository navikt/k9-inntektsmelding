package no.nav.familie.inntektsmelding.forespørsel.modell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.DelvisFraværsDagDto;
import no.nav.familie.inntektsmelding.typer.dto.FraværsPeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.OmsorgspengerDataDto;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaExtension.class)
class ForespørselRepositoryTest extends EntityManagerAwareTest {

    private ForespørselRepository forespørselRepository;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
    }

    @Test
    void skal_teste_at_forespørsel_lagres_uten_første_uttak() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "9999999999999",
            "999999999",
            "123",
            null);

        clearHibernateCache(); // Fjerne hibernate cachen før assertions skal evalueres

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.PLEIEPENGER_SYKT_BARN);
        assertThat(hentet.getSaksnummer().get()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isEmpty();
    }

    @Test
    void skal_teste_at_forespørsel_lagres_med_første_uttaksdato() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "9999999999999",
            "999999999",
            "123",
            LocalDate.now());

        clearHibernateCache(); // Fjerne hibernate cachen før assertions skal evalueres

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.PLEIEPENGER_SYKT_BARN);
        assertThat(hentet.getSaksnummer().get()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isPresent();
        assertThat(hentet.getFørsteUttaksdato()).contains(LocalDate.now());
    }

    @Test
    void skal_lagre_forespørsel_med_omsorgspenger() {
        var begrunnelse = "Begrunnelse for søknad";
        var fraværsPeriode = new FraværsPeriodeDto(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
        var delvisFraværsDag = new DelvisFraværsDagDto(LocalDate.now().minusDays(3), BigDecimal.valueOf(4.5), BigDecimal.valueOf(7.5));

        var omsorgspengerData = new OmsorgspengerDataDto(begrunnelse, List.of(fraværsPeriode), List.of(delvisFraværsDag));
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.OMSORGSPENGER,
            "9999999999999",
            "999999999",
            "123",
            null,
            omsorgspengerData);

        clearHibernateCache(); // Fjerne hibernate cachen før assertions skal evalueres

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.OMSORGSPENGER);
        assertThat(hentet.getSaksnummer().get()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isEmpty();
        assertThat(hentet.getOmsorgspenger()).isNotNull();
        assertThat(hentet.getOmsorgspenger().getBegrunnelseForSøknad()).isEqualTo(begrunnelse);
        assertThat(hentet.getOmsorgspenger().getFraværsPerioder()).hasSize(1);
        assertThat(hentet.getOmsorgspenger().getFraværsPerioder().get(0).getPeriode().getFom()).isEqualTo(fraværsPeriode.fom());
        assertThat(hentet.getOmsorgspenger().getFraværsPerioder().get(0).getPeriode().getTom()).isEqualTo(fraværsPeriode.tom());
        assertThat(hentet.getOmsorgspenger().getDelvisFraværsDager()).hasSize(1);
        assertThat(hentet.getOmsorgspenger().getDelvisFraværsDager().get(0).getDato()).isEqualTo(delvisFraværsDag.dato());
        assertThat(hentet.getOmsorgspenger().getDelvisFraværsDager().get(0).getFraværstimer()).isEqualByComparingTo(delvisFraværsDag.fraværstimer());
        assertThat(hentet.getOmsorgspenger().getDelvisFraværsDager().get(0).getNormalArbeidstid()).isEqualByComparingTo(delvisFraværsDag.normalArbeidstid());
    }

    @Test
    void skal_kaste_feil_dersom_et_obligatorisk_felt_mangler() {
        assertThrows(NullPointerException.class,
            () -> forespørselRepository.lagreForespørsel(LocalDate.now(),
                Ytelsetype.PLEIEPENGER_SYKT_BARN,
                null,
                "999999999",
                "123",
                null));

        Exception e1 = assertThrows(IllegalArgumentException.class,
            () -> forespørselRepository.lagreForespørsel(LocalDate.now(),
                Ytelsetype.PLEIEPENGER_SYKT_BARN,
                "9999999999999",
                "999999999",
                null,
                null));
        assertThat(e1.getMessage()).contains("Mangler obligatoriske felt(er) for å bygge ForespørselEntitet");

        Exception e2 = assertThrows(IllegalArgumentException.class,
            () -> forespørselRepository.lagreForespørsel(LocalDate.now(),
                Ytelsetype.PLEIEPENGER_SYKT_BARN,
                "9999999999999",
                null,
                "123",
                null));
        assertThat(e2.getMessage()).contains("Mangler obligatoriske felt(er) for å bygge ForespørselEntitet");

        Exception e3 = assertThrows(IllegalArgumentException.class,
            () -> forespørselRepository.lagreForespørsel(LocalDate.now(),
                null,
                "9999999999999",
                "999999999",
                "123",
                null));

        assertThat(e3.getMessage()).contains("Mangler obligatoriske felt(er) for å bygge ForespørselEntitet");

        Exception e4 = assertThrows(IllegalArgumentException.class,
            () -> forespørselRepository.lagreForespørsel(null,
                Ytelsetype.PLEIEPENGER_SYKT_BARN,
                "9999999999999",
                "999999999",
                "123",
                null));

        assertThat(e4.getMessage()).contains("Mangler obligatoriske felt(er) for å bygge ForespørselEntitet");
    }

    @Test
    void skal_feile_for_omsorgspenger_med_annen_ytelse_type() {
        var begrunnelse = "Begrunnelse for søknad";
        var fraværsPeriode = new FraværsPeriodeDto(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
        var delvisFraværsDag = new DelvisFraværsDagDto(LocalDate.now().minusDays(3), BigDecimal.valueOf(4.5), BigDecimal.valueOf(7.5));
        var omsorgspengerData = new OmsorgspengerDataDto(begrunnelse, List.of(fraværsPeriode), List.of(delvisFraværsDag));

        Exception e4 = assertThrows(IllegalArgumentException.class,
            () -> forespørselRepository.lagreForespørsel(LocalDate.now(),
                Ytelsetype.PLEIEPENGER_SYKT_BARN,
                "9999999999999",
                "999999999",
                "123",
                null,
                omsorgspengerData));

        assertThat(e4.getMessage()).contains("OmsorgspengerForespørselEntitet skal kun settes for ytelseType Omsorgspenger, ikke for yteleseType:");
    }

    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }
}
