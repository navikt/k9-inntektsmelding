package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.k9inntektsmelding.FinnSakerDto;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class K9SakTjeneste {
    private K9SakKlient klient;

    K9SakTjeneste() {
        // CDI
    }

    @Inject
    public K9SakTjeneste(K9SakKlient klient) {
        this.klient = klient;
    }

    public FagsakInfo hentFagsakInfo(SaksnummerDto saksnummer) {
        FinnSakerDto k9Fagsak = klient.hentFagsakInfo(saksnummer);

        return new FagsakInfo(
            new SaksnummerDto(k9Fagsak.saksnummer().getVerdi()),
            mapYtelsetype(k9Fagsak.ytelseType()),
            k9Fagsak.aktørId(),
            new PeriodeDto(k9Fagsak.gyldigPeriode().getFom(), k9Fagsak.gyldigPeriode().getTom()),
            mapSøknadsperiode(k9Fagsak),
            k9Fagsak.venterForTidligSøknad());
    }

    public List<FagsakInfo> hentFagsakInfo(Ytelsetype ytelsetype, AktørId aktørId) {
        List<FinnSakerDto> k9Fagsaker = klient.hentFagsakInfo(ytelsetype, aktørId);

        return k9Fagsaker.stream()
            .map(k9Fagsak ->
                new FagsakInfo(
                    new SaksnummerDto(k9Fagsak.saksnummer().getVerdi()),
                    mapYtelsetype(k9Fagsak.ytelseType()),
                    k9Fagsak.aktørId(),
                    new PeriodeDto(k9Fagsak.gyldigPeriode().getFom(), k9Fagsak.gyldigPeriode().getTom()),
                    mapSøknadsperiode(k9Fagsak),
                    k9Fagsak.venterForTidligSøknad()
                )
            ).toList();
    }

    private static List<PeriodeDto> mapSøknadsperiode(FinnSakerDto k9Fagsak) {
        return k9Fagsak.søknadsperioder()
            .stream()
            .map(søknadsPeriode -> new PeriodeDto(søknadsPeriode.getFom(), søknadsPeriode.getTom()))
            .toList();
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
