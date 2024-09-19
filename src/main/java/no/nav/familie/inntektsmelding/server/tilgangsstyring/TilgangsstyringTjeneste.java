package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.Optional;
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
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
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
        var kontekst = KontekstHolder.getKontekst();
        if (!erTokenXProvider(kontekst)) {
            ikkeTilgang("Kun TokenX brukere støttes.");
        }

        var orgNrSet = Optional.of(forespørselUuid)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        if (orgNrSet.isEmpty()) {
            ikkeTilgang("Mangler informasjon om bedrift.");
        } else {
            for (var orgNr : orgNrSet) {
                if (!altinnTilgangTjeneste.harTilgangTilBedriften(orgNr)) {
                    SECURE_LOG.warn("Bruker {} mangler tilgang til bedrift {}", kontekst.getUid(), orgNr);
                    ikkeTilgang("Mangler tilgang til bedrift.");
                }
            }
        }
    }

    private boolean erTokenXProvider(Kontekst kontekst) {
        if (kontekst instanceof RequestKontekst requestKontekst) {
            var provider = requestKontekst.getToken().provider();
            return OpenIDProvider.TOKENX.equals(provider);
        }
        return false;
    }

    private static void ikkeTilgang(String begrunnelse) {
        LOG.info("Fikk ikke tilgang pga: {}", begrunnelse);
        throw new ManglerTilgangException("IM-00403", "Mangler tilgang til tjenesten.");
    }

}
