package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnloggetBrukerDto;

public class InnloggetBrukerTjeneste {
    private final PersonTjeneste personTjeneste;

    @Inject
    public InnloggetBrukerTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    public InnloggetBrukerDto hentInnloggetBruker(Ytelsetype ytelseType) {
        var innloggetBruker = personTjeneste.hentInnloggetPerson(ytelseType);
        if (innloggetBruker == null) {
            return new InnloggetBrukerDto(null, null, null, null);
        }
        return new InnloggetBrukerDto(
            innloggetBruker.fornavn(),
            innloggetBruker.mellomnavn(),
            innloggetBruker.etternavn(),
            innloggetBruker.telefonnummer()
        );
    }
}
