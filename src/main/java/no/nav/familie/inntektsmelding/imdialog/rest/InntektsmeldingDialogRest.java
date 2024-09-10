package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.UUID;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingDialogTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.authz.TilgangsstyringInputTyper;
import no.nav.familie.inntektsmelding.server.authz.api.ActionType;
import no.nav.familie.inntektsmelding.server.authz.api.PolicyType;
import no.nav.familie.inntektsmelding.server.authz.api.Tilgangsstyring;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInput;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInputSupplier;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
@AutentisertMedTokenX
public class InntektsmeldingDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogRest.class);

    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_GRUNNLAG = "/grunnlag";
    private static final String HENT_INNTEKTSMELDINGER_FOR_OPPGAVE = "/inntektsmeldinger";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";
    private static final String LAST_NED_PDF = "/last-ned-pdf";
    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;

    InntektsmeldingDialogRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingDialogRest(InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste) {
        this.inntektsmeldingDialogTjeneste = inntektsmeldingDialogTjeneste;
    }

    @GET
    @Path(HENT_GRUNNLAG)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet.", tags = "imdialog")
    @Tilgangsstyring(policy = PolicyType.ARBEIDSGIVER, action = ActionType.READ)
    public Response hentInnsendingsinfo(
        @Parameter(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet basert på en forespørsel UUID") @NotNull
        @QueryParam("foresporselUuid")
        @TilgangsstyringInputSupplier(ForespørselIdSupplier.class) UUID forespørselUuid) {
        LOG.info("Henter grunnlag for forespørsel " + forespørselUuid);
        var dto = inntektsmeldingDialogTjeneste.lagDialogDto(forespørselUuid);
        return Response.ok(dto).build();
    }

    @GET
    @Path(HENT_INNTEKTSMELDINGER_FOR_OPPGAVE)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter alle inntektsmeldinger som er sendt inn for en forespørsel", tags = "imdialog")
    @Tilgangsstyring(policy = PolicyType.ARBEIDSGIVER, action = ActionType.READ)
    public Response hentInntektsmeldingerForOppgave(
        @Parameter(description = "Henter alle inntektsmeldinger som er sendt inn for en forespørsel") @NotNull
        @QueryParam("foresporselUuid") UUID forespørselUuid) {
        LOG.info("Henter inntektsmeldinger for forespørsel " + forespørselUuid);
        var dto = inntektsmeldingDialogTjeneste.hentInntektsmeldinger(forespørselUuid);
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sender inn inntektsmelding", tags = "imdialog")
    @Tilgangsstyring(policy = PolicyType.ARBEIDSGIVER, action = ActionType.WRITE)
    public Response sendInntektsmelding(@Parameter(description = "Datapakke med informasjon om inntektsmeldingen") @NotNull @Valid
                                        @TilgangsstyringInputSupplier(ForespørselIdSupplier.class)
                                        SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        LOG.info("Mottok inntektsmelding for forespørsel " + sendInntektsmeldingRequestDto.foresporselUuid());
        inntektsmeldingDialogTjeneste.mottaInntektsmelding(sendInntektsmeldingRequestDto);
        return Response.ok(sendInntektsmeldingRequestDto).build();
    }

    @GET
    @Path(LAST_NED_PDF)
    @Produces("application/pdf")
    @Operation(description = "Lager PDF av inntektsmelding", tags = "imdialog")
    @Tilgangsstyring(policy = PolicyType.ARBEIDSGIVER, action = ActionType.READ)
    public Response lastNedPDF(
        @Parameter(description = "id for inntektsmelding å lage PDF av") @NotNull
        @QueryParam("id") Long id) {
        LOG.info("Henter inntektsmelding for id " + id);
        var pdf = inntektsmeldingDialogTjeneste.hentPDF(id);

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=dokument.pdf");
        return responseBuilder.build();
    }

    public static class ForespørselIdSupplier implements Function<Object, TilgangsstyringInput> {
        @Override
        public TilgangsstyringInput apply(Object obj) {
            var uuid = (UUID) obj;
            return TilgangsstyringInput.opprett().leggTil(TilgangsstyringInputTyper.FORESPORSEL_ID, uuid);
        }
    }
}
