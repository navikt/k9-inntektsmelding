package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FagsystemTjenesteTest {

    @Mock
    private FpsakKlient fpKlient;

    private FagsystemTjeneste fagsystemTjeneste;

    @BeforeEach
    void setUp() {
        fagsystemTjeneste = new FagsystemTjeneste(fpKlient);
    }

    @Test
    public void tester_ingen_respons_fpsak() {
        var saksnummer = "123";
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER,
            saksnummer);
        when(fpKlient.hentSøkersFravær(saksnummer)).thenReturn(List.of());

        var fravær = fagsystemTjeneste.hentSøkersFraværsperioder(forespørsel);

        assertThat(fravær).isEmpty();
        verify(fpKlient, Mockito.times(1)).hentSøkersFravær(saksnummer);
    }

    @Test
    public void tester_respons_fpsak() {
        var saksnummer = "123";
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.FORELDREPENGER,
            saksnummer);
        when(fpKlient.hentSøkersFravær(saksnummer)).thenReturn(List.of(new SøkersFraværsperiode(LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 15)),
            new SøkersFraværsperiode(LocalDate.of(2024, 11, 1), LocalDate.of(2024, 10, 15))));

        var fravær = fagsystemTjeneste.hentSøkersFraværsperioder(forespørsel);

        assertThat(fravær).hasSize(2);
        assertThat(fravær.stream().anyMatch(p -> p.fom().equals(LocalDate.of(2024, 10, 1)))).isTrue();
        assertThat(fravær.stream().anyMatch(p -> p.fom().equals(LocalDate.of(2024, 11, 1)))).isTrue();
        verify(fpKlient, Mockito.times(1)).hentSøkersFravær(saksnummer);
    }

    @Test
    public void fpsak_skal_ikke_kalles_for_k9ytelser() {
        var saksnummer = "123";
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"), Ytelsetype.PLEIEPENGER_SYKT_BARN,
            saksnummer);

        var fravær = fagsystemTjeneste.hentSøkersFraværsperioder(forespørsel);

        assertThat(fravær).isEmpty();
        verify(fpKlient, Mockito.times(0)).hentSøkersFravær(saksnummer);
    }

}
