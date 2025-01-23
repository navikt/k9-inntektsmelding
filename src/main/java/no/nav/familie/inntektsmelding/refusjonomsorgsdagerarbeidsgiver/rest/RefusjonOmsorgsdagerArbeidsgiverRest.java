package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import static no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.RefusjonOmsorgsdagerArbeidsgiverRest.BASE_PATH;

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
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
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
    private PersonTjeneste personTjeneste;

    RefusjonOmsorgsdagerArbeidsgiverRest() {
        // CDI
    }

    @Inject
    public RefusjonOmsorgsdagerArbeidsgiverRest(ArbeidstakerTjeneste arbeidstakerTjeneste, PersonTjeneste personTjeneste, InnloggetBrukerTjeneste innloggetBrukerTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.innloggetBrukerTjeneste = innloggetBrukerTjeneste;
        this.personTjeneste = personTjeneste;
    }

    @POST
    @Path(SLÅ_OPP_ARBEIDSTAKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om arbeidstaker, gitt et fødselsnummer.", tags = "imdialog")
    @Tilgangskontrollert
    public Response slåOppArbeidstaker(
        @Parameter(description = "Datapakke som inneholder fødselsnummeret til en arbeidstaker")
        @NotNull @Valid
        SlåOppArbeidstakerRequestDto slåOppArbeidstakerRequestDto
    ) {

        LOG.info("Slår opp arbeidstaker med fødselsnummer {}", slåOppArbeidstakerRequestDto.fødselsnummer());

        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(slåOppArbeidstakerRequestDto.fødselsnummer(),
            LocalDate.now());
        var personInfo = personTjeneste.hentPersonFraIdent(slåOppArbeidstakerRequestDto.fødselsnummer(), slåOppArbeidstakerRequestDto.ytelseType());
        if (arbeidsforhold.isEmpty() || personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = new SlåOppArbeidstakerResponseDto(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforhold);
        return Response.ok(dto).build();
    }

    @POST
    @Path(INNLOGGET_BRUKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter opplysninger om innlogget bruker.", tags = "imdialog")
    @Tilgangskontrollert
    public Response hentInnloggetBruker(
        @Parameter(description = "Datapakke som inneholder ytelsestypen og organisasjonsnummeret til den innloggede brukeren")
        @NotNull @Valid HentInnloggetBrukerRequestDto hentInnloggetBrukerRequestDto
    ) {
        var dto = innloggetBrukerTjeneste.hentInnloggetBruker(hentInnloggetBrukerRequestDto.ytelseType(), hentInnloggetBrukerRequestDto.organisasjonsnummer());
        return Response.ok(dto).build();
    }
}
