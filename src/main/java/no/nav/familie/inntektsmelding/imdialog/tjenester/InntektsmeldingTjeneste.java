package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private K9DokgenTjeneste k9DokgenTjeneste;

    InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository,
                                   K9DokgenTjeneste k9DokgenTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        var inntektsmeldinger = forespørsel.getInntektsmeldinger();

        return inntektsmeldinger
            .stream()
            .map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørsel.getUuid()))
            .toList();
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldingerForÅr(AktørIdEntitet aktørId,
                                                                       String arbeidsgiverIdent,
                                                                       int år,
                                                                       Ytelsetype ytelsetype) {

        var forespørsler = forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, arbeidsgiverIdent);

        // dersom det ikke finnes noen forespørsler er det ikke noe for GUI å vise
        if (forespørsler.isEmpty()) {
            return List.of();
        }

        // da denne koden er kun for GUI så er det ikke så viktig at forespørsel stemmer
        var førsteForespørsel = forespørsler.stream().findFirst();

        var inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldingerForÅr(aktørId,
            arbeidsgiverIdent,
            år,
            ytelsetype);

        return inntektsmeldinger.stream()
            .map(im -> InntektsmeldingMapper.mapFraEntitet(im, førsteForespørsel.get().getUuid()))
            .toList();
    }

    public byte[] hentPDF(long id) {
        var inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(id);
        return k9DokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet);
    }
}
