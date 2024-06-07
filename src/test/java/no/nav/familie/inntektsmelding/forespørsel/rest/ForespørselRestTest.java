package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class ForespørselRestTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";

    private ForespørselRest forespørselRest;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;


    @BeforeEach
    void setUp() {
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
        verify(forespørselBehandlingTjeneste).håndterInnkommendeForespørsel(eq(LocalDate.now()), eq(Ytelsetype.PLEIEPENGER_SYKT_BARN), eq(new AktørIdEntitet(aktørId.id())),
            eq(orgnummer), eq(fagsakSaksnummer));
    }

    @Test
    void serdes_rerosepørsel_mapper() {
        var expectedOrg = "123456789";
        var expectedBruker = "123342532424";
        var expectedSkjæringstidspunkt = LocalDate.now();
        var input = new ForespørselEntitet(expectedOrg, expectedSkjæringstidspunkt, new AktørIdEntitet(expectedBruker), Ytelsetype.FORELDREPENGER, "9876544321");

        var resultat = ForespørselRest.mapTilDto(input);

        assertThat(resultat).isNotNull().isInstanceOf(ForespørselRest.ForespørselDto.class);
        assertThat(resultat.organisasjonsnummer()).isEqualTo(new OrganisasjonsnummerDto(expectedOrg));
        assertThat(resultat.skjæringstidspunkt()).isEqualTo(expectedSkjæringstidspunkt);
        assertThat(resultat.brukerAktørId()).isEqualTo(new AktørIdDto(expectedBruker));
        assertThat(resultat.ytelseType()).isEqualTo(YtelseTypeDto.FORELDREPENGER);
        assertThat(resultat.uuid()).isNotNull();
    }

    @Test
    void serdes() {
        var expectedOrg = new OrganisasjonsnummerDto("123456789");
        var expectedBruker = new AktørIdDto("123342532424");
        var expectedSkjæringstidspunkt = LocalDate.now();
        var dto = new ForespørselRest.ForespørselDto(UUID.randomUUID(), expectedOrg, expectedSkjæringstidspunkt, expectedBruker, YtelseTypeDto.SVANGERSKAPSPENGER);

        var ser = DefaultJsonMapper.toJson(dto);
        var des = DefaultJsonMapper.fromJson(ser, ForespørselRest.ForespørselDto.class);


        assertThat(ser).contains(expectedOrg.orgnr(), expectedBruker.id(), expectedSkjæringstidspunkt.toString());
        assertThat(des).isEqualTo(dto);
    }
}
