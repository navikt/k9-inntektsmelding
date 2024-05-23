package no.nav.familie.inntektsmelding.rest.arbeidsgivernotifikasjon;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

import java.time.LocalDateTime;

@Path(ArbeidsgiverNotifikasjonRest.BASE_PATH)
@ApplicationScoped
@Transactional
public class ArbeidsgiverNotifikasjonRest {
    public static final String BASE_PATH = "/oppgaver";

    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;

    @Inject
    public ArbeidsgiverNotifikasjonRest(ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon) {
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
    }

    ArbeidsgiverNotifikasjonRest() {
        //CDI
    }

    @POST
    @UtenAutentisering
    @Path("/opprett")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Oppretter en oppgave om inntektsmelding til arbeidsgiver", tags = "arbeidsgivernotifikasjon")
    public Response opprettOppgave(@NotNull @Valid OpprettOppgaveDto request) {
        var merkelapp = ArbeidsgiverNotifikasjonUtils.getMerkelapp(request.ytelse());

        var hh = arbeidsgiverNotifikasjon.opprettSak(request.saksnummer(), request.organisasjonsnummer(),
                ArbeidsgiverNotifikasjonUtils.lagSaksTittel(request.ytelse(), request.brukerID()), ArbeidsgiverNotifikasjonUtils.lagNotifikasjonsLenke(),
                merkelapp);

        var oppgaveID = arbeidsgiverNotifikasjon.opprettOppgave(request.saksnummer(), request.saksnummer(), request.organisasjonsnummer(),
                ArbeidsgiverNotifikasjonUtils.lagNotifikasjonstekst(request.ytelse()), ArbeidsgiverNotifikasjonUtils.lagNotifikasjonsLenke(), merkelapp);

        return Response.ok(new OppgaveResponse(oppgaveID)).build();
    }

    @POST
    @UtenAutentisering
    @Path("/lukk")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Lukker en oppgave om inntektsmelding til arbeidsgiver", tags = "arbeidsgivernotifikasjon")
    public Response lukkOppgave(@NotNull @Valid OpprettOppgaveDto request) {
        var lukketOppgave = arbeidsgiverNotifikasjon.lukkOppgave(request.saksnummer(), LocalDateTime.now());

        return Response.ok(new OppgaveResponse(lukketOppgave)).build();
    }

}
