package no.nav.familie.inntektsmelding.database.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.database.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class InnkommendeForespørselTjenesteTest {

    public static final String BRREG_ORGNUMMER = "974760673";
    public static final String SAK_ID = "1";
    public static final String OPPGAVE_ID = "2";

    @Inject
    private EntityManager entityManager;
    private final ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon = Mockito.mock(ArbeidsgiverNotifikasjon.class);
    private ForespørselRepository forespørselRepository;
    private InnkommendeForespørselTjeneste innkommendeForespørselTjeneste;


    @BeforeEach
    public void setUp() {
        this.forespørselRepository = new ForespørselRepository(entityManager);
        when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), any(), any())).thenReturn(SAK_ID);
        when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), any(), any(), any())).thenReturn(OPPGAVE_ID);
        this.innkommendeForespørselTjeneste = new InnkommendeForespørselTjeneste(
                new ForespørselTjenesteImpl(forespørselRepository), arbeidsgiverNotifikasjon);

    }

    @Test
    public void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        var skjæringstidspunkt = LocalDate.now();
        var aktørId = new AktørId("1234567891234");
        var saksnummer = "FAGSAK_SAKEN";
        innkommendeForespørselTjeneste.håndterInnkommendeForespørsel(skjæringstidspunkt, Ytelsetype.PLEIEPENGER_SYKT_BARN, aktørId,
                new Organisasjonsnummer(BRREG_ORGNUMMER), new FagsakSaksnummer(saksnummer));


        var lagret = forespørselRepository.hentForespørsler(new FagsakSaksnummer(saksnummer));

        assertThat(lagret.size()).isEqualTo(1);
        assertThat(lagret.get(0).getSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.get(0).getOppgaveId()).isEqualTo(OPPGAVE_ID);
    }
}
