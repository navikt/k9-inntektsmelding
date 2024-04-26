package no.nav.familie.inntektsmelding.imdialog.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
public class InntektsmeldingDialogRest {
    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_PERSONINFO = "/personinfo";

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @UtenAutentisering
    @Path(HENT_PERSONINFO)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter personinfo gitt aktørId", tags = "imdialog")
    public Response hentPersoninfo(@NotNull @Parameter(description = "AktørId for personen") @QueryParam("aktorId") @Valid AktørIdDto aktørId) {
        var responseBuilder = Response.ok();
        return responseBuilder.build();
    }
}
