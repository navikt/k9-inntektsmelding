package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import static no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.RefusjonOmsorgsdagerArbeidsgiverRest.BASE_PATH;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
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
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester.InnloggetBrukerTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(BASE_PATH)
public class RefusjonOmsorgsdagerArbeidsgiverRest {
    private static final Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerArbeidsgiverRest.class);

    public static final String BASE_PATH = "/refusjon-omsorgsdager-arbeidsgiver";
    private static final String SLÅ_OPP_ARBEIDSTAKER = "/arbeidstaker";
    private static final String INNLOGGET_BRUKER = "/innlogget-bruker";

    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;

    RefusjonOmsorgsdagerArbeidsgiverRest() {
        // CDI
    }

    @Inject
    public RefusjonOmsorgsdagerArbeidsgiverRest(ArbeidstakerTjeneste arbeidstakerTjeneste, InnloggetBrukerTjeneste innloggetBrukerTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.innloggetBrukerTjeneste = innloggetBrukerTjeneste;
    }

    @POST
    @Path(SLÅ_OPP_ARBEIDSTAKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om arbeidstaker, gitt et fødselsnummer.", tags = "imdialog")
    @Tilgangskontrollert
    public Response slåOppArbeidstaker(
        @Parameter(description = "Datapakke som inneholder fødselsnummeret til en arbeidstaker")
        @NotNull @Valid
        SlåOppArbeidstakerDto slåOppArbeidstakerDto
    ) {

        LOG.info("Slår opp arbeidstaker med fødselsnummer {}", slåOppArbeidstakerDto.fødselsnummer());

        var dto = arbeidstakerTjeneste.slåOppArbeidstaker(slåOppArbeidstakerDto.fødselsnummer(), slåOppArbeidstakerDto.ytelseType());
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @GET
    @Path(INNLOGGET_BRUKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om innlogget bruker.", tags = "imdialog")
    @Tilgangskontrollert
    public Response hentInnloggetBruker(
        @Parameter(description = "Hvilken ytelse den innloggete brukeren skal sende inn inntektsmelding for")
        @NotNull @QueryParam("ytelseType") @Valid Ytelsetype ytelseType,
        @Parameter(description = "Hvilken organisasjon den innloggete brukeren skal sende inn inntektsmelding for")
        @NotNull @Pattern(regexp = "^\\d{9}$") @Valid @QueryParam("organisasjonsnummer") String organisasjonsnummer
    ) {
        var dto = innloggetBrukerTjeneste.hentInnloggetBruker(ytelseType, organisasjonsnummer);
        return Response.ok(dto).build();
    }
}
