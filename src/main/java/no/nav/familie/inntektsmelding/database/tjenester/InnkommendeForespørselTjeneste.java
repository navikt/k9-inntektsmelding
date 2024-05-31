package no.nav.familie.inntektsmelding.database.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class InnkommendeForespørselTjeneste {

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private PersonTjeneste personTjeneste;
    private String inntektsmeldingSkjemaLenke;

    public InnkommendeForespørselTjeneste() {
    }

    @Inject
    public InnkommendeForespørselTjeneste(ForespørselTjeneste forespørselTjeneste,
                                          ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                          PersonTjeneste personTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://familie-inntektsmelding.nav.no");
    }

    public void håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                              Ytelsetype ytelsetype,
                                              AktørIdDto aktørId,
                                              OrganisasjonsnummerDto organisasjonsnummer,
                                              SaksnummerDto fagsakSaksnummer) {
        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        var person = personTjeneste.hentPersonInfo(aktørId, ytelsetype);
        var merkelapp = finnMerkelapp(ytelsetype);
        var sakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),merkelapp, organisasjonsnummer.orgnr(), lagSaksTittel(person),
            URI.create(inntektsmeldingSkjemaLenke + "/ny/" + uuid));

        forespørselTjeneste.setSakId(uuid, sakId);

        var oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(), merkelapp, uuid.toString(), organisasjonsnummer.orgnr(),
            "NAV trenger inntektsmelding for å kunne behandle saken til din ansatt", URI.create(inntektsmeldingSkjemaLenke + "/ny/" + uuid));

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    private Merkelapp finnMerkelapp(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
            case PLEIEPENGER_NÆRSTÅENDE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
        };
    }

    protected String lagSaksTittel(PersonInfo personInfo) {
        return String.format("Inntektsmelding for %s: f. %s", StringUtils.capitalize(personInfo.navn()),
            personInfo.fødselsdato().format(DateTimeFormatter.ofPattern("ddMMyy")));
    }
}
