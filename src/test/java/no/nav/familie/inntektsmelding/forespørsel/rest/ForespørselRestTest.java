package no.nav.familie.inntektsmelding.forespørsel.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
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

        var saksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.oppdaterForespørsler(
            new OppdaterForespørslerRequest(aktørId, forespørsler, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, saksnummer));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    }

    @Test
    void skal_gi_400_for_oppdater_med_duplikate_forespørsler() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp1 = LocalDate.now();
        var stp2 = LocalDate.now().minusMonths(2);
        var aktørId = new AktørIdDto("1234567890134");

        var forespørsler = List.of(new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT, null),
            new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT, null));

        var saksnummer = new SaksnummerDto("SAK");
        var response = forespørselRest.oppdaterForespørsler(
            new OppdaterForespørslerRequest(aktørId, forespørsler, YtelseTypeDto.PLEIEPENGER_SYKT_BARN, saksnummer));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    }

    @Test
    void skal_gi_valideringsfeil_for_OppdaterForespørslerRequest_for_omsorgspenger_uten_etterspurte_perioder() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp1 = LocalDate.now();
        var forespørselDto = new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT, null);

        var forespørsler = List.of(forespørselDto);
        var aktørId = new AktørIdDto("1234567890134");
        var saksnummer = new SaksnummerDto("SAK");

        var request = new OppdaterForespørslerRequest(aktørId, forespørsler, YtelseTypeDto.OMSORGSPENGER, saksnummer);

        // Validate the request
        Set<ConstraintViolation<OppdaterForespørslerRequest>> violations = validator.validate(request);

        Assertions.assertTrue(violations != null && !violations.isEmpty()); // Assert validation errors exist
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Hvis ytelsestype er omsorgspenger, må etterspurtePerioder være med");
    }

    @Test
    void skal_gi_valideringsfeil_for_OppdaterForespørselDto_med_duplikate_etterspurte_perioder() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp1 = LocalDate.now();
        var etterspurtePerioder = List.of(
            new PeriodeDto(stp1, stp1.plusDays(10)),
            new PeriodeDto(stp1, stp1.plusDays(10) // Duplicate periods
            ));

        var forespørselDto = new OppdaterForespørselDto(stp1, orgnummer, ForespørselAksjon.OPPRETT, etterspurtePerioder);

        // Validate the DTO
        Set<ConstraintViolation<OppdaterForespørselDto>> violations = validator.validate(forespørselDto);

        Assertions.assertTrue(violations != null && !violations.isEmpty()); // Assert validation errors exist
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Hvis etterspurtePerioder finnes kan den ikke inneholde duplikate perioder");
    }

    @Test
    void hent_skal_filtrere_vekk_duplikate_forespørsler_1() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp = LocalDate.now();
        var aktørId = new AktørIdDto("1234567890134");

        var forespørsel1 = ForespørselMapper.mapForespørsel(orgnummer.orgnr(), stp, aktørId.id(), Ytelsetype.PLEIEPENGER_SYKT_BARN, "SAK", stp);
        forespørsel1.setStatus(ForespørselStatus.UTGÅTT);
        var forespørsel2 = ForespørselMapper.mapForespørsel(orgnummer.orgnr(), stp, aktørId.id(), Ytelsetype.PLEIEPENGER_SYKT_BARN, "SAK", stp);
        forespørsel2.setStatus(ForespørselStatus.FERDIG);

        when(forespørselBehandlingTjeneste.hentForespørslerForFagsak(any(SaksnummerDto.class), eq(null), eq(null))).thenReturn(List.of(forespørsel1, forespørsel2));

        var response = forespørselRest.hentForespørslerForSak("SAK");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        List<ForespørselResponse> forespørselResponses = (List<ForespørselResponse>) response.getEntity();

        assertThat(forespørselResponses).isNotNull();
        assertThat(forespørselResponses.size()).isEqualTo(1);
        assertThat(forespørselResponses.getFirst().status()).isEqualTo(ForespørselStatus.FERDIG);
    }

    @Test
    void hent_skal_filtrere_vekk_duplikate_forespørsler_2() {
        var orgnummer = new OrganisasjonsnummerDto(BRREG_ORGNUMMER);
        var stp = LocalDate.now();
        var aktørId = new AktørIdDto("1234567890134");

        var forespørsel1 = ForespørselMapper.mapForespørsel(orgnummer.orgnr(), stp, aktørId.id(), Ytelsetype.PLEIEPENGER_SYKT_BARN, "SAK", stp);
        forespørsel1.setStatus(ForespørselStatus.UTGÅTT);
        var forespørsel2 = ForespørselMapper.mapForespørsel(orgnummer.orgnr(), stp, aktørId.id(), Ytelsetype.PLEIEPENGER_SYKT_BARN, "SAK", stp);
        forespørsel2.setStatus(ForespørselStatus.UTGÅTT);

        when(forespørselBehandlingTjeneste.hentForespørslerForFagsak(any(SaksnummerDto.class), eq(null), eq(null))).thenReturn(List.of(forespørsel1, forespørsel2));

        var response = forespørselRest.hentForespørslerForSak("SAK");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        List<ForespørselResponse> forespørselResponses = (List<ForespørselResponse>) response.getEntity();

        assertThat(forespørselResponses).isNotNull();
        assertThat(forespørselResponses.size()).isEqualTo(1);
        assertThat(forespørselResponses.getFirst().status()).isEqualTo(ForespørselStatus.UTGÅTT);
    }

    @Test
    void serdes_forespørsel_mapper() {
        var expectedOrg = "123456789";
        var expectedBruker = "1233425324241";
        var expectedSkjæringstidspunkt = LocalDate.now();
        var input = ForespørselMapper.mapForespørsel(expectedOrg, expectedSkjæringstidspunkt, expectedBruker, Ytelsetype.PLEIEPENGER_SYKT_BARN, "9876544321", expectedSkjæringstidspunkt.plusDays(10));

        var resultat = ForespørselRest.mapTilForespørselResponse(input);

        assertThat(resultat).isNotNull().isInstanceOf(ForespørselResponse.class);
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
        var expectedEtterspurtePerioder = List.of(new PeriodeDto(expectedSkjæringstidspunkt, expectedSkjæringstidspunkt.plusDays(10)));
        var dto = new ForespørselResponse(UUID.randomUUID(), expectedOrg, expectedSkjæringstidspunkt, expectedBruker,
            YtelseTypeDto.OMSORGSPENGER, ForespørselStatus.UNDER_BEHANDLING, expectedEtterspurtePerioder);

        var ser = DefaultJsonMapper.toJson(dto);
        var des = DefaultJsonMapper.fromJson(ser, ForespørselResponse.class);


        assertThat(ser).contains(expectedOrg.orgnr(), expectedBruker.id(), expectedSkjæringstidspunkt.toString());
        assertThat(ser).contains(expectedEtterspurtePerioder.getFirst().fom().toString(), expectedEtterspurtePerioder.getFirst().tom().toString());
        assertThat(des).isEqualTo(dto);
    }
}
