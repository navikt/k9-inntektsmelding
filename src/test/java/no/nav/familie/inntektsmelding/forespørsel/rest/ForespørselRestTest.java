package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
class ForespørselRestTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String ORGNUMMER_TEST = "450674427";

    private ForespørselRest forespørselRest;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private Tilgang tilgang;

    @BeforeEach
    void setUp() {
        this.forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
        this.forespørselRest = new ForespørselRest(forespørselBehandlingTjeneste, tilgang);
    }

    @Test
    void skal_oppdatere_forespørsler() {
        var orgnummer1 = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var orgnummer2 = new OrganisasjonsnummerDto(ORGNUMMER_TEST);
        var stp1 = LocalDate.now();
        var stp2 = LocalDate.now().minusMonths(2);
        var aktørId = new AktørIdDto("1234567890134");

        var forespørsler = List.of(new OppdaterForespørselDto(stp1, orgnummer1, ForespørselAksjon.OPPRETT),
            new OppdaterForespørselDto(stp1, orgnummer2, ForespørselAksjon.OPPRETT),
            new OppdaterForespørselDto(stp2, orgnummer1, ForespørselAksjon.OPPRETT),
            new OppdaterForespørselDto(stp2, orgnummer2, ForespørselAksjon.OPPRETT));

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.oppdaterForespørsler(
            new OppdaterForespørslerRequest(aktørId, forespørsler, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    }

    @Test
    void skal_gi_400_for_oppdater_med_duplikate_forespørsler() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp1 = LocalDate.now();
        var stp2 = LocalDate.now().minusMonths(2);
        var aktørId = new AktørIdDto("1234567890134");

        var forespørsler = List.of(new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT),
            new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT));

        var fagsakSaksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.oppdaterForespørsler(
            new OppdaterForespørslerRequest(aktørId, forespørsler, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, fagsakSaksnummer));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    void serdes_forespørsel_mapper() {
        var expectedOrg = "123456789";
        var expectedBruker = "1233425324241";
        var expectedSkjæringstidspunkt = LocalDate.now();
        var input = new ForespørselEntitet(expectedOrg, expectedSkjæringstidspunkt, new AktørIdEntitet(expectedBruker), Ytelsetype.PLEIEPENGER_SYKT_BARN,
            "9876544321", expectedSkjæringstidspunkt.plusDays(10));

        var resultat = ForespørselRest.mapTilDto(input);

        assertThat(resultat).isNotNull().isInstanceOf(ForespørselRest.ForespørselDto.class);
        assertThat(resultat.organisasjonsnummer()).isEqualTo(new OrganisasjonsnummerDto(expectedOrg));
        assertThat(resultat.skjæringstidspunkt()).isEqualTo(expectedSkjæringstidspunkt);
        assertThat(resultat.brukerAktørId()).isEqualTo(new AktørIdDto(expectedBruker));
        assertThat(resultat.ytelseType()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(resultat.uuid()).isNotNull();
    }

    @Test
    void serdes() {
        var expectedOrg = new OrganisasjonsnummerDto("123456789");
        var expectedBruker = new AktørIdDto("123342532424");
        var expectedSkjæringstidspunkt = LocalDate.now();
        var dto = new ForespørselRest.ForespørselDto(UUID.randomUUID(), expectedOrg, expectedSkjæringstidspunkt, expectedBruker,
            YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE, ForespørselStatus.UNDER_BEHANDLING);

        var ser = DefaultJsonMapper.toJson(dto);
        var des = DefaultJsonMapper.fromJson(ser, ForespørselRest.ForespørselDto.class);


        assertThat(ser).contains(expectedOrg.orgnr(), expectedBruker.id(), expectedSkjæringstidspunkt.toString());
        assertThat(des).isEqualTo(dto);
    }
}
