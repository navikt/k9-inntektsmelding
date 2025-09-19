package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.fagsak.FagsakInfoDto;

@ApplicationScoped
public class K9SakTjeneste {
    private K9SakKlient klient;

    public K9SakTjeneste() {
        // CDI
    }

    @Inject
    public K9SakTjeneste(K9SakKlient klient) {
        this.klient = klient;
    }

    public List<FagsakInfo> hentFagsakInfo(Ytelsetype ytelsetype, PersonIdent personIdent) {
        List<FagsakInfoDto> k9Fagsaker = klient.hentFagsakInfo(ytelsetype, personIdent.getIdent());

        return k9Fagsaker.stream()
            .map(k9Fagsak ->
                new FagsakInfo(
                    new SaksnummerDto(k9Fagsak.getSaksnummer().getVerdi()),
                    mapYtelsetype(k9Fagsak.getYtelseType()),
                    new PeriodeDto(k9Fagsak.getGyldigPeriode().getFom(), k9Fagsak.getGyldigPeriode().getTom())
                )
            ).toList();
    }

    private Ytelsetype mapYtelsetype(FagsakYtelseType fagsagYtelseType) {
        return switch (fagsagYtelseType) {
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            default -> throw new IllegalArgumentException("Ukjent ytelsetype: " + fagsagYtelseType);
        };
    }

}
