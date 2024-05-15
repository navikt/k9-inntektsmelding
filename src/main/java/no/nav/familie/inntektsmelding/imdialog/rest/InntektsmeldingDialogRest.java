package no.nav.familie.inntektsmelding.imdialog.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
public class InntektsmeldingDialogRest {
    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_PERSONINFO = "/personinfo";
    private static final String HENT_ORGANISASJON = "/organisasjon";

    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;

    @Inject
    public InntektsmeldingDialogRest(PersonTjeneste personTjeneste, OrganisasjonTjeneste organisasjonTjeneste) {
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @UtenAutentisering
    @Path(HENT_PERSONINFO)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter personinfo gitt aktørId", tags = "imdialog")
    public Response hentPersoninfo(@QueryParam("aktorId") @Valid AktørIdDto aktørIdDto,
                                   @NotNull @QueryParam("ytelse") Ytelsetype ytelse){
        var aktørId = new AktørId(aktørIdDto.aktørId());
        PersonInfo personInfo = personTjeneste.hentPersonInfo(aktørId, ytelse);
        var dto = new PersonInfoDto(personInfo.navn(), personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getId());
        return Response.ok(dto).build();
    }

    @GET
    @UtenAutentisering
    @Path(HENT_ORGANISASJON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter organisasjonsnavn gitt organisasjonsnummer", tags = "imdialog")
    public Response hentOrganisasjon(@NotNull @Parameter(description = "Organisasjonsnummer") @QueryParam("organisasjonsnummer") @Valid OrganisasjonsnummerDto organisasjonsnummerDto ){
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummerDto.organisasjonsnummer());
        var organisassjonInfoDto = organisasjon.map( o -> new OrganisasjonInfoDto(o.navn(), o.orgnr()));

        return organisassjonInfoDto.map(oi -> Response.ok(organisassjonInfoDto).build()).orElse(Response.noContent().build());
    }
}
