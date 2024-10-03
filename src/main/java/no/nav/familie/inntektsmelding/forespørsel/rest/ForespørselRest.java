package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;

import io.micrometer.core.instrument.Tag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);
    public static final String BASE_PATH = "/foresporsel";
    private static final String COUNTER_FORESPØRRSEL = "ftinntektsmelding.opppgaver";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    public ForespørselRest() {
    }

    @Inject
    public ForespørselRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                           ForespørselTjeneste forespørselTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @POST
    @Path("/opprett")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        LOG.info("Mottok forespørsel om inntektsmeldingoppgave på saksnummer " + request.saksnummer());
        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(), KodeverkMapper.mapYtelsetype(request.ytelsetype()),
            new AktørIdEntitet(request.aktørId().id()), new OrganisasjonsnummerDto(request.orgnummer().orgnr()), request.saksnummer());
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag("ytelse", request.ytelsetype().name()));
        Metrics.counter(COUNTER_FORESPØRRSEL, tags).increment();
        return Response.ok().build();
    }

    @POST
    @Path("/lukk")
    public Response lukkForespørsel(LukkForespørselRequest request) {
        LOG.info("Lukk forespørsel for saksnummer {} med orgnummer {} og skjæringstidspunkt {}", request.saksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        forespørselBehandlingTjeneste.lukkForespørsel(request.saksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    record ForespørselDto(UUID uuid, OrganisasjonsnummerDto organisasjonsnummer, LocalDate skjæringstidspunkt, AktørIdDto brukerAktørId,
                          YtelseTypeDto ytelseType) {
    }

    static ForespørselDto mapTilDto(ForespørselEntitet entitet) {
        return new ForespørselDto(entitet.getUuid(), new OrganisasjonsnummerDto(entitet.getOrganisasjonsnummer()), entitet.getSkjæringstidspunkt(),
            new AktørIdDto(entitet.getAktørId().getAktørId()), KodeverkMapper.mapYtelsetype(entitet.getYtelseType()));
    }
}

