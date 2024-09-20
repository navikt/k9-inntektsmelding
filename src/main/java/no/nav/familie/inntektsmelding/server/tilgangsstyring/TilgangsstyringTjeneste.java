package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

@Dependent
public class TilgangsstyringTjeneste implements Tilgang {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangsstyringTjeneste.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final AltinnTilgangTjeneste altinnTilgangTjeneste;
    private final PipTjeneste pipTjeneste;

    @Inject
    public TilgangsstyringTjeneste(PipTjeneste pipTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.pipTjeneste = pipTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    @Override
    public void sjekkOmArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid) {
        sjekkErBorgerInisjertKall();

        var orgNrSet = Optional.of(forespørselUuid)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkOmArbeidsgiverHarTilgangTilBedrift(long inntektsmeldingId) {
        sjekkErBorgerInisjertKall();

        var orgNrSet = Optional.of(inntektsmeldingId)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    private void sjekkBorgersAltinnTilgangTilOrganisasjon(Set<String> organisasjoner) {
        if (organisasjoner.isEmpty()) {
            ikkeTilgang("Mangler informasjon om bedrift.");
        } else {
            for (var orgNr : organisasjoner) {
                if (!altinnTilgangTjeneste.harTilgangTilBedriften(orgNr)) {
                    SECURE_LOG.warn("Bruker mangler tilgang til bedrift {}", orgNr);
                    ikkeTilgang("Mangler tilgang til bedrift.");
                }
            }
        }
    }

    private void sjekkErBorgerInisjertKall() {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq) {
            if (OpenIDProvider.TOKENX.equals(rq.getToken().provider())) {
                return;
            }
        }
        ikkeTilgang("Kun TokenX brukere støttes.");
    }

    private static void ikkeTilgang(String begrunnelse) {
        LOG.info("Fikk ikke tilgang pga: {}", begrunnelse);
        throw new ManglerTilgangException("IM-00403", "Mangler tilgang til tjenesten.");
    }

}
