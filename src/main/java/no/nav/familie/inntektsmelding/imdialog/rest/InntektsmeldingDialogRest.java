package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingDialogTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
public class InntektsmeldingDialogRest {
    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_PERSONINFO = "/personinfo";
    private static final String HENT_ORGANISASJON = "/organisasjon";
    private static final String HENT_INNTEKT = "/inntekt";
    private static final String HENT_GRUNNLAG = "/grunnlag";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";

    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;

    @Inject
    public InntektsmeldingDialogRest(PersonTjeneste personTjeneste,
                                     OrganisasjonTjeneste organisasjonTjeneste,
                                     InntektTjeneste inntektTjeneste,
                                     InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste) {
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.inntektsmeldingDialogTjeneste = inntektsmeldingDialogTjeneste;
    }

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @UtenAutentisering
    @Path(HENT_GRUNNLAG)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet.", tags = "imdialog")
    public Response hentInnsendingsinfo(@Parameter(description = "Henter et grunnlag av all data vi har om søker, inntekt og arbeidsforholdet basert på en forespørsel UUID") @NotNull @QueryParam("foresporselUuid") UUID forespørselUuid) {
        var dto = inntektsmeldingDialogTjeneste.lagDialogDto(forespørselUuid);
        return Response.ok(dto).build();
    }

    @GET
    @UtenAutentisering
    @Path(HENT_PERSONINFO)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter personinfo gitt id", tags = "imdialog")
    public Response hentPersoninfo(@NotNull @QueryParam("aktorId") @Valid AktørIdDto aktørIdRequestDto,
                                   @NotNull @QueryParam("ytelse") @Valid YtelseTypeDto ytelse) {
        PersonInfo personInfo = personTjeneste.hentPersonInfo(new AktørIdEntitet(aktørIdRequestDto.id()), KodeverkMapper.mapYtelsetype(ytelse));
        var dto = new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        return Response.ok(dto).build();
    }

    @GET
    @UtenAutentisering
    @Path(HENT_ORGANISASJON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter organisasjonsnavn gitt organisasjonsnummer", tags = "imdialog")
    public Response hentOrganisasjon(@NotNull @Parameter(description = "Organisasjonsnummer") @QueryParam("organisasjonsnummer") @Valid OrganisasjonsnummerDto organisasjonsnummer) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjonOptional(organisasjonsnummer.orgnr());
        var organisassjonInfoDto = organisasjon.map(o -> new OrganisasjonInfoResponseDto(o.navn(), o.orgnr()));
        return organisassjonInfoDto.map(oi -> Response.ok(organisassjonInfoDto).build()).orElse(Response.noContent().build());
    }

    @POST
    @UtenAutentisering
    @Path(HENT_INNTEKT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter inntekt siste tre måneder for en aktør", tags = "imdialog")
    public Response hentInntekt(@Parameter(description = "Request for å hente inntekt, hvis startdato er null brukes dagens dato") @NotNull HentInntektRequestDto hentInntektRequestDto) {
        var startdato = hentInntektRequestDto.startdato == null ? LocalDate.now() : hentInntektRequestDto.startdato;
        var aktørId = new AktørIdDto(hentInntektRequestDto.aktorId().id());
        var inntekt = inntektTjeneste.hentInntekt(new AktørIdEntitet(aktørId.id()), startdato, hentInntektRequestDto.arbeidsgiverIdent().ident());
        return Response.ok(inntekt.stream()
            .map(i -> new MånedsinntektResponsDto(i.måned().atDay(1), i.måned().atEndOfMonth(), i.beløp(), i.organisasjonsnummer()))
            .toList()).build();
    }

    @POST
    @UtenAutentisering
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sender inn inntektsmelding", tags = "imdialog")
    public Response sendInntektsmelding(@Parameter(description = "Datapakke med informasjon om inntektsmeldingen") @NotNull @Valid SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        inntektsmeldingDialogTjeneste.mottaInntektsmelding(sendInntektsmeldingRequestDto);
        return Response.ok(sendInntektsmeldingRequestDto).build();
    }

    public record OrganisasjonInfoResponseDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {
    }

    public record HentInntektRequestDto(@NotNull @QueryParam("aktorId") AktørIdDto aktorId, @NotNull @QueryParam("ytelse") YtelseTypeDto ytelse,
                                        @NotNull @QueryParam("arbeidsgiverIdent") @Valid ArbeidsgiverDto arbeidsgiverIdent, LocalDate startdato) {
    }

    public record MånedsinntektResponsDto(LocalDate fom, LocalDate tom, BigDecimal beløp, String organisasjonsnummer) {
    }
}
