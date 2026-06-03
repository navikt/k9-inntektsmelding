package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
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
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.TOM_FORESPOERSEL,
                    "Finner ikke forespørsel for uuid " + request.foresporselUuid(),
                    request.foresporselUuid().toString()));
        }

        if (ForespørselStatus.UTGÅTT.equals(forespørsel.getStatus())) {
            LOG.info("Forespørsel har status utgått, kan ikke motta inntektsmelding. forespørselUuid: {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.UGYLDIG_FORESPOERSEL,
                    "Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel",
                    request.foresporselUuid().toString()));
        }

        InntektsmeldingEntitet nyIm = InntektsmeldingApiMapper.mapTilEntitet(request, aktørId, forespørsel);

        InntektsmeldingEntitet sisteIm = forespørsel.getInntektsmeldinger().stream().findFirst().orElse(null);
        if (sisteIm != null && inntektsmeldingerErLike(nyIm, sisteIm)) {
            LOG.info("Inntektsmelding avvises. Ingen endring sammenlignet med sist innsendt. forespørselUuid: {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.DUPLIKAT,
                    "Inntektsmelding avvises. Ingen endring sammenlignet med sist innsendt inntektsmelding med uuid: " + sisteIm.getUuid(),
                    sisteIm.getUuid().toString()));
        }

        var inntektSjekk = sjekkMånedInntektMotRapportertInntekt(request, aktørId, forespørsel, nyIm);
        if (!inntektSjekk.success()) {
            return inntektSjekk;
        }

        var imId = lagreOgLagJournalførTask(nyIm, forespørsel);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(
            request.foresporselUuid(),
            aktørId,
            new OrganisasjonsnummerDto(request.organisasjonsnummer().orgnr()),
            LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.of(nyIm.getUuid())
        );

        var lagretEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(lagretEntitet);

        return new SendInntektsmeldingResponse(true, lagretEntitet.getUuid(), null);
    }

    private SendInntektsmeldingResponse sjekkMånedInntektMotRapportertInntekt(SendInntektsmeldingRequest request,
                                                                               AktørIdEntitet aktørId,
                                                                               ForespørselEntitet forespørsel,
                                                                               InntektsmeldingEntitet entitet) {
        Inntektsopplysninger inntektFraAInntekt = inntektTjeneste.hentInntekt(
            aktørId,
            forespørsel.getSkjæringstidspunkt(),
            LocalDate.now(),
            request.organisasjonsnummer().orgnr(),
            forespørsel.getYtelseType()
        );

        var nedetidAInntekt = inntektFraAInntekt.måneder() != null && inntektFraAInntekt.måneder().stream()
            .anyMatch(m -> MånedslønnStatus.NEDETID_AINNTEKT.equals(m.status()));

        if (nedetidAInntekt) {
            LOG.warn("Inntektskomponenten har nedetid. forespørselUuid: {}", request.foresporselUuid());
            return new SendInntektsmeldingResponse(false, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.NEDETID_AINNTEKT,
                    "Inntektskomponenten har nedetid, og vi kan ikke verifisere inntekt. Prøv igjen om litt.",
                    request.foresporselUuid().toString()));
        }

        var inntektErUlikOgIngenÅrsakOppgitt = inntektFraAInntekt.gjennomsnitt() != null
            && inntektFraAInntekt.gjennomsnitt().subtract(entitet.getMånedInntekt()).abs().compareTo(AKSEPTERT_AVVIK) > 0
            && (entitet.getEndringsårsaker() == null || entitet.getEndringsårsaker().isEmpty());

        if (inntektErUlikOgIngenÅrsakOppgitt) {
            var feilmelding = String.format(
                "Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt. Gjennomsnittlig inntekt fra A-inntekt: %s, oppgitt inntekt: %s",
                inntektFraAInntekt.gjennomsnitt(), entitet.getMånedInntekt());
            return new SendInntektsmeldingResponse(false, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.ULIK_INNTEKT, feilmelding, request.foresporselUuid().toString()));
        }

        return new SendInntektsmeldingResponse(true, null, null);
    }

    private boolean inntektsmeldingerErLike(InntektsmeldingEntitet ny, InntektsmeldingEntitet gammel) {
        return Objects.equals(ny.getStartDato(), gammel.getStartDato())
            && Objects.equals(ny.getMånedInntekt(), gammel.getMånedInntekt())
            && Objects.equals(ny.getMånedRefusjon(), gammel.getMånedRefusjon())
            && Objects.equals(ny.getOpphørsdatoRefusjon(), gammel.getOpphørsdatoRefusjon())
            && Objects.equals(ny.getKontaktperson().getNavn(), gammel.getKontaktperson().getNavn())
            && Objects.equals(ny.getKontaktperson().getTelefonnummer(), gammel.getKontaktperson().getTelefonnummer());
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingEntitet inntektsmelding, ForespørselEntitet forespørsel) {
        LOG.info("Lagrer inntektsmelding fra LPS-system for ytelse {} og saksnummer {}", inntektsmelding.getYtelsetype(), forespørsel.getSaksnummer().orElse(null));
        var imId = inntektsmeldingRepository.lagreInntektsmelding(inntektsmelding);
        opprettTaskForSendTilJoark(inntektsmelding, forespørsel, imId);
        return imId;
    }

    private void opprettTaskForSendTilJoark(InntektsmeldingEntitet inntektsmelding, ForespørselEntitet forespørsel, Long imId) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        forespørsel.getSaksnummer().ifPresent(task::setSaksnummer);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_YTELSE_TYPE, inntektsmelding.getYtelsetype().toString());
        prosessTaskTjeneste.lagre(task);
    }
}
