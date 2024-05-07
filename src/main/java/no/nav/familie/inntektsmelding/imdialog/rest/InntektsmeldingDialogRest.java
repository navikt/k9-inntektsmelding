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

    private PersonTjeneste personTjeneste;

    @Inject
    public InntektsmeldingDialogRest(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @UtenAutentisering
    @Path(HENT_PERSONINFO)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter personinfo gitt aktørId", tags = "imdialog")
    public Response hentPersoninfo(@NotNull @Parameter(description = "AktørId for personen") @QueryParam("aktorId") @Valid AktørIdDto aktørIdDto,
                                   @NotNull @Parameter(description = "Ytelse som skal kobles til oppslaget") @QueryParam("ytelse") Ytelsetype ytelse){
        var aktørId = new AktørId(aktørIdDto.aktørId());
        PersonInfo personInfo = personTjeneste.hentPersonInfo(aktørId, ytelse);
        var dto = new PersonInfoDto(personInfo.navn(), personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getId());
        return Response.ok(dto).build();
    }
}
