package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

@ApplicationScoped
public class PersonTjeneste {
    private PdlKlient pdlKlient;

    PersonTjeneste() {
        // CDI
    }

    @Inject
    public PersonTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public PersonInfo hentPersonInfo(AktørId aktørId, Ytelsetype ytelseType) {
        var request = new HentPersonQueryRequest();
        request.setIdent(aktørId.getId());

        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn());

        PersonIdent personIdent = hentPersonidentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalStateException("Finner ikke personnummer for aktørId " + aktørId));

        var person = pdlKlient.hentPerson(utledYtelse(ytelseType), request, projection);

        return new PersonInfo(mapNavn(person), personIdent, aktørId);
    }

    private String mapNavn(Person person) {
        return person.getNavn().stream()
            .map(PersonTjeneste::mapNavn)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseGet(() -> Environment.current().isProd() ? null : "Navnløs i Folkeregister");
    }

    private static Optional<String> mapNavn(Navn navn) {
        if (navn.getEtternavn() == null || navn.getFornavn() == null) {
            return Optional.empty();
        }
        return Optional.of(navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn()));
    }

    private Optional<PersonIdent> hentPersonidentForAktørId(AktørId aktørId) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(aktørId.getId());
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID));
        request.setHistorikk(Boolean.FALSE);
        var projection = new IdentlisteResponseProjection()
            .identer(new IdentInformasjonResponseProjection().ident());
        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).map(PersonIdent::new);
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return Optional.empty();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FT-723618", "PDL timeout") : e;
        }
    }

    private static Persondata.Ytelse utledYtelse(Ytelsetype ytelseType) {
        if (Ytelsetype.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return Persondata.Ytelse.SVANGERSKAPSPENGER;
        } else {
            return Persondata.Ytelse.FORELDREPENGER;
        }
    }

}
