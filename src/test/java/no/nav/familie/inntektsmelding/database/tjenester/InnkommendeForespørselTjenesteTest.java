package no.nav.familie.inntektsmelding.database.tjenester;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;
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
    private final PersonTjeneste personTjeneste = Mockito.mock(PersonTjeneste.class);
    private ForespørselRepository forespørselRepository;
    private InnkommendeForespørselTjeneste innkommendeForespørselTjeneste;


    @BeforeEach
    public void setUp() {
        this.forespørselRepository = new ForespørselRepository(entityManager);
        this.innkommendeForespørselTjeneste = new InnkommendeForespørselTjeneste(new ForespørselTjenesteImpl(forespørselRepository),
            arbeidsgiverNotifikasjon, personTjeneste);

    }

    @Test
    public void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        var skjæringstidspunkt = LocalDate.now();
        var aktørId = new AktørIdDto("1234567891234");
        var ytelsetype = Ytelsetype.PLEIEPENGER_SYKT_BARN;
        var saksnummer = "FAGSAK_SAKEN";

        var personInfo = new PersonInfo("Navn Navnesen", new PersonIdent("01019100000"), new AktørIdDto("1111111111111"),
            LocalDate.of(1991, 1, 1).minusYears(30));

        var saksTittel = innkommendeForespørselTjeneste.lagSaksTittel(personInfo);


        when(personTjeneste.hentPersonInfo(aktørId, ytelsetype)).thenReturn(personInfo);
        when(arbeidsgiverNotifikasjon.opprettSak(any(), any(), eq(BRREG_ORGNUMMER), eq(saksTittel), any())).thenReturn(SAK_ID);
        when(arbeidsgiverNotifikasjon.opprettOppgave(any(), any(), any(), eq(BRREG_ORGNUMMER), any(), any())).thenReturn(OPPGAVE_ID);

        innkommendeForespørselTjeneste.håndterInnkommendeForespørsel(skjæringstidspunkt, ytelsetype, aktørId,
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER), new SaksnummerDto(saksnummer));

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(saksnummer));

        assertThat(lagret.size()).isEqualTo(1);
        assertThat(lagret.getFirst().getSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.getFirst().getOppgaveId()).isEqualTo(OPPGAVE_ID);
    }
}
