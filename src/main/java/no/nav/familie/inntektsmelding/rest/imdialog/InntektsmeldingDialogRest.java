package no.nav.familie.inntektsmelding.rest.imdialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@Path(InntektsmeldingDialogRest.BASE_PATH)
@ApplicationScoped
@Transactional
public class InntektsmeldingDialogRest {
    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_PERSONINFO = "/personinfo";
    private static final String HENT_ORGANISASJON = "/organisasjon";
    private static final String HENT_INNTEKT = "/inntekt";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";

    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;

    @Inject
    public InntektsmeldingDialogRest(PersonTjeneste personTjeneste, OrganisasjonTjeneste organisasjonTjeneste, InntektTjeneste inntektTjeneste) {
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    InntektsmeldingDialogRest() {
        // CDI
    }

    @GET
    @UtenAutentisering
    @Path(HENT_PERSONINFO)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter personinfo gitt aktørId", tags = "imdialog")
    public Response hentPersoninfo(@NotNull @QueryParam("aktorId") @Valid AktørIdRequestDto aktørIdRequestDto,
                                   @NotNull @QueryParam("ytelse") @Valid Ytelsetype ytelse) {
        var aktørId = new AktørId(aktørIdRequestDto.aktørId());
        PersonInfo personInfo = personTjeneste.hentPersonInfo(aktørId, ytelse);
        var dto = new PersonInfoResponseDto(personInfo.navn(), personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getId());
        return Response.ok(dto).build();
    }

    @GET
    @UtenAutentisering
    @Path(HENT_ORGANISASJON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter organisasjonsnavn gitt organisasjonsnummer", tags = "imdialog")
    public Response hentOrganisasjon(@NotNull @Parameter(description = "Organisasjonsnummer") @QueryParam("organisasjonsnummer") @Valid OrganisasjonsnummerRequestDto organisasjonsnummerRequestDto) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummerRequestDto.organisasjonsnummer());
        var organisassjonInfoDto = organisasjon.map(o -> new OrganisasjonInfoResponseDto(o.navn(), o.orgnr()));
        return organisassjonInfoDto.map(oi -> Response.ok(organisassjonInfoDto).build()).orElse(Response.noContent().build());
    }

    @GET
    @UtenAutentisering
    @Path(HENT_INNTEKT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter inntekt siste tre måneder for en aktør", tags = "imdialog")
    public Response hentInntekt(@Parameter(description = "Request for å hente inntekt, hvis startdato er null brukes dagens dato") @NotNull HentInntektRequestDto hentInntektRequestDto) {
        var startdato = hentInntektRequestDto.startdato == null ? LocalDate.now() : hentInntektRequestDto.startdato;
        var aktørId = new AktørId(hentInntektRequestDto.aktorId().aktørId());
        var inntekt = inntektTjeneste.hentInntekt(aktørId, startdato, hentInntektRequestDto.organisasjonsnummer().organisasjonsnummer());
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
        return Response.ok(sendInntektsmeldingRequestDto).build();
    }

    public record AktørIdRequestDto(
        @JsonValue @NotNull @Pattern(regexp = VALID_REGEXP, message = "aktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String aktørId) {
        private static final String VALID_REGEXP = "^\\d{13}$";
    }

    public record PersonInfoResponseDto(@NotNull String navn, @NotNull String fødselsnummer, @NotNull String aktørId) {
    }

    public record OrganisasjonsnummerRequestDto(
        @JsonValue @NotNull @Pattern(regexp = VALID_REGEXP, message = "organisasjonsnummer ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String organisasjonsnummer) {
        private static final String VALID_REGEXP = "^\\d{9}$";
    }

    public record OrganisasjonInfoResponseDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {
    }

    public record HentInntektRequestDto(@NotNull @QueryParam("aktorId") AktørIdRequestDto aktorId, @NotNull @QueryParam("ytelse") Ytelsetype ytelse,
                                        @NotNull @QueryParam("organisasjonsnummer") @Valid OrganisasjonsnummerRequestDto organisasjonsnummer,
                                        LocalDate startdato) {
    }

    public record MånedsinntektResponsDto(LocalDate fom, LocalDate tom, BigDecimal beløp, String organisasjonsnummer) {
    }

    public record RefusjonsperiodeRequestDto(@NotNull LocalDate fom, LocalDate tom,
                                             @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record NaturalytelseBortfaltRequestDto(@NotNull LocalDate fom, LocalDate tom, @NotNull Naturalytelsetype naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record SendInntektsmeldingRequestDto(@NotNull @Valid AktørIdRequestDto aktorId, @NotNull @Valid Ytelsetype ytelse,
                                                @NotNull String arbeidsgiverIdent, @NotNull String telefonnummer, @NotNull LocalDate startdato,
                                                @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                                @NotNull List<@Valid RefusjonsperiodeRequestDto> refusjonsperioder,
                                                @NotNull List<@Valid NaturalytelseBortfaltRequestDto> bortfaltNaturaltytelsePerioder) {
    }
}
