package no.nav.familie.inntektsmelding.imapi.forespørsel;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørslerResponse;

@ExtendWith(MockitoExtension.class)
class ForespørselApiRestTest {
    private static final String ORGNUMMER = "974760673";

    private ForespørselApiRest forespørselApiRest;
    @Mock
    private Tilgang tilgang;
    @Mock
    private ForespørselApiTjeneste forespørselApiTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselApiRest = new ForespørselApiRest(forespørselApiTjeneste, tilgang);
    }

    @Test
    void skal_hente_forespørsel() {
        var forespørselUuid = UUID.randomUUID();
        var forventetDto = lagForespørselDto(forespørselUuid);
        when(forespørselApiTjeneste.hentForesørselDto(forespørselUuid)).thenReturn(Optional.of(forventetDto));

        var response = forespørselApiRest.hentForespørsel(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetDto);
        verify(forespørselApiTjeneste).hentForesørselDto(forespørselUuid);
    }

    @Test
    void skal_returnere_404_når_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(forespørselApiTjeneste.hentForesørselDto(forespørselUuid)).thenReturn(Optional.empty());

        var response = forespørselApiRest.hentForespørsel(forespørselUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    void skal_hente_forespørsler() {
        var forespørselUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(forespørselUuid);
        var filterRequest = new HentForespørselerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, null, null, null);
        when(forespørselApiTjeneste.hentForespørslerDto(new ArbeidsgiverDto(ORGNUMMER), null, null, null, null, null))
            .thenReturn(List.of(forespørselDto));

        var response = forespørselApiRest.hentForespørsler(filterRequest);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(((HentForespørslerResponse) response.getEntity()).forespørsler()).asList().containsExactly(forespørselDto);
    }

    private ForespørselDto lagForespørselDto(UUID uuid) {
        return new ForespørselDto(uuid,
            new OrganisasjonsnummerDto(ORGNUMMER),
            new FødselsnummerDto("11111111111"),
            LocalDate.now(),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            ForespørselStatusDto.UNDER_BEHANDLING,
            List.of(),
            LocalDateTime.now());
    }
}
