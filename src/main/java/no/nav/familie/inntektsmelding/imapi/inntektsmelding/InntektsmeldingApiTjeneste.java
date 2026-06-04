package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static no.nav.familie.inntektsmelding.imapi.inntektsmelding.InntektsmeldingApiMapper.mapYtelsetype;

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
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;

@ApplicationScoped
public class InntektsmeldingApiTjeneste {

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private PersonTjeneste personTjeneste;

    InntektsmeldingApiTjeneste() {
        // CDI
    }

    @Inject
    public InntektsmeldingApiTjeneste(InntektsmeldingRepository inntektsmeldingRepository,
                                      ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                      PersonTjeneste personTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.personTjeneste = personTjeneste;
    }

    public Optional<InntektsmeldingDto> hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.hentInntektsmeldingForUuid(inntektsmeldingUuid)
            .map(entitet -> {
                var personIdent = personTjeneste.finnPersonIdentForAktørId(entitet.getAktørId());
                return InntektsmeldingApiMapper.mapFraEntitet(entitet, personIdent);
            });
    }

    public List<InntektsmeldingDto> hentInntektsmeldinger(HentInntektsmeldingerRequest request) {
        if (request.forespørselUuid() != null) {
            return hentInntektsmeldingerForForespørsel(request.forespørselUuid());
        }
        return hentInntektsmeldingerFraFilter(request);
    }

    private List<InntektsmeldingDto> hentInntektsmeldingerForForespørsel(UUID forespørselUuid) {
        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .map(ForespørselEntitet::getInntektsmeldinger)
            .orElse(List.of())
            .stream()
            .map(inntektsmelding -> {
                var personIdent = personTjeneste.finnPersonIdentForAktørId(inntektsmelding.getAktørId());
                return InntektsmeldingApiMapper.mapFraEntitet(inntektsmelding, personIdent);
            })
            .toList();
    }

    private List<InntektsmeldingDto> hentInntektsmeldingerFraFilter(HentInntektsmeldingerRequest request) {
        AktørIdEntitet aktørId = request.fnr() != null
            ? personTjeneste.finnAktørIdForPersonIdent(request.fnr().fnr()).orElse(null)
            : null;
        Ytelsetype ytelsetype = request.ytelseType() != null ? mapYtelsetype(request.ytelseType()) : null;
        List<InntektsmeldingEntitet> inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(
            request.orgnr().orgnr(), aktørId, ytelsetype, request.fom(), request.tom());

        Set<AktørIdEntitet> aktørIder = inntektsmeldinger.stream().map(im -> im.getAktørId()).collect(Collectors.toSet());
        Map<AktørIdEntitet, PersonIdent> aktørIdPersonIdentMap = personTjeneste.finnPersonIdentForAktørIdBolk(aktørIder);

        return inntektsmeldinger.stream()
            .map(im -> InntektsmeldingApiMapper.mapFraEntitet(im, aktørIdPersonIdentMap.get(im.getAktørId())))
            .toList();
    }
}
