package no.nav.familie.inntektsmelding.forepørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.database.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.database.tjenester.ForespørselTjenesteImpl;
import no.nav.familie.inntektsmelding.database.tjenester.InnkommendeForespørselTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaExtension.class)
public class ForespørselRestTjenesteTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private final PersonInfo personMock = new PersonInfo("Navn Navnesen", new PersonIdent("01019100000"), new AktørIdDto("1111111111111"),
        LocalDate.of(1991, 01, 01).minusYears(30));

    private ForespørselRepository forespørselRepository;
    private ForespørselRestTjeneste forespørselRestTjeneste;


    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        ArbeidsgiverNotifikasjon agTjeneste = Mockito.mock(ArbeidsgiverNotifikasjon.class);
        PersonTjeneste personTjeneste = Mockito.mock(PersonTjeneste.class);
        this.forespørselRestTjeneste = new ForespørselRestTjeneste(
            new InnkommendeForespørselTjeneste(new ForespørselTjenesteImpl(forespørselRepository), agTjeneste, personTjeneste), new ForespørselTjenesteImpl());
        when(personTjeneste.hentPersonInfo(any(), any())).thenReturn(personMock);
        when(agTjeneste.opprettSak(any(), any(), any(), any(), any())).thenReturn("1");
        when(agTjeneste.opprettOppgave(any(), any(), any(), any(), any(), any())).thenReturn("2");
    }

    @Test
    void skal_opprette_forespørsel() {
        var fagsakSaksnummer = new SaksnummerDto("SAK");
        forespørselRestTjeneste.opprettForespørsel(
            new OpprettForespørselRequest(new AktørIdDto("1234567890134"), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), LocalDate.now(),
                YtelseTypeDto.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer));

        var forespørsler = forespørselRepository.hentForespørsler(fagsakSaksnummer);

        assertThat(forespørsler.size()).isEqualTo(1);
    }
}
