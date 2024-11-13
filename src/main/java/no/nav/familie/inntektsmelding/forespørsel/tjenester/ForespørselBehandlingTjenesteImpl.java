package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OpprettForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
class ForespørselBehandlingTjenesteImpl implements ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjenesteImpl.class);

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private PersonTjeneste personTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private String inntektsmeldingSkjemaLenke;

    @Inject
    public ForespørselBehandlingTjenesteImpl(ForespørselTjeneste forespørselTjeneste,
                                             ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                             PersonTjeneste personTjeneste,
                                             ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    @Override
    public ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelsetype,
                                                             AktørIdEntitet aktørId,
                                                             OrganisasjonsnummerDto organisasjonsnummer,
                                                             SaksnummerDto fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        var åpenForespørsel = forespørselTjeneste.finnÅpenForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer);

        if (åpenForespørsel.isPresent()) {
            LOG.warn("Finnes allerede forespørsel for aktør {} med orgnummer {} og saksnr {} på startdato {} på ytelse {}", aktørId, organisasjonsnummer, fagsakSaksnummer, skjæringstidspunkt, ytelsetype);
            return ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE_ÅPEN;
        }

        opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt, førsteUttaksdato);
        return ForespørselResultat.FORESPØRSEL_OPPRETTET;
    }

    @Override
    public ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                                     AktørIdEntitet aktorId,
                                                     OrganisasjonsnummerDto organisasjonsnummerDto,
                                                     LocalDate startdato,
                                                     LukkeÅrsak årsak) {
        var foresporsel = forespørselTjeneste.finnForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);
        validerStartdato(foresporsel, startdato);

        arbeidsgiverNotifikasjon.oppgaveUtført(foresporsel.getOppgaveId(), OffsetDateTime.now());
        arbeidsgiverNotifikasjon.ferdigstillSak(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i arbeidsgiver-notifikasjon
        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(foresporsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak));
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel
        return foresporsel;
    }

    @Override
    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.finnForespørsel(forespørselUUID);
    }

    @Override
    public void oppdaterForespørsler(Ytelsetype ytelsetype,
                                     AktørIdEntitet aktørId,
                                     Map<LocalDate, List<OrganisasjonsnummerDto>> organisasjonerPerSkjæringstidspunkt,
                                     SaksnummerDto fagsakSaksnummer) {
        final var eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer);
        final var taskGruppe = new ProsessTaskGruppe();

        // Oppretter forespørsler for alle skjæringstidspunkter som ikke allerede er opprettet
        organisasjonerPerSkjæringstidspunkt.forEach((skjæringstidspunkt, organisasjoner) -> {
            organisasjoner.forEach(organisasjon -> {
                var eksisterendeForespørsel = eksisterendeForespørsler.stream()
                    .filter(forespørsel -> forespørsel.getSkjæringstidspunkt().equals(skjæringstidspunkt))
                    .filter(forespørsel -> forespørsel.getOrganisasjonsnummer().equals(organisasjon.orgnr()))
                    .filter(forespørsel -> !forespørsel.getStatus().equals(ForespørselStatus.UTGÅTT))
                    .findFirst();

                if (eksisterendeForespørsel.isEmpty()) {
                    var opprettForespørselTask = OpprettForespørselTask.lagTaskData(ytelsetype,
                        aktørId,
                        fagsakSaksnummer,
                        organisasjon,
                        skjæringstidspunkt);
                    taskGruppe.addNesteParallell(opprettForespørselTask);
                }
            });
        });

        // Forespørsler som ikke lenger er aktuelle settes til utgått
        eksisterendeForespørsler.forEach(eksisterendeForespørsel -> {
            boolean trengerEksisterendeForespørsel = innholderRequestEksisterendeForespørsel(organisasjonerPerSkjæringstidspunkt,
                eksisterendeForespørsel);

            if (!trengerEksisterendeForespørsel && eksisterendeForespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
                var settForespørselTilUtgåttTask = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
                settForespørselTilUtgåttTask.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, eksisterendeForespørsel.getUuid().toString());
                taskGruppe.addNesteParallell(settForespørselTilUtgåttTask);
            }
        });

        if (!taskGruppe.getTasks().isEmpty()) {
            taskGruppe.setCallIdFraEksisterende();
            prosessTaskTjeneste.lagre(taskGruppe);
        } else {
            LOG.info("Ingen oppdatering er nødvendig for saksnr: {}", fagsakSaksnummer);
        }
    }

    @Override
    public void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel) {
        arbeidsgiverNotifikasjon.oppgaveUtgått(eksisterendeForespørsel.getOppgaveId(), OffsetDateTime.now());
        arbeidsgiverNotifikasjon.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i arbeidsgiver-notifikasjon
        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getFagsystemSaksnummer(),
            eksisterendeForespørsel.getYtelseType());
        LOG.info(msg);
    }

    private boolean innholderRequestEksisterendeForespørsel(Map<LocalDate, List<OrganisasjonsnummerDto>> organisasjonerPerSkjæringstidspunkt,
                                                            ForespørselEntitet eksisterendeForespørsel) {
        var stp = eksisterendeForespørsel.getSkjæringstidspunkt();
        var orgnrList = organisasjonerPerSkjæringstidspunkt.get(stp);

        if (orgnrList == null) {
            return false;
        }

        var orgnrFraRequestForStp = orgnrList.stream().map(OrganisasjonsnummerDto::orgnr).toList();
        return orgnrFraRequestForStp.contains(eksisterendeForespørsel.getOrganisasjonsnummer());
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørIdEntitet aktørId,
                                   SaksnummerDto fagsakSaksnummer,
                                   OrganisasjonsnummerDto organisasjonsnummer,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnr(),
            ytelsetype);
        LOG.info(msg);

        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt, ytelsetype, aktørId, organisasjonsnummer, fagsakSaksnummer, førsteUttaksdato);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var arbeidsgiverNotifikasjonSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, arbeidsgiverNotifikasjonSakId);

        var oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(),
            merkelapp,
            uuid.toString(),
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagOppgaveTekst(ytelsetype),
            ForespørselTekster.lagVarselTekst(ytelsetype),
            ForespørselTekster.lagPåminnelseTekst(ytelsetype),
            skjemaUri);

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    @Override
    public void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.getUuid(),
                f.getAktørId(),
                new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()),
                f.getSkjæringstidspunkt(),
                LukkeÅrsak.EKSTERN_INNSENDING);
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    @Override
    public void settForespørselTilUtgått(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        forespørsler.forEach(this::settForespørselTilUtgått);
    }

    private List<ForespørselEntitet> hentÅpneForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                                   OrganisasjonsnummerDto orgnummerDto,
                                                                   LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    @Override
    public List<ForespørselEntitet> hentForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                              OrganisasjonsnummerDto orgnummerDto,
                                                              LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    @Override
    public void slettForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var sakerSomSkalSlettes = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> skjæringstidspunkt == null || f.getSkjæringstidspunkt().equals(skjæringstidspunkt))
            .filter(f -> orgnummerDto == null || f.getOrganisasjonsnummer().equals(orgnummerDto.orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
            .toList();

        if (sakerSomSkalSlettes.size() != 1) {
            var msg = String.format("Fant ikke akkurat 1 sak som skulle slettes. Fant istedet %s saker ", sakerSomSkalSlettes.size());
            throw new IllegalStateException(msg);
        }
        var agPortalSakId = sakerSomSkalSlettes.getFirst().getArbeidsgiverNotifikasjonSakId();
        arbeidsgiverNotifikasjon.slettSak(agPortalSakId);
        forespørselTjeneste.settForespørselTilUtgått(agPortalSakId);
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
}
