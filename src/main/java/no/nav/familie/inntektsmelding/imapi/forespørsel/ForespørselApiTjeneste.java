package no.nav.familie.inntektsmelding.imapi.forespørsel;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;

@ApplicationScoped
public class ForespørselApiTjeneste {
    private PersonTjeneste personTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselApiTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselApiTjeneste(PersonTjeneste personTjeneste,
                                  ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.personTjeneste = personTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    public Optional<ForespørselDto> hentForesørselDto(UUID forespørselUuid) {

        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .map(forespørsel -> {
                var personIdent = personTjeneste.finnPersonIdentForAktørId(forespørsel.getAktørId());
                return mapTilResponseDto(forespørsel, personIdent);
            });
    }

    public List<ForespørselDto> hentForespørslerDto(ArbeidsgiverDto arbeidsgiverDto,
                                                    FødselsnummerDto fnrDto,
                                                    ForespørselStatusDto statusDto,
                                                    YtelseTypeDto ytelseTypeDto,
                                                    LocalDate fom,
                                                    LocalDate tom) {
        AktørIdEntitet aktørId = fnrDto != null
                      ? personTjeneste.finnAktørIdForPersonIdent(fnrDto.fnr()).orElse(null)
                      : null;
        ForespørselStatus status = statusDto != null ? mapForespørselStatus(statusDto) : null;
        Ytelsetype ytelsetype = ytelseTypeDto != null ? mapYtelsetype(ytelseTypeDto) : null;

        List<ForespørselEntitet> forespørsler = forespørselBehandlingTjeneste.hentForespørsler(arbeidsgiverDto, aktørId, status, ytelsetype, fom, tom);

        Set<AktørIdEntitet> aktørIder = forespørsler.stream().map(ForespørselEntitet::getAktørId).collect(Collectors.toSet());
        Map<AktørIdEntitet, PersonIdent> aktørIdPersonIdentMap = personTjeneste.finnPersonIdentForAktørIdBolk(aktørIder);

        return forespørsler.stream()
            .map(f -> mapTilResponseDto(f, aktørIdPersonIdentMap.get(f.getAktørId())))
            .toList();
    }

    private ForespørselDto mapTilResponseDto(ForespørselEntitet forespørsel, PersonIdent personIdent) {
        if (personIdent == null) {
            throw new IllegalArgumentException("Finner ikke fødselsnummer for aktørId " + forespørsel.getAktørId());
        }

        List<PeriodeDto> etterspurtePerioder = forespørsel.getEtterspurtePerioder().stream()
            .map(p -> new PeriodeDto(p.fom(), p.tom()))
            .toList();

        return new ForespørselDto(forespørsel.getUuid(),
            new OrganisasjonsnummerDto(forespørsel.getOrganisasjonsnummer()),
            new FødselsnummerDto(personIdent.getIdent()),
            forespørsel.getSkjæringstidspunkt(),
            mapYtelsetype(forespørsel.getYtelseType()),
            mapForespørselStatus(forespørsel.getStatus()),
            etterspurtePerioder,
            forespørsel.getOpprettetTidspunkt());
    }

    static YtelseTypeDto mapYtelsetype(Ytelsetype ytelseType) {
        return switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
        };
    }

    static Ytelsetype mapYtelsetype(YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
        };
    }

    static ForespørselStatusDto mapForespørselStatus(ForespørselStatus status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselStatusDto.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatusDto.FERDIG;
            case UTGÅTT -> ForespørselStatusDto.UTGÅTT;
        };
    }

    static ForespørselStatus mapForespørselStatus(ForespørselStatusDto statusDto) {
        return switch (statusDto) {
            case UNDER_BEHANDLING -> ForespørselStatus.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatus.FERDIG;
            case UTGÅTT -> ForespørselStatus.UTGÅTT;
        };
    }
}
