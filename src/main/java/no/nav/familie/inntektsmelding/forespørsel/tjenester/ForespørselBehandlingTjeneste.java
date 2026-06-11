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
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SendNyBeskjedOgVarselTask;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.SettForespørselTilUtgåttTask;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenKlient;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselAksjon;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselOppdatering;
import no.nav.familie.inntektsmelding.typer.dto.NyBeskjedResultat;
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
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private DialogportenKlient dialogportenKlient;
    private PersonTjeneste personTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private String arbeidsgiverportalSkjemaLenke;
    private boolean dialogportenEnabled;

    ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(ForespørselTjeneste forespørselTjeneste,
                                         MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                         DialogportenKlient dialogportenKlient,
                                         PersonTjeneste personTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.dialogportenKlient = dialogportenKlient;
        this.personTjeneste = personTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.arbeidsgiverportalSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke");
        this.dialogportenEnabled = ENV.getProperty("dialogporten.enabled", Boolean.class, false);
    }

    public ForespørselEntitet ferdigstillForespørsel(UUID forespørselUuid,
                                                     AktørIdEntitet aktørId,
                                                     OrganisasjonsnummerDto organisasjonsnummer,
                                                     LukkeÅrsak lukkeÅrsak,
                                                     Optional<UUID> inntektsmeldingUuid) {
        return ferdigstillForespørsel(forespørselUuid, aktørId, organisasjonsnummer, lukkeÅrsak, List.of(), List.of(), inntektsmeldingUuid);
    }

    public ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                                     AktørIdEntitet aktorId,
                                                     OrganisasjonsnummerDto organisasjonsnummerDto,
                                                     LukkeÅrsak årsak,
                                                     List<FraværsPeriodeEntitet> fraværsPerioder,
                                                     List<DelvisFraværsPeriodeEntitet> delvisFraværDag,
                                                     // inntektsmeldingUuid er optional fordi vi ikke har inntektsmeldingen lagret hvis den er innsendt via Altinn / LPS'er
                                                     Optional<UUID> inntektsmeldingUuid) {
        var forespørsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(forespørsel, aktorId);
        validerOrganisasjon(forespørsel, organisasjonsnummerDto);

        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        if (forespørsel.getOppgaveId().isPresent()) {
            minSideArbeidsgiverTjeneste.oppgaveUtført(forespørsel.getOppgaveId().get(), OffsetDateTime.now());
        }

        var erArbeidsgiverinitiert = forespørsel.getOppgaveId().isEmpty();
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel.getArbeidsgiverNotifikasjonSakId(), erArbeidsgiverinitiert); // Oppdaterer status i arbeidsgiver-notifikasjon

        var erOmsorgspenger = forespørsel.getYtelseType().equals(Ytelsetype.OMSORGSPENGER);
        String tilleggsinformasjon;
        if (erOmsorgspenger) {
            tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(fraværsPerioder, delvisFraværDag);
        } else {
            tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(årsak, forespørsel.getSkjæringstidspunkt());
        }

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørsel.getArbeidsgiverNotifikasjonSakId(), tilleggsinformasjon);
        forespørselTjeneste.ferdigstillForespørsel(forespørsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel

        // Oppdaterer status i altinn dialogporten
        if (forespørsel.getDialogportenUuid().isPresent()) {
            if (dialogportenEnabled) {
                try {
                    dialogportenKlient.ferdigstillDialog(forespørsel.getDialogportenUuid().get(),
                        new ArbeidsgiverDto(organisasjonsnummerDto.orgnr()),
                        lagSaksTittelForDialogporten(aktorId),
                        forespørsel.getYtelseType(),
                        forespørsel.getSkjæringstidspunkt(),
                        inntektsmeldingUuid,
                        årsak);
                } catch (Exception e) {
                    // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                    LOG.warn("Feil ved kall til dialogporten: ", e);
                }
            }
        }
        return forespørsel;
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    public List<ForespørselEntitet> hentForespørsler(ArbeidsgiverDto arbeidsgiver,
                                                     AktørIdEntitet aktørId,
                                                     ForespørselStatus status,
                                                     Ytelsetype ytelseType,
                                                     LocalDate fom,
                                                     LocalDate tom) {
        return forespørselTjeneste.hentForespørslerFraFilter(arbeidsgiver.ident(), aktørId, status, ytelseType, fom, tom);
    }

    public List<ForespørselEntitet> finnAlleForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnAlleForespørsler(aktørId, ytelsetype, orgnr);
    }

    public void oppdaterForespørsler(Ytelsetype ytelsetype,
                                     AktørIdEntitet aktørId,
                                     List<OppdaterForespørselDto> forespørsler,
                                     SaksnummerDto saksnummer) {
        final var eksisterendeForespørsler = forespørselTjeneste.finnForespørslerForFagsak(saksnummer);
        final List<ProsessTaskData> tasker = new ArrayList<>();

        // Forespørsler som skal opprettes
        var skalOpprettes = utledNyeForespørsler(forespørsler, eksisterendeForespørsler);
        for (OppdaterForespørselDto forespørselDto : skalOpprettes) {
            var opprettForespørselTask = OpprettForespørselTask.lagOpprettForespørselTaskData(ytelsetype, aktørId, saksnummer, forespørselDto);
            tasker.add(opprettForespørselTask);
        }

        // Forespørsler som skal oppdateres
        if (ytelsetype == Ytelsetype.OMSORGSPENGER) {
            var skalOppdateres = utledForespørslerSomSkalOppdateres(forespørsler, eksisterendeForespørsler);
            for (ForespørselOppdatering forespørsel : skalOppdateres) {
                var oppdaterForespørselTask = OppdaterForespørselTask.lagOppdaterTaskData(forespørsel.forespørselUuid(), ytelsetype, forespørsel.oppdaterDto().etterspurtePerioder());
                tasker.add(oppdaterForespørselTask);
            }
        }

        // Forespørsler som skal settes til utgått
        var skalSettesUtgått = utledForespørslerSomSkalSettesUtgått(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalSettesUtgått) {
            var settForespørselTilUtgåttTask = SettForespørselTilUtgåttTask.lagSettTilUtgåttTask(forespørsel.getUuid(), saksnummer);
            tasker.add(settForespørselTilUtgåttTask);
        }

        // Forespørsler som skal gjenåpnes
        var skalGjenåpnes = utledForespørslerSomSkalGjenåpnes(forespørsler, eksisterendeForespørsler);
        for (ForespørselEntitet forespørsel : skalGjenåpnes) {
            var gjenåpneForespørselTask = GjenåpneForespørselTask.lagGjenåpneForespørselTask(forespørsel.getUuid(), saksnummer);
            tasker.add(gjenåpneForespørselTask);
        }

        if (!tasker.isEmpty()) {
            var taskGruppe = new ProsessTaskGruppe();
            taskGruppe.addNesteParallell(tasker);
            taskGruppe.setSaksnummer(saksnummer.saksnr());
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
                Optional<ForespørselEntitet> eksisterendeForespørselEntitet = finnEksisterendeForespørselMedUlikEtterspurtePerioder(forespørselDto, eksisterendeForespørsler);
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

    private static Optional<ForespørselEntitet> finnEksisterendeForespørselMedUlikEtterspurtePerioder(OppdaterForespørselDto forespørselDto,
                                                                                                      List<ForespørselEntitet> eksisterendeForespørsler) {
        return eksisterendeForespørsler.stream()
            .filter(f -> f.getSkjæringstidspunkt().equals(forespørselDto.skjæringstidspunkt()))
            .filter(f -> f.getOrganisasjonsnummer().equals(forespørselDto.orgnr().orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
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

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(etterspurtePerioder));

        LOG.info("Oppdaterer forespørsel med nye etterspurte perioder, uuid: {}, perioder: {}", forespørselUuid, etterspurtePerioder);
        forespørselTjeneste.oppdaterForespørselMedNyeEtterspurtePerioder(forespørselUuid, etterspurtePerioder);
    }

    public void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        if (skalOppdatereArbeidsgiverNotifikasjon) {
            eksisterendeForespørsel.getOppgaveId().ifPresent(oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            minSideArbeidsgiverTjeneste.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
                false); // Oppdaterer status i arbeidsgiver-notifikasjon
        }

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, eksisterendeForespørsel.getSkjæringstidspunkt()));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());
        //oppdaterer status til not applicable i altinn dialogporten
        if (dialogportenEnabled) {
            eksisterendeForespørsel.getDialogportenUuid().ifPresent(dialogUuid ->
                dialogportenKlient.settDialogTilUtgått(dialogUuid, lagSaksTittelForDialogporten(eksisterendeForespørsel.getAktørId())));
        }

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
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(), null);
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
                                   List<PeriodeDto> etterspurtePerioder,
                                   ForespørselType forespørselType) {
        LOG.info("Oppretter forespørsel, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
            organisasjonsnummer,
            skjæringstidspunkt,
            saksnummer.saksnr(),
            ytelsetype);

        var forespørselUuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            saksnummer,
            førsteUttaksdato,
            etterspurtePerioder,
            forespørselType);

        opprettForespørselMinSideArbeidsgiver(ytelsetype, aktørId, organisasjonsnummer, skjæringstidspunkt, etterspurtePerioder, forespørselUuid);

        if (dialogportenEnabled) {
            try {
                opprettForespørselDialogporten(forespørselUuid, new ArbeidsgiverDto(organisasjonsnummer.orgnr()), aktørId, ytelsetype, skjæringstidspunkt);
            } catch (Exception e) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }
    }

    private void opprettForespørselMinSideArbeidsgiver(Ytelsetype ytelsetype,
                                                       AktørIdEntitet aktørId,
                                                       OrganisasjonsnummerDto organisasjonsnummer,
                                                       LocalDate skjæringstidspunkt,
                                                       List<PeriodeDto> etterspurtePerioder,
                                                       UUID forespørselUuid) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr());
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(arbeidsgiverportalSkjemaLenke + "/" + forespørselUuid);
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        String tilleggsinformasjon = (ytelsetype == Ytelsetype.OMSORGSPENGER)
                                     ? ForespørselTekster.lagTilleggsInformasjonForOmsorgspenger(etterspurtePerioder)
                                     : ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, skjæringstidspunkt);

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(arbeidsgiverNotifikasjonSakId, tilleggsinformasjon);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørselUuid.toString(),
                merkelapp,
                forespørselUuid.toString(),
                organisasjonsnummer.orgnr(),
                ForespørselTekster.lagOppgaveTekst(ytelsetype),
                ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon),
                ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon),
                skjemaUri);
        } catch (Exception e) {
            //Manuell rollback er nødvendig fordi sak og oppgave går i to forskjellige kall
            minSideArbeidsgiverTjeneste.slettSak(arbeidsgiverNotifikasjonSakId);
            throw e;
        }

        forespørselTjeneste.setOppgaveId(forespørselUuid, oppgaveId);
    }

    private void opprettForespørselDialogporten(UUID forespørselUuid,
                                                ArbeidsgiverDto arbeidsgiver,
                                                AktørIdEntitet aktørId,
                                                Ytelsetype ytelsetype,
                                                LocalDate førsteUttaksdato) {
        String saksTittelDialog = lagSaksTittelForDialogporten(aktørId);
        String dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselUuid, arbeidsgiver, saksTittelDialog, førsteUttaksdato, ytelsetype);

        String vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, UUID.fromString(vasketDialogUuid));
    }

    public void oppdaterPortalerMedEndretInntektsmelding(ForespørselEntitet forespørsel,
                                                         Optional<UUID> inntektsmeldingUuid,
                                                         OrganisasjonsnummerDto arbeidsgiver) {
        // Oppdater status i arbeidsgiverportalen
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.getYtelseType());
        var beskjedTekst = ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
        var hentInntektsmeldingPdfUrl = arbeidsgiverportalSkjemaLenke + "/server/api/" + PdfDokumentRest.INNTEKTSMELDING_FULL_PATH + "/" + inntektsmeldingUuid;
        minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørsel.getUuid().toString(),
            merkelapp,
            arbeidsgiver.orgnr(),
            beskjedTekst,
            URI.create(hentInntektsmeldingPdfUrl));

        // Oppdater status i altinn dialogporten
        if (forespørsel.getDialogportenUuid().isPresent()) {
            dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(forespørsel.getDialogportenUuid().get(),
                new ArbeidsgiverDto(arbeidsgiver.orgnr()),
                inntektsmeldingUuid);
        }
    }

    private String lagSaksTittelForDialogporten(AktørIdEntitet aktørId) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        return ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato());
    }

    public UUID opprettForespørselForArbeidsgiverInitiertInntektsmelding(AktørIdEntitet aktørId,
                                                                         OrganisasjonsnummerDto organisasjonsnummer,
                                                                         LocalDate skjæringstidspunkt,
                                                                         Ytelsetype ytelsetype,
                                                                         ForespørselType forespørselType) {
        LOG.info("Oppretter forespørsel for arbeidsgiverinitiert inntektsmelding, orgnr: {}, stp: {}, aktørId: {}, ytelse: {}", organisasjonsnummer.orgnr(), skjæringstidspunkt, aktørId.getAktørId(), ytelsetype);

        // opprettt forespørsel i databasen
        var forespørselUuid = forespørselTjeneste.opprettForespørselUtenFagsaksnummer(skjæringstidspunkt, aktørId, organisasjonsnummer, ytelsetype, forespørselType);

        // opprett sak på min side arbeidsgiver
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var inntektsmeldingOppsummeringsUri = lagUriForInntektsmeldingOppsummering(forespørselUuid);
        var sakstittel = ForespørselTekster.lagSaksTittelInntektsmelding(person.mapFulltNavn(), person.fødselsdato());
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(), merkelapp, organisasjonsnummer.orgnr(), sakstittel, inntektsmeldingOppsummeringsUri);

        // oppdater forespørsel med sakId fra min side arbeidsgiver
        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        if (dialogportenEnabled) {
            try {
                opprettForespørselDialogporten(forespørselUuid, new ArbeidsgiverDto(organisasjonsnummer.orgnr()), aktørId, ytelsetype, skjæringstidspunkt);
            } catch (Exception e) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }

        return forespørselUuid;
    }

    public UUID opprettForespørselForOmsorgspengerRefusjonIm(AktørIdEntitet aktørId,
                                                             OrganisasjonsnummerDto organisasjonsnummer,
                                                             LocalDate skjæringstidspunkt) {
        LOG.info("Oppretter forespørsel for omsorgspenger refusjon, orgnr: {}, stp: {}, ytelse: {}", organisasjonsnummer, skjæringstidspunkt, Ytelsetype.OMSORGSPENGER);

        var forespørselUuid = forespørselTjeneste.opprettForespørselUtenFagsaksnummer(skjæringstidspunkt, aktørId, organisasjonsnummer, Ytelsetype.OMSORGSPENGER, ForespørselType.OMSORGSPENGER_REFUSJON);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var merkelapp = Merkelapp.REFUSJONSKRAV_OMP;
        var skjemaUri = URI.create(arbeidsgiverportalSkjemaLenke + "/refusjon-omsorgspenger/" + organisasjonsnummer.orgnr() + "/" + forespørselUuid);
        var sakstittel = ForespørselTekster.lagSaksTittelRefusjonskrav(person.mapFulltNavn(), person.fødselsdato());
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(), merkelapp, organisasjonsnummer.orgnr(), sakstittel, skjemaUri);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        if (dialogportenEnabled) {
            try {
                opprettForespørselDialogporten(forespørselUuid, new ArbeidsgiverDto(organisasjonsnummer.orgnr()), aktørId, Ytelsetype.OMSORGSPENGER, skjæringstidspunkt);
            } catch (Exception e) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }

        return forespørselUuid;
    }

    public void lukkForespørsel(SaksnummerDto saksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(saksnummer, orgnummerDto, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.getUuid(),
                f.getAktørId(),
                new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()),
                LukkeÅrsak.EKSTERN_INNSENDING,
                List.of(),
                List.of(),
                Optional.empty());
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
        minSideArbeidsgiverTjeneste.slettSak(agPortalSakId);
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

    public void opprettNyeBeskjederMedEksternVarsling(SaksnummerDto saksnummer) {
        List<ForespørselEntitet> åpneForespørsler = forespørselTjeneste.finnÅpneForespørslerForFagsak(saksnummer);
        List<ProsessTaskData> tasker = new ArrayList<>();
        for (var forespørsel : åpneForespørsler) {
            var task = ProsessTaskData.forProsessTask(SendNyBeskjedOgVarselTask.class);
            task.setProperty(SendNyBeskjedOgVarselTask.FORESPØRSEL_UUID, forespørsel.getUuid().toString());
            tasker.add(task);
        }
        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.addNesteParallell(tasker);
        taskGruppe.setSaksnummer(saksnummer.saksnr());
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    public NyBeskjedResultat opprettNyBeskjedMedEksternVarsling(SaksnummerDto saksnummer,
                                                                OrganisasjonsnummerDto organisasjonsnummer,
                                                                LocalDate skjæringstidspunkt) {
        final ForespørselEntitet forespørsel = hentForespørslerForFagsak(saksnummer, organisasjonsnummer, skjæringstidspunkt).stream()
            .filter(f -> f.getStatus() == ForespørselStatus.UNDER_BEHANDLING)
            .findFirst().orElse(null);

        if (forespørsel == null) {
            return NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE;
        }

        opprettNyBeskjedMedEksternVarsling(forespørsel);
        return NyBeskjedResultat.NY_BESKJED_SENDT;
    }

    public void opprettNyBeskjedMedEksternVarsling(ForespørselEntitet forespørsel) {
        Merkelapp merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.getYtelseType());
        UUID forespørselUuid = forespørsel.getUuid();
        URI hentInntektsmeldingPdfUri = URI.create(arbeidsgiverportalSkjemaLenke + "/" + forespørselUuid);
        Organisasjon organisasjon = organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer());
        PersonInfo person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId());
        String varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.getYtelseType(), organisasjon);
        String beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.getYtelseType(), person.mapFulltNavn());

        minSideArbeidsgiverTjeneste.sendNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            merkelapp,
            organisasjon.orgnr(),
            beskjedTekst,
            varselTekst,
            hentInntektsmeldingPdfUri);
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

    private URI lagUriForInntektsmeldingOppsummering(UUID forespørselUuid) {
        return URI.create(arbeidsgiverportalSkjemaLenke + "/" + forespørselUuid);
    }
}
