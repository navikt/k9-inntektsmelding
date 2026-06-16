package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiRestTest {
    private static final String ORGNUMMER = "974760673";
    private static final String FNR = "11111111111";

    private InntektsmeldingApiRest inntektsmeldingApiRest;
    @Mock
    private Tilgang tilgang;
    @Mock
    private InntektsmeldingApiTjeneste inntektsmeldingApiTjeneste;
    @Mock
    private InntektsmeldingApiMottakTjeneste mottakTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;

    @BeforeEach
    void setUp() {
        inntektsmeldingApiRest = new InntektsmeldingApiRest(inntektsmeldingApiTjeneste, mottakTjeneste, personTjeneste, tilgang);
    }

    @Test
    void skal_hente_inntektsmelding() {
        var inntektsmeldingUuid = UUID.randomUUID();
        var forventetDto = lagInntektsmeldingDto(inntektsmeldingUuid, UUID.randomUUID());
        when(inntektsmeldingApiTjeneste.hentInntektsmelding(inntektsmeldingUuid)).thenReturn(Optional.of(forventetDto));

        var response = inntektsmeldingApiRest.hentInntektsmelding(inntektsmeldingUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.getEntity()).isEqualTo(forventetDto);
        verify(inntektsmeldingApiTjeneste).hentInntektsmelding(inntektsmeldingUuid);
    }

    @Test
    void skal_returnere_404_når_inntektsmelding_ikke_finnes() {
        var inntektsmeldingUuid = UUID.randomUUID();
        when(inntektsmeldingApiTjeneste.hentInntektsmelding(inntektsmeldingUuid)).thenReturn(Optional.empty());

        var response = inntektsmeldingApiRest.hentInntektsmelding(inntektsmeldingUuid);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
    }

    @Test
    void skal_sende_inntektsmelding_ok() {
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();
        var aktørId = new AktørIdEntitet("1234567890123");
        var request = lagSendInntektsmeldingRequest(forespørselUuid);
        var forventetSvar = new SendInntektsmeldingResponse(true, inntektsmeldingUuid, null);

        when(personTjeneste.finnAktørIdForPersonIdent(FNR)).thenReturn(Optional.of(aktørId));
        when(mottakTjeneste.mottaInntektsmelding(request, aktørId)).thenReturn(forventetSvar);

        var svar = inntektsmeldingApiRest.sendInntektsmelding(request);

        assertThat(svar.success()).isTrue();
        assertThat(svar.inntektsmeldingUuid()).isEqualTo(inntektsmeldingUuid);
        verify(mottakTjeneste).mottaInntektsmelding(request, aktørId);
    }

    @Test
    void skal_returnere_feil_ved_ingen_aktørId() {
        var forespørselUuid = UUID.randomUUID();
        var request = lagSendInntektsmeldingRequest(forespørselUuid);

        when(personTjeneste.finnAktørIdForPersonIdent(FNR)).thenReturn(Optional.empty());

        var svar = inntektsmeldingApiRest.sendInntektsmelding(request);

        assertThat(svar.success()).isFalse();
        assertThat(svar.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.INGEN_AKTØR_ID);
    }

    @Test
    void skal_hente_inntektsmeldinger_med_filter() {
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();
        var filterRequest = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(ORGNUMMER), null, null, null, null, null);
        var forventetDto = lagInntektsmeldingDto(inntektsmeldingUuid, forespørselUuid);
        when(inntektsmeldingApiTjeneste.hentInntektsmeldinger(filterRequest)).thenReturn(List.of(forventetDto));

        var response = inntektsmeldingApiRest.hentInntektsmeldinger(filterRequest);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(((HentInntektsmeldingerResponse) response.getEntity()).inntektsmeldinger()).asList().containsExactly(forventetDto);
        verify(inntektsmeldingApiTjeneste).hentInntektsmeldinger(filterRequest);
    }

    private InntektsmeldingDto lagInntektsmeldingDto(UUID inntektsmeldingUuid, UUID forespørselUuid) {
        return new InntektsmeldingDto(
            inntektsmeldingUuid,
            forespørselUuid,
            new FødselsnummerDto(FNR),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new OrganisasjonsnummerDto(ORGNUMMER),
            new KontaktpersonDto("Kontakt Person", "99999999"),
            LocalDate.now(),
            new BigDecimal("50000"),
            LocalDateTime.now(),
            null,
            null,
            new AvsenderSystemDto("NAV_NO", "1.0"),
            List.of(),
            List.of(),
            List.of(),
            null
        );
    }

    private SendInntektsmeldingRequest lagSendInntektsmeldingRequest(UUID forespørselUuid) {
        return new SendInntektsmeldingRequest(
            forespørselUuid,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNUMMER),
            LocalDate.now(),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new KontaktpersonDto("Kontakt Person", "99999999"),
            new BigDecimal("50000"),
            List.of(),
            List.of(),
            List.of(),
            null,
            null
        );
    }
}
