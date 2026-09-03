package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.task.FerdigstillInntektsmeldingEtterNedetidTask;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.FeilInfo;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerResponse;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class InntektsmeldingApiMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiMottakTjeneste.class);
    private static final BigDecimal AKSEPTERT_AVVIK = new BigDecimal("50");

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private InntektTjeneste inntektTjeneste;

    InntektsmeldingApiMottakTjeneste() {
        // CDI
    }

    @Inject
    public InntektsmeldingApiMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                            InntektsmeldingRepository inntektsmeldingRepository,
                                            ProsessTaskTjeneste prosessTaskTjeneste,
                                            InntektTjeneste inntektTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    public SendInntektsmeldingResponse mottaInntektsmelding(SendInntektsmeldingRequest request, AktørIdEntitet aktørId) {
        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(request.foresporselUuid()).orElse(null);
        if (forespørsel == null) {
            LOG.info("Finner ikke forespørsel for uuid {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new FeilInfo(FeilkodeDto.TOM_FORESPOERSEL,
                    "Finner ikke forespørsel for uuid " + request.foresporselUuid(),
                    request.foresporselUuid().toString()));
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.getStatus())) {
            LOG.info("Forespørsel har status utgått, kan ikke motta inntektsmelding. forespørselUuid: {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new FeilInfo(FeilkodeDto.UGYLDIG_FORESPOERSEL,
                    "Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel",
                    request.foresporselUuid().toString()));
        }

        InntektsmeldingEntitet nyIm = InntektsmeldingApiMapper.mapTilEntitet(request, aktørId, forespørsel);

        InntektsmeldingEntitet sisteIm = forespørsel.getInntektsmeldinger().stream()
            .max(java.util.Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt))
            .orElse(null);
        if (sisteIm != null && inntektsmeldingerErLike(nyIm, sisteIm)) {
            LOG.info("Inntektsmelding avvises. Ingen endring sammenlignet med sist innsendt. forespørselUuid: {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new FeilInfo(FeilkodeDto.DUPLIKAT,
                    "Inntektsmelding avvises. Ingen endring sammenlignet med sist innsendt inntektsmelding med uuid: " + sisteIm.getUuid(),
                    sisteIm.getUuid().toString()));
        }

        settForrigeUtdatertHvisVenterVurdering(forespørsel);

        Optional<FeilInfo> inntektFeil = sjekkInntektMotRapportertInntekt(
            aktørId,
            request.organisasjonsnummer().orgnr(),
            forespørsel.getSkjæringstidspunkt(),
            forespørsel.getYtelseType(),
            nyIm.getMånedInntekt(),
            nyIm.getEndringsårsaker() != null && !nyIm.getEndringsårsaker().isEmpty(),
            request.foresporselUuid());
        if (inntektFeil.isPresent()) {
            if (FeilkodeDto.NEDETID_AINNTEKT.equals(inntektFeil.get().feilkode())) {
                nyIm.setStatus(InntektsmeldingStatus.VENTER_VURDERING);
                Long imId = lagreImOgOpprettTaskForEtterkontroll(nyIm, forespørsel);
                var lagretEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
                return new SendInntektsmeldingResponse(true, lagretEntitet.getUuid(), inntektFeil.get());
            }
            return new SendInntektsmeldingResponse(false, null, inntektFeil.get());
        }

        Long imId = lagreOgLagJournalførTask(nyIm, forespørsel);
        OrganisasjonsnummerDto orgnummer = new OrganisasjonsnummerDto(request.organisasjonsnummer().orgnr());

        // ved første im skal vi ferdigstille forespørsel. Ved andre skal vi oppdatere arbeidsgiverportalen og dialogporten
        if (sisteIm == null) {
            forespørselBehandlingTjeneste.ferdigstillForespørsel(
                request.foresporselUuid(), aktørId, orgnummer, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(nyIm));
        } else {
            forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(
                forespørsel, orgnummer, Optional.ofNullable(nyIm.getUuid()));
        }

        InntektsmeldingEntitet lagretEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretEntitet);

        return new SendInntektsmeldingResponse(true, lagretEntitet.getUuid(), null);
    }

    public SendRefusjonOmsorgspengerResponse mottaInntektsmeldingForOmsorgspengerRefusjon(SendRefusjonOmsorgspengerRequest request,
                                                                                          AktørIdEntitet aktørId) {
        var orgnummer = new OrganisasjonsnummerDto(request.organisasjonsnummer().orgnr());

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForOmsorgspengerRefusjonIm(aktørId, orgnummer, request.startdato());

        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke nyopprettet forespørsel: " + forespørselUuid));

        var nyIm = InntektsmeldingApiMapper.mapTilEntitetOmsorgspengerRefusjon(request, aktørId, forespørsel);

        List<InntektsmeldingEntitet> tidligereInntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(
            request.organisasjonsnummer().orgnr(),
            aktørId,
            Ytelsetype.OMSORGSPENGER,
            request.startdato(),
            null,
            null );

        InntektsmeldingEntitet sisteIm = tidligereInntektsmeldinger.stream()
            .max(java.util.Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt))
            .orElse(null);

        if (sisteIm != null && inntektsmeldingerErLike(nyIm, sisteIm)) {
            LOG.info("Refusjonskrav avvises. Ingen endring sammenlignet med sist innsendt.");
            return new SendRefusjonOmsorgspengerResponse(false, null,
                new FeilInfo(FeilkodeDto.DUPLIKAT,
                    "Refusjonskrav avvises. Ingen endring sammenlignet med sist innsendt inntektsmelding med uuid: " + sisteIm.getUuid(),
                    sisteIm.getUuid().toString()));
        }

        settForrigeUtdatertHvisVenterVurdering(forespørsel);

        Optional<FeilInfo> inntektFeil = sjekkInntektMotRapportertInntekt(
            aktørId,
            request.organisasjonsnummer().orgnr(),
            request.startdato(),
            Ytelsetype.OMSORGSPENGER,
            request.inntekt(),
            request.endringAvInntektÅrsaker() != null && !request.endringAvInntektÅrsaker().isEmpty(),
            forespørselUuid);
        if (inntektFeil.isPresent()) {
            if (FeilkodeDto.NEDETID_AINNTEKT.equals(inntektFeil.get().feilkode())) {
                nyIm.setStatus(InntektsmeldingStatus.VENTER_VURDERING);
                Long imId = lagreImOgOpprettTaskForEtterkontroll(nyIm, forespørsel);
                var lagretEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
                return new SendRefusjonOmsorgspengerResponse(true, lagretEntitet.getUuid(), inntektFeil.get());
            }
            return new SendRefusjonOmsorgspengerResponse(false, null, inntektFeil.get());
        }

        Long imId = lagreOgLagJournalførTask(nyIm, forespørsel);
        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, aktørId, orgnummer, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(nyIm));

        var lagretEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
        MetrikkerTjeneste.logginnsendtImOmsorgspengerRefusjon(lagretEntitet);

        return new SendRefusjonOmsorgspengerResponse(true, lagretEntitet.getUuid(), null);
    }

    private Optional<FeilInfo> sjekkInntektMotRapportertInntekt(AktørIdEntitet aktørId,
                                                                String orgnr,
                                                                LocalDate skjæringstidspunkt,
                                                                Ytelsetype ytelseType,
                                                                BigDecimal månedInntekt,
                                                                boolean harEndringsårsaker,
                                                                UUID forespørselUuid) {
        Inntektsopplysninger inntektFraAInntekt = inntektTjeneste.hentInntekt(aktørId, skjæringstidspunkt, LocalDate.now(), orgnr, ytelseType);

        if (inntektFraAInntekt.harNedetid()) {
            LOG.warn("Inntektskomponenten har nedetid. ForespørselUuid: {}", forespørselUuid);
            return Optional.of(new FeilInfo(FeilkodeDto.NEDETID_AINNTEKT,
                "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt. Prøv igjen om litt.",
                String.valueOf(forespørselUuid)));
        }

        boolean inntektErUlikOgIngenÅrsakOppgitt = inntektFraAInntekt.gjennomsnitt() != null
            && inntektFraAInntekt.gjennomsnitt().subtract(månedInntekt).abs().compareTo(AKSEPTERT_AVVIK) > 0
            && !harEndringsårsaker;

        if (inntektErUlikOgIngenÅrsakOppgitt) {
            String feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt: %s",
                inntektFraAInntekt.gjennomsnitt(), månedInntekt);
            LOG.info("Ulik inntekt uten endringsårsak. orgnr: {}, startdato: {}, forespørselUuid: {}",
                new OrganisasjonsnummerDto(orgnr), skjæringstidspunkt, forespørselUuid);
            return Optional.of(new FeilInfo(FeilkodeDto.ULIK_INNTEKT, feilmelding, String.valueOf(forespørselUuid)));
        }

        return Optional.empty();
    }

    private boolean inntektsmeldingerErLike(InntektsmeldingEntitet ny, InntektsmeldingEntitet gammel) {
        return Objects.equals(ny.getStartDato(), gammel.getStartDato())
            && Objects.equals(ny.getMånedInntekt(), gammel.getMånedInntekt())
            && Objects.equals(ny.getMånedRefusjon(), gammel.getMånedRefusjon())
            && Objects.equals(ny.getOpphørsdatoRefusjon(), gammel.getOpphørsdatoRefusjon())
            && Objects.equals(ny.getKontaktperson().getNavn(), gammel.getKontaktperson().getNavn())
            && Objects.equals(ny.getKontaktperson().getTelefonnummer(), gammel.getKontaktperson().getTelefonnummer())
            && new HashSet<>(ny.getBorfalteNaturalYtelser()).equals(new HashSet<>(gammel.getBorfalteNaturalYtelser()))
            && new HashSet<>(ny.getRefusjonsendringer()).equals(new HashSet<>(gammel.getRefusjonsendringer()))
            && new HashSet<>(ny.getEndringsårsaker()).equals(new HashSet<>(gammel.getEndringsårsaker()))
            && Objects.equals(ny.getOmsorgspenger(), gammel.getOmsorgspenger());
    }

    public void kontrollerInntektsmeldingEtterNedetid(Long inntektsmeldingId) {
        var inntektsmelding = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
        if (InntektsmeldingStatus.UTDATERT.equals(inntektsmelding.getStatus())) {
            LOG.info("Inntektsmelding {} er utdatert, hopper over etterkontroll etter nedetid", inntektsmelding.getUuid());
            return;
        }

        var forespørsel = inntektsmelding.getForespørsel();
        Inntektsopplysninger inntektFraAInntekt = inntektTjeneste.hentInntekt(
            inntektsmelding.getAktørId(),
            forespørsel.getSkjæringstidspunkt(),
            LocalDate.now(),
            inntektsmelding.getArbeidsgiverIdent(),
            forespørsel.getYtelseType());

        if (inntektFraAInntekt.harNedetid()) {
            inntektsmeldingRepository.oppdaterStatus(inntektsmelding.getUuid(), InntektsmeldingStatus.VENTER_VURDERING);
            // Kaster feil som fører til at vi vil opprette en ny prosesstask som retryer
            throw new TekniskException("K9INNTEKTSMELDING_NEDETID_1", "Nedetid i a-inntekt, får ikke ferdigstilt inntektsmelding " + inntektsmeldingId);
        }

        boolean inntektErUgyldig = inntektFraAInntekt.gjennomsnitt() != null
            && inntektFraAInntekt.gjennomsnitt().subtract(inntektsmelding.getMånedInntekt()).abs().compareTo(AKSEPTERT_AVVIK) > 0
            && (inntektsmelding.getEndringsårsaker() == null || inntektsmelding.getEndringsårsaker().isEmpty());

        if (inntektErUgyldig) {
            inntektsmeldingRepository.oppdaterStatus(inntektsmelding.getUuid(), InntektsmeldingStatus.AVVIST);
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt: %s",
                inntektFraAInntekt.gjennomsnitt(), inntektsmelding.getMånedInntekt());
            forespørselBehandlingTjeneste.sendMeldingOmAvvistInntektsmelding(forespørsel, feilmelding);
        } else {
            inntektsmeldingRepository.oppdaterStatus(inntektsmelding.getUuid(), InntektsmeldingStatus.GODKJENT);
            opprettTaskForSendTilJoark(inntektsmelding, forespørsel, inntektsmeldingId);
            OrganisasjonsnummerDto orgnummer = new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiverIdent());
            if (ForespørselStatus.FERDIG.equals(forespørsel.getStatus())) {
                forespørselBehandlingTjeneste.oppdaterPortalerMedEndretInntektsmelding(
                    forespørsel, orgnummer, Optional.ofNullable(inntektsmelding.getUuid()));
            } else {
                forespørselBehandlingTjeneste.ferdigstillForespørsel(
                    forespørsel.getUuid(), inntektsmelding.getAktørId(), orgnummer, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(inntektsmelding));
            }
            MetrikkerTjeneste.loggInnsendtInntektsmelding(inntektsmelding);
        }
    }

    private void settForrigeUtdatertHvisVenterVurdering(ForespørselEntitet forespørsel) {
        // burde lage en felles kode for å hente siste inntektsmelding
        forespørsel.getInntektsmeldinger().stream()
            .max(java.util.Comparator.comparing(InntektsmeldingEntitet::getOpprettetTidspunkt))
            .filter(im -> InntektsmeldingStatus.VENTER_VURDERING.equals(im.getStatus()))
            .ifPresent(im -> {
                LOG.info("Forrige inntektsmelding {} venter på vurdering. Setter status utdatert.", im.getUuid());
                inntektsmeldingRepository.oppdaterStatus(im.getUuid(), InntektsmeldingStatus.UTDATERT);
            });
    }

    private Long lagreImOgOpprettTaskForEtterkontroll(InntektsmeldingEntitet inntektsmelding, ForespørselEntitet forespørsel) {
        LOG.info("Lagrer inntektsmelding med status VENTER_VURDERING for ytelse {} og saksnummer {}", inntektsmelding.getYtelsetype(), forespørsel.getSaksnummer().orElse(null));
        Long imId = inntektsmeldingRepository.lagreInntektsmelding(inntektsmelding);
        ProsessTaskData task = ProsessTaskData.forProsessTask(FerdigstillInntektsmeldingEtterNedetidTask.class);
        forespørsel.getSaksnummer().ifPresent(task::setSaksnummer);
        task.setProperty(FerdigstillInntektsmeldingEtterNedetidTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        prosessTaskTjeneste.lagre(task);
        return imId;
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingEntitet inntektsmelding, ForespørselEntitet forespørsel) {
        LOG.info("Lagrer inntektsmelding fra LPS-system for ytelse {} og saksnummer {}", inntektsmelding.getYtelsetype(), forespørsel.getSaksnummer().orElse(null));
        Long imId = inntektsmeldingRepository.lagreInntektsmelding(inntektsmelding);
        opprettTaskForSendTilJoark(inntektsmelding, forespørsel, imId);
        return imId;
    }

    private void opprettTaskForSendTilJoark(InntektsmeldingEntitet inntektsmelding, ForespørselEntitet forespørsel, Long imId) {
        ProsessTaskData task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        forespørsel.getSaksnummer().ifPresent(task::setSaksnummer);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_YTELSE_TYPE, inntektsmelding.getYtelsetype().toString());
        prosessTaskTjeneste.lagre(task);
    }
}
