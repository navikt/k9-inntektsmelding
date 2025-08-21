package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.GjenåpneForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OppdaterForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.OpprettForespørselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselOppdatering;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjeneste.class);
    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon;
    private PersonTjeneste personTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private String inntektsmeldingSkjemaLenke;

    ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(ForespørselTjeneste forespørselTjeneste,
                                         ArbeidsgiverNotifikasjon arbeidsgiverNotifikasjon,
                                         PersonTjeneste personTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.arbeidsgiverNotifikasjon = arbeidsgiverNotifikasjon;
        this.personTjeneste = personTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke");
    }

    public ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                                     AktørIdEntitet aktorId,
                                                     OrganisasjonsnummerDto organisasjonsnummerDto,
                                                     LukkeÅrsak årsak,
                                                     List<FraværsPeriodeEntitet> fraværsPerioder,
                                                     List<DelvisFraværsPeriodeEntitet> delvisFraværDag) {
        var foresporsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);

        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        foresporsel.getOppgaveId().ifPresent(oppgaveId -> arbeidsgiverNotifikasjon.oppgaveUtført(oppgaveId, OffsetDateTime.now()));

        var erArbeidsgiverinitiert = foresporsel.getOppgaveId().isEmpty();
        arbeidsgiverNotifikasjon.ferdigstillSak(foresporsel.getArbeidsgiverNotifikasjonSakId(), erArbeidsgiverinitiert); // Oppdaterer status i arbeidsgiver-notifikasjon

        var erOmsorgspenger = foresporsel.getYtelseType().equals(Ytelsetype.OMSORGSPENGER);
        String tilleggsinformasjon;
        if (erOmsorgspenger) {
            tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(fraværsPerioder, delvisFraværDag);
        } else {
            tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(årsak, foresporsel.getSkjæringstidspunkt());
        }

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(foresporsel.getArbeidsgiverNotifikasjonSakId(), tilleggsinformasjon);
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel
        return foresporsel;
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    public List<ForespørselEntitet> finnForespørslerUnderBehandling(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnForespørslerUnderBehandling(aktørId, ytelsetype, orgnr);
    }

    public List<ForespørselEntitet> finnAlleForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnAlleForespørsler(aktørId, ytelsetype, orgnr);
    }

    public void oppdaterForespørsler(Ytelsetype ytelsetype,
                                     AktørIdEntitet aktørId,
                                     List<OppdaterForespørselDto> forespørsler,
                                     SaksnummerDto saksnummer) {
        final var eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForFagsak(saksnummer);
        final var taskGruppe = new ProsessTaskGruppe();

        // Forespørsler som skal opprettes
        var skalOpprettes = utledNyeForespørsler(forespørsler, eksisterendeForespørsler);
        for (OppdaterForespørselDto forespørselDto : skalOpprettes) {
            var opprettForespørselTask = OpprettForespørselTask.lagOpprettForespørselTaskData(ytelsetype, aktørId, saksnummer, forespørselDto);
            taskGruppe.addNesteParallell(opprettForespørselTask);
        }

        // Forespørsler som skal oppdateres
        if (ytelsetype == Ytelsetype.OMSORGSPENGER) {
            // oppdater forespørsler som er under behandling og har endret etterspurte perioder
            var forespørslerSomSkalOppdateres = utledForespørslerSomSkalOppdateres(forespørsler, eksisterendeForespørsler);
            for (ForespørselOppdatering forespørsel : forespørslerSomSkalOppdateres) {
                var oppdaterForespørselTask = OppdaterForespørselTask.lagOppdaterTaskData(forespørsel.forespørselUuid(), ytelsetype, forespørsel.oppdaterDto().etterspurtePerioder());
                taskGruppe.addNesteParallell(oppdaterForespørselTask);
            }

            // opprett nye forspørsler hvis en eksisterende fedig behanlet forespørsel har endret etterspurte perioder
            var forespørslerSomSkalOpprettes = utledForespørslerSomErFerdigeMenSomHarUlikeEtterspurtePerioder(forespørsler, eksisterendeForespørsler);
            for (ForespørselOppdatering forespørsel : forespørslerSomSkalOpprettes) {
                var opprettForespørselTask = OpprettForespørselTask.lagOpprettForespørselTaskData(ytelsetype, aktørId, saksnummer, forespørsel.oppdaterDto());
                taskGruppe.addNesteParallell(opprettForespørselTask);
            }
        }

        // Forespørsler som skal settes til utgått
        var skalSettesUtgått = utledForespørslerSomSkalSettesUtgått(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalSettesUtgått) {
            var settForespørselTilUtgåttTask = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
            settForespørselTilUtgåttTask.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            settForespørselTilUtgåttTask.setSaksnummer(saksnummer.saksnr());
            taskGruppe.addNesteParallell(settForespørselTilUtgåttTask);
        }

        // Forespørsler som skal gjenåpnes
        var skalGjenåpnes = utledForespørslerSomSkalGjenåpnes(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalGjenåpnes) {
            var gjenåpneForespørselTask = ProsessTaskData.forProsessTask(GjenåpneForespørselTask.class);
            gjenåpneForespørselTask.setProperty(GjenåpneForespørselTask.FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            gjenåpneForespørselTask.setSaksnummer(saksnummer.saksnr());
            taskGruppe.addNesteParallell(gjenåpneForespørselTask);
        }

        if (!taskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(taskGruppe);
        } else {
            LOG.info("Ingen oppdatering er nødvendig for saksnummer: {}", saksnummer);
        }
    }

    private static List<OppdaterForespørselDto> utledNyeForespørsler(List<OppdaterForespørselDto> forespørsler,
                                                                     List<ForespørselEntitet> eksisterendeForespørsler) {
        // Skal opprette forespørsler for alle skjæringstidspunkt som ikke allerede er opprettet
        return forespørsler.stream()
            .filter(f -> f.aksjon() == ForespørselAksjon.OPPRETT)
            .filter(f -> finnEksisterendeForespørsel(f,
                eksisterendeForespørsler,
                List.of(ForespørselStatus.UNDER_BEHANDLING, ForespørselStatus.FERDIG)).isEmpty())
            .toList();
    }

    public List<ForespørselOppdatering> utledForespørslerSomSkalOppdateres(List<OppdaterForespørselDto> forespørselDtoer,
                                                                           List<ForespørselEntitet> eksisterendeForespørsler) {
        if (forespørselDtoer.isEmpty() || eksisterendeForespørsler.isEmpty()) {
            return List.of();
        }

        return forespørselDtoer.stream()
            .filter(forespørselDto -> forespørselDto.aksjon() == ForespørselAksjon.OPPRETT || forespørselDto.aksjon() == ForespørselAksjon.BEHOLD)
            .map(forespørselDto -> {
                Optional<ForespørselEntitet> eksisterendeForespørselEntitet = finnEksisterendeForespørselMedUlikEtterspurtePerioderSomErUnderBehandling(forespørselDto, eksisterendeForespørsler);
                return eksisterendeForespørselEntitet.map(forspørsel -> new ForespørselOppdatering(forespørselDto, forspørsel.getUuid())).orElse(null);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public List<ForespørselOppdatering> utledForespørslerSomErFerdigeMenSomHarUlikeEtterspurtePerioder(List<OppdaterForespørselDto> forespørselDtoer,
                                                                                                       List<ForespørselEntitet> eksisterendeForespørsler) {
        if (forespørselDtoer.isEmpty() || eksisterendeForespørsler.isEmpty()) {
            return List.of();
        }

        return forespørselDtoer.stream()
            .filter(forespørselDto -> forespørselDto.aksjon() == ForespørselAksjon.OPPRETT || forespørselDto.aksjon() == ForespørselAksjon.BEHOLD)
            .map(forespørselDto -> {
                Optional<ForespørselEntitet> eksisterendeForespørselEntitet = finnEksisterendeForespørselMedUlikEtterspurtePerioderSomErFerdig(forespørselDto, eksisterendeForespørsler);
                return eksisterendeForespørselEntitet.map(forspørsel -> new ForespørselOppdatering(forespørselDto, forspørsel.getUuid())).orElse(null);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static List<ForespørselEntitet> utledForespørslerSomSkalSettesUtgått(List<OppdaterForespørselDto> forespørsler,
                                                                                 List<ForespørselEntitet> eksisterendeForespørsler) {
        List<ForespørselEntitet> skalSettesUtgått = new ArrayList<>();
        // Forespørsler som ikke lenger er aktuelle settes til utgått
        for (ForespørselEntitet eksisterendeForespørsel : eksisterendeForespørsler) {
            if (eksisterendeForespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
                boolean trengerEksisterendeForespørsel = innholderRequestEksisterendeForespørsel(forespørsler, eksisterendeForespørsel);
                if (!trengerEksisterendeForespørsel) {
                    skalSettesUtgått.add(eksisterendeForespørsel);
                }
            }
        }
        // Forespørsler som skal sperres for endringer settes til utgått
        for (OppdaterForespørselDto forespørselDto : forespørsler) {
            if (forespørselDto.aksjon() == ForespørselAksjon.UTGÅTT) {
                var skalSperresForEndringer = finnEksisterendeForespørsel(forespørselDto,
                    eksisterendeForespørsler,
                    List.of(ForespørselStatus.values()));
                skalSperresForEndringer.ifPresent(skalSettesUtgått::add);
            }
        }
        return skalSettesUtgått;
    }

    private static List<ForespørselEntitet> utledForespørslerSomSkalGjenåpnes(List<OppdaterForespørselDto> forespørsler,
                                                                              List<ForespørselEntitet> eksisterendeForespørsler) {
        List<ForespørselEntitet> forespørslerSomSkalGjenåpnes = new ArrayList<>();
        for (OppdaterForespørselDto forespørselDto : forespørsler) {
            if (forespørselDto.aksjon() == ForespørselAksjon.GJENOPPRETT) {
                var skalGjenåpnes = finnEksisterendeForespørsel(forespørselDto,
                    eksisterendeForespørsler,
                    List.of(ForespørselStatus.UTGÅTT));
                skalGjenåpnes.ifPresent(forespørslerSomSkalGjenåpnes::add);
            }
        }
        return forespørslerSomSkalGjenåpnes;
    }

    private static Optional<ForespørselEntitet> finnEksisterendeForespørsel(OppdaterForespørselDto forespørselDto,
                                                                            List<ForespørselEntitet> eksisterendeForespørsler,
                                                                            List<ForespørselStatus> statuser) {
        return eksisterendeForespørsler.stream()
            .filter(f -> f.getSkjæringstidspunkt().equals(forespørselDto.skjæringstidspunkt()))
            .filter(f -> f.getOrganisasjonsnummer().equals(forespørselDto.orgnr().orgnr()))
            .filter(f -> statuser.contains(f.getStatus()))
            .findFirst();
    }

    private static Optional<ForespørselEntitet> finnEksisterendeForespørselMedUlikEtterspurtePerioderSomErUnderBehandling(OppdaterForespørselDto forespørselDto,
                                                                                                                          List<ForespørselEntitet> eksisterendeForespørsler) {
        return eksisterendeForespørsler.stream()
            .filter(f -> f.getSkjæringstidspunkt().equals(forespørselDto.skjæringstidspunkt()))
            .filter(f -> f.getOrganisasjonsnummer().equals(forespørselDto.orgnr().orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
            .filter(f -> !f.getEtterspurtePerioder().stream().sorted(Comparator.comparing(PeriodeDto::fom).thenComparing(PeriodeDto::tom)).toList()
                .equals(forespørselDto.etterspurtePerioder().stream().sorted(Comparator.comparing(PeriodeDto::fom).thenComparing(PeriodeDto::tom)).toList()))
            .findFirst();
    }

    private static Optional<ForespørselEntitet> finnEksisterendeForespørselMedUlikEtterspurtePerioderSomErFerdig(OppdaterForespørselDto forespørselDto,
                                                                                                                          List<ForespørselEntitet> eksisterendeForespørsler) {
        return eksisterendeForespørsler.stream()
            .filter(f -> f.getSkjæringstidspunkt().equals(forespørselDto.skjæringstidspunkt()))
            .filter(f -> f.getOrganisasjonsnummer().equals(forespørselDto.orgnr().orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.FERDIG))
            .filter(f -> !f.getEtterspurtePerioder().stream().sorted(Comparator.comparing(PeriodeDto::fom).thenComparing(PeriodeDto::tom)).toList()
                .equals(forespørselDto.etterspurtePerioder().stream().sorted(Comparator.comparing(PeriodeDto::fom).thenComparing(PeriodeDto::tom)).toList()))
            .findFirst();
    }

    private static boolean innholderRequestEksisterendeForespørsel(List<OppdaterForespørselDto> forepørsler,
                                                                   ForespørselEntitet eksisterendeForespørsel) {
        return forepørsler.stream()
            .anyMatch(forespørselDto -> forespørselDto.orgnr().orgnr().equals(eksisterendeForespørsel.getOrganisasjonsnummer()) &&
                forespørselDto.skjæringstidspunkt().equals(eksisterendeForespørsel.getSkjæringstidspunkt()));
    }

    public void oppdaterForespørselMedNyeEtterspurtePerioder(UUID forespørselUuid, List<PeriodeDto> etterspurtePerioder) {
        ForespørselEntitet forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for UUID: " + forespørselUuid));

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(forespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(etterspurtePerioder));

        LOG.info("Oppdaterer forespørsel med nye etterspurte perioder, uuid: {}, perioder: {}", forespørselUuid, etterspurtePerioder);
        forespørselTjeneste.oppdaterForespørselMedNyeEtterspurtePerioder(forespørselUuid, etterspurtePerioder);
    }

    public void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        if (skalOppdatereArbeidsgiverNotifikasjon) {
            eksisterendeForespørsel.getOppgaveId().ifPresent(oppgaveId -> arbeidsgiverNotifikasjon.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            arbeidsgiverNotifikasjon.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
                false); // Oppdaterer status i arbeidsgiver-notifikasjon
        }

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, eksisterendeForespørsel.getSkjæringstidspunkt()));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        LOG.info("Setter forespørsel til utgått, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getSaksnummer().orElse(null),
            eksisterendeForespørsel.getYtelseType());
    }

    public void gjenåpneForespørsel(ForespørselEntitet eksisterendeForespørsel) {
        if (eksisterendeForespørsel.getStatus() != ForespørselStatus.UTGÅTT) {
            throw new IllegalArgumentException(
                "Forespørsel som skal gjenåpnes må ha status UTGÅTT, var " + eksisterendeForespørsel.getStatus() + ". " + eksisterendeForespørsel);
        }
        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(), null);
        forespørselTjeneste.ferdigstillForespørsel(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        LOG.info("Gjenåpner forespørsel, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt(),
            eksisterendeForespørsel.getSaksnummer().orElse(null),
            eksisterendeForespørsel.getYtelseType());
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørIdEntitet aktørId,
                                   SaksnummerDto saksnummer,
                                   OrganisasjonsnummerDto organisasjonsnummer,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato,
                                   List<PeriodeDto> etterspurtePerioder) {
        LOG.info("Oppretter forespørsel, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
            organisasjonsnummer,
            skjæringstidspunkt,
            saksnummer.saksnr(),
            ytelsetype);

        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr());

        var uuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            saksnummer,
            førsteUttaksdato,
            etterspurtePerioder);
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var arbeidsgiverNotifikasjonSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        String tilleggsinformasjon = (ytelsetype == Ytelsetype.OMSORGSPENGER)
                                     ? ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(etterspurtePerioder) : ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, skjæringstidspunkt);

        arbeidsgiverNotifikasjon.oppdaterSakTilleggsinformasjon(arbeidsgiverNotifikasjonSakId, tilleggsinformasjon);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = arbeidsgiverNotifikasjon.opprettOppgave(uuid.toString(),
                merkelapp,
                uuid.toString(),
                organisasjonsnummer.orgnr(),
                ForespørselTekster.lagOppgaveTekst(ytelsetype),
                ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon),
                ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon),
                skjemaUri);
        } catch (Exception e) {
            //Manuell rollback er nødvendig fordi sak og oppgave går i to forskjellige kall
            arbeidsgiverNotifikasjon.slettSak(arbeidsgiverNotifikasjonSakId);
            throw e;
        }

        forespørselTjeneste.setOppgaveId(uuid, oppgaveId);
    }

    public UUID opprettForespørselForOmsorgspengerRefusjonIm(AktørIdEntitet aktørId,
                                                             OrganisasjonsnummerDto organisasjonsnummer,
                                                             LocalDate skjæringstidspunkt) {
        LOG.info("Oppretter forespørsel for omsorgspenger refusjon, orgnr: {}, stp: {}, ytelse: {}",
            organisasjonsnummer,
            skjæringstidspunkt,
            Ytelsetype.OMSORGSPENGER);

        var uuid = forespørselTjeneste.opprettForespørselOmsorgspengerRefusjon(skjæringstidspunkt,
            aktørId,
            organisasjonsnummer,
            skjæringstidspunkt);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var merkelapp = Merkelapp.REFUSJONSKRAV_OMP;
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/refusjon-omsorgspenger/" + organisasjonsnummer.orgnr() + "/" + uuid);
        var fagerSakId = arbeidsgiverNotifikasjon.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittelRefusjonskrav(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, fagerSakId);

        return uuid;
    }

    public void lukkForespørsel(SaksnummerDto saksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(saksnummer, orgnummerDto, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.getUuid(),
                f.getAktørId(),
                new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()),
                LukkeÅrsak.EKSTERN_INNSENDING, List.of(), List.of());
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    public void settForespørselTilUtgått(SaksnummerDto saksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(saksnummer, orgnummerDto, skjæringstidspunkt);

        forespørsler.forEach(it -> settForespørselTilUtgått(it, true));
    }

    private List<ForespørselEntitet> hentÅpneForespørslerForFagsak(SaksnummerDto saksnummer,
                                                                   OrganisasjonsnummerDto orgnummerDto,
                                                                   LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(saksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    public List<ForespørselEntitet> hentForespørslerForFagsak(SaksnummerDto saksnummer,
                                                              OrganisasjonsnummerDto orgnummerDto,
                                                              LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnForespørslerForFagsak(saksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt()))
            .toList();
    }

    public void slettForespørsel(SaksnummerDto saksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var sakerSomSkalSlettes = forespørselTjeneste.finnForespørslerForFagsak(saksnummer).stream()
            .filter(f -> skjæringstidspunkt == null || f.getSkjæringstidspunkt().equals(skjæringstidspunkt))
            .filter(f -> orgnummerDto == null || f.getOrganisasjonsnummer().equals(orgnummerDto.orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
            .toList();

        if (sakerSomSkalSlettes.size() != 1) {
            String msg = String.format("Fant ikke akkurat 1 sak som skulle slettes. Fant istedet %s saker ", sakerSomSkalSlettes.size());
            throw new IllegalStateException(msg);
        }
        var agPortalSakId = sakerSomSkalSlettes.getFirst().getArbeidsgiverNotifikasjonSakId();
        arbeidsgiverNotifikasjon.slettSak(agPortalSakId);
        forespørselTjeneste.settForespørselTilUtgått(agPortalSakId);
    }

    public List<InntektsmeldingForespørselDto> finnForespørslerForFagsak(SaksnummerDto saksnummer) {
        return forespørselTjeneste.finnForespørslerForFagsak(saksnummer).stream().map(forespoersel ->
                new InntektsmeldingForespørselDto(
                    forespoersel.getUuid(),
                    forespoersel.getSkjæringstidspunkt(),
                    forespoersel.getOrganisasjonsnummer(),
                    forespoersel.getAktørId().getAktørId(),
                    forespoersel.getYtelseType().toString(),
                    forespoersel.getFørsteUttaksdato().orElse(null)))
            .toList();
    }

    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørIdEntitet, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørIdEntitet, ytelsetype);
    }

    public Optional<ForespørselEntitet> finnOpprinneligForespørsel(AktørIdEntitet aktørId, Ytelsetype ytelseType, LocalDate startdato) {
        return finnForespørslerForAktørId(aktørId, ytelseType).stream()
            .filter(f -> f.getFørsteUttaksdato().orElse(f.getSkjæringstidspunkt()).isBefore(startdato))
            .max(Comparator.comparing(f -> f.getFørsteUttaksdato().orElse(f.getSkjæringstidspunkt())));
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
