package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import static no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.RefusjonOmsorgsdagerRest.BASE_PATH;

import java.time.LocalDate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.RefusjonOmsorgsdagerService;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(BASE_PATH)
public class RefusjonOmsorgsdagerRest {
    private static final Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerRest.class);

    public static final String BASE_PATH = "/refusjon-omsorgsdager";
    private static final String SLÅ_OPP_ARBEIDSTAKER = "/arbeidstaker";
    private static final String INNLOGGET_BRUKER = "/innlogget-bruker";
    private static final String INNTEKTSOPPLYSNINGER = "/inntektsopplysninger";

    private RefusjonOmsorgsdagerService refusjonOmsorgsdagerService;

    RefusjonOmsorgsdagerRest() {
        // CDI
    }

    @Inject
    public RefusjonOmsorgsdagerRest(RefusjonOmsorgsdagerService refusjonOmsorgsdagerService) {
        this.refusjonOmsorgsdagerService = refusjonOmsorgsdagerService;
    }

    @POST
    @Path(SLÅ_OPP_ARBEIDSTAKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om arbeidstaker, gitt et fødselsnummer.", tags = "imdialog")
    @Tilgangskontrollert
    public Response slåOppArbeidstaker(
        @Parameter(description = "Datapakke som inneholder fødselsnummeret til en arbeidstaker")
        @NotNull @Valid
        SlåOppArbeidstakerRequestDto dto
    ) {
        var response = refusjonOmsorgsdagerService.hentArbeidstaker(dto.fødselsnummer());
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(response).build();
    }


    @POST
    @Path(INNLOGGET_BRUKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om innlogget bruker.", tags = "imdialog")
    @Tilgangskontrollert
    public Response hentInnloggetBruker(
        @Parameter(description = "Datapakke som inneholder ytelsestypen og organisasjonsnummeret til den innloggede brukeren")
        @NotNull @Valid HentInnloggetBrukerRequestDto dto
    ) {
        var response = refusjonOmsorgsdagerService.hentInnloggetBruker(dto.organisasjonsnummer());
        return Response.ok(response).build();
    }


    @POST
    @Path(INNTEKTSOPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om inntekt, gitt et fødselsnummer og skjæringstidspunkt.", tags = "imdialog")
    @Tilgangskontrollert
    public Response hentInntektsopplysninger(
        @Parameter(description = "Datapakke som inneholder fødselsnummeret og skjæringstidspunktet til en arbeidstaker")
        @NotNull @Valid HentInntektsopplysningerRequestDto dto
    ) {
        var inntektsopplysninger = refusjonOmsorgsdagerService.hentInntektsopplysninger(dto.fødselsnummer(), dto.organisasjonsnummer(), LocalDate.parse(dto.skjæringstidspunkt()));

        if (inntektsopplysninger == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(inntektsopplysninger).build();
    }
}
