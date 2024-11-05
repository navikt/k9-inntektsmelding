package no.nav.familie.inntektsmelding.forespørsel.modell;

import no.nav.familie.inntektsmelding.database.JpaTestcontainerExtension;


import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.IntervallEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaTestcontainerExtension.class)
class ForespørselRepositoryTest extends EntityManagerAwareTest {

    private ForespørselRepository forespørselRepository;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
    }

    @Test
    void skal_teste_at_forespørsel_lagres_uten_perioder() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            Collections.emptyList());

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSøknadsperioder()).isEmpty();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEqualTo("123");
    }

    @Test
    void skal_teste_at_forespørsel_lagres_med_en_periode() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            Collections.singletonList(IntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10))));

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSøknadsperioder()).hasSize(1);
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getFom().equals(LocalDate.now()))).isTrue();
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getTom().equals(LocalDate.now().plusDays(10)))).isTrue();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEqualTo("123");
    }

    @Test
    void skal_teste_at_forespørsel_lagres_med_flere_perioder() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            List.of(IntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10)),
            IntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(11), LocalDate.now().plusDays(15))));

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSøknadsperioder()).hasSize(2);
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getFom().equals(LocalDate.now()))).isTrue();
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getTom().equals(LocalDate.now().plusDays(10)))).isTrue();
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getFom().equals(LocalDate.now().plusDays(11)))).isTrue();
        assertThat(hentet.getSøknadsperioder().stream().anyMatch(p -> p.getPeriode().getTom().equals(LocalDate.now().plusDays(15)))).isTrue();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEqualTo("123");
    }

}
