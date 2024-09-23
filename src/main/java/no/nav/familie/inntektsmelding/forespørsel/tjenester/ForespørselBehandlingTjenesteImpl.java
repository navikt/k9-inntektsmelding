package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
class ForespørselBehandlingTjenesteImpl implements ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjenesteImpl.class);

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private PersonTjeneste personTjeneste;
    private String inntektsmeldingSkjemaLenke;

    public ForespørselBehandlingTjenesteImpl() {
    }

    @Inject
    public ForespørselBehandlingTjenesteImpl(ForespørselTjeneste forespørselTjeneste,
                                             ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                             PersonTjeneste personTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    @Override
    public void håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                              Ytelsetype ytelsetype,
                                              AktørIdEntitet aktørId,
                                              OrganisasjonsnummerDto organisasjonsnummer,
                                              SaksnummerDto fagsakSaksnummer) {
        var åpenForespørsel = forespørselTjeneste.finnÅpenForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer);

        if (åpenForespørsel.isPresent()) {
            var msg = String.format("Finnes allerede forespørsel for aktør %s på startdato %s + på ytelse %s", aktørId, skjæringstidspunkt,
                ytelsetype);
            LOG.info(msg);
            return;
        }

        opprettOppgave(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);
    }

    @Override
    public void ferdigstillForespørsel(UUID foresporselUuid,
                                       AktørIdEntitet aktorId,
                                       OrganisasjonsnummerDto organisasjonsnummerDto,
                                       LocalDate startdato) {
        var foresporsel = forespørselTjeneste.finnForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);
        validerStartdato(foresporsel, startdato);

        arbeidsgiverNotifikasjon.lukkOppgave(foresporsel.getOppgaveId(), OffsetDateTime.now());
        arbeidsgiverNotifikasjon.ferdigstillSak(foresporsel.getSakId()); // Oppdaterer status i arbeidsgiver-notifikasjon
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getSakId()); // Oppdaterer status i forespørsel
    }

    @Override
    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.finnForespørsel(forespørselUUID);
    }

    @Override
    public void oppdaterAlleForespørslerISaken(Ytelsetype ytelsetype,
                                               AktørIdEntitet aktørId,
                                               Map<OrganisasjonsnummerDto, List<LocalDate>> skjæringstidspunkterPerOrganisasjon,
                                               SaksnummerDto fagsakSaksnummer) {
        var eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForSak(fagsakSaksnummer);

        // Opprett forespørsler for alle skjæringstidspunkter som ikke allerede er opprettet
        skjæringstidspunkterPerOrganisasjon.forEach((organisasjonsnummer, skjæringstidspunkterForOrganisasjon) -> {
            skjæringstidspunkterForOrganisasjon.forEach(skjæringstidspunkt -> {
                var åpenForespørsel = eksisterendeForespørsler.stream()
                    .filter(eksisterendeForespørsel -> eksisterendeForespørsel.getSkjæringstidspunkt().equals(skjæringstidspunkt))
                    .filter(eksisterendeForespørsel -> eksisterendeForespørsel.getOrganisasjonsnummer().equals(organisasjonsnummer.orgnr()))
                    .findFirst();

                if (åpenForespørsel.isPresent()) {
                    var msg = String.format("Finnes allerede forespørsel for aktør %s på startdato %s + på ytelse %s", aktørId, skjæringstidspunkt,
                        ytelsetype);
                    LOG.info(msg);
                    return;
                }

                opprettOppgave(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);

            });
        });

        // Slett forespørsler som ikke lenger er aktuelle
        for (ForespørselEntitet eksisterende : eksisterendeForespørsler) {
            if (eksisterende.getStatus() == ForespørselStatus.FERDIG) {
                continue;
            }
            List<LocalDate> stperFraRequest = skjæringstidspunkterPerOrganisasjon.get(new OrganisasjonsnummerDto(eksisterende.getOrganisasjonsnummer()));
            if (!stperFraRequest.contains(eksisterende.getSkjæringstidspunkt())) {
                //TODO slette
            }
        }
    }

    private void opprettOppgave(Ytelsetype ytelsetype,
                                AktørIdEntitet aktørId,
                                SaksnummerDto fagsakSaksnummer,
                                OrganisasjonsnummerDto organisasjonsnummer,
                                LocalDate skjæringstidspunkt) {
        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var sakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(), merkelapp, organisasjonsnummer.orgnr(), lagSaksTittel(person), skjemaUri);

        forespørselTjeneste.setSakId(uuid, sakId);

        var oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(), merkelapp, uuid.toString(), organisasjonsnummer.orgnr(),
            "NAV trenger inntektsmelding for å kunne behandle saken til din ansatt", skjemaUri);

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    public void lukkForespørsel(SaksnummerDto saksnummerDto, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = forespørselTjeneste.finnÅpneForespørslerForFagsak(saksnummerDto).stream()
            .filter(f -> orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();

            forespørsler.forEach(f -> ferdigstillForespørsel(f.getUuid(), f.getAktørId(), new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()), f.getSkjæringstidspunkt()));
    }

    private void validerStartdato(ForespørselEntitet forespørsel, LocalDate startdato) {
        if (!forespørsel.getSkjæringstidspunkt().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    private void validerOrganisasjon(ForespørselEntitet forespørsel, OrganisasjonsnummerDto orgnummer) {
        if (!forespørsel.getOrganisasjonsnummer().equals(orgnummer.orgnr())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    private void validerAktør(ForespørselEntitet forespørsel, AktørIdEntitet aktorId) {
        if (!forespørsel.getAktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
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
        return String.format("Inntektsmelding for %s: f. %s", StringUtils.capitalize(personInfo.mapNavn()),
            personInfo.fødselsdato().format(DateTimeFormatter.ofPattern("ddMMyy")));
    }
}
