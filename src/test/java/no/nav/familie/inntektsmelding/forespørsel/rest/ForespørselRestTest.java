package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaExtension.class)
public class ForespørselRestTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private final PersonInfo personMock = new PersonInfo("Navn Navnesen", new PersonIdent("01019100000"), new AktørIdDto("1111111111111"),
        LocalDate.of(1991, 01, 01).minusYears(30));

    private ForespørselRepository forespørselRepository;
    private ForespørselRest forespørselRest;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;


    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
        doNothing().when(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(any(), any(), any(), any(), any());
        this.forespørselRest = new ForespørselRest(forespørselBehandlingTjeneste, new ForespørselTjeneste());
    }

    @Test
    void skal_opprette_forespørsel() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var aktørId = new AktørIdDto("1234567890134");

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.createForespørsel(
            new OpprettForespørselRequest(aktørId, orgnummer, LocalDate.now(), YtelseTypeDto.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(eq(LocalDate.now()), eq(Ytelsetype.PLEIEPENGER_SYKT_BARN), eq(aktørId),
            eq(orgnummer), eq(fagsakSaksnummer));
    }
}
