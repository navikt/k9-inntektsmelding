package no.nav.familie.inntektsmelding.forepørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.database.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.database.tjenester.ForespørselTjenesteImpl;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.FagsakSaksnummer;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaExtension.class)
public class ForespørselRestTjenesteTest extends EntityManagerAwareTest {

    public static final String BRREG_ORGNUMMER = "974760673";
    private ForespørselRepository forespørselRepository;
    private ForespørselRestTjeneste forespørselRestTjeneste;


    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselRestTjeneste = new ForespørselRestTjeneste(new ForespørselTjenesteImpl(forespørselRepository));
    }

    @Test
    void skal_opprette_forespørsel() {
        var fagsakSaksnummer = new FagsakSaksnummer("SAK");
        forespørselRestTjeneste.opprettForespørsel(
            new OpprettForespørselRequest(new AktørId("123456789"), new Organisasjonsnummer(BRREG_ORGNUMMER), LocalDate.now(),
                Ytelsetype.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer));

        var forespørsler = forespørselRepository.hentForespørsler(fagsakSaksnummer);

        assertThat(forespørsler.size()).isEqualTo(1);
    }
}
