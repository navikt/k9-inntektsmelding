package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class FagsystemTjeneste {
    private static Set<Ytelsetype> FPSAK_YTELSER = Set.of(Ytelsetype.FORELDREPENGER, Ytelsetype.SVANGERSKAPSPENGER);
    private static Set<Ytelsetype> K9SAK_YTELSER = Set.of(Ytelsetype.OMSORGSPENGER, Ytelsetype.OPPLÆRINGSPENGER, Ytelsetype.PLEIEPENGER_SYKT_BARN, Ytelsetype.PLEIEPENGER_NÆRSTÅENDE);
    private FpsakKlient fpKlient;
    private K9sakKlient k9Klient;

    FagsystemTjeneste() {
        // CDI
    }

    @Inject
    public FagsystemTjeneste(FpsakKlient fpKlient) {
        this.fpKlient = fpKlient;
        this.k9Klient = new K9sakKlient();
    }

    public List<SøkersFraværsperiode> hentSøkersFraværsperioder(ForespørselEntitet forespørsel) {
        if (FPSAK_YTELSER.contains(forespørsel.getYtelseType())) {
            return fpKlient.hentSøkersFravær(forespørsel.getFagsystemSaksnummer());
        } else if (K9SAK_YTELSER.contains(forespørsel.getYtelseType())) {
            return k9Klient.hentSøkersFravær(forespørsel.getFagsystemSaksnummer());
        }
        throw new IllegalStateException(String.format("Ukjent ytelsetype: %s", forespørsel.getYtelseType()));
    }
}
