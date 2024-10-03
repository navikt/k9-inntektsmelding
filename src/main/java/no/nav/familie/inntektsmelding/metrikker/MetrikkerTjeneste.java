package no.nav.familie.inntektsmelding.metrikker;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.konfig.Tid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

public class MetrikkerTjeneste {
    // Måler opprettelse av oppgaver per ytelse
    private static final String COUNTER_FORESPØRRSEL = "ftinntektsmelding.oppgaver";

    // Måler mottak av inntektsmeldinger i ftinntektsmelding per ytelse
    private static final String COUNTER_INNTEKTSMELDING = "ftinntektsmelding.inntektsmeldinger";

    // Måler årsaker til endring av inntekt i inntektsmeldinger innsendt i ftinntektsmelding
    private static final Map<Ytelsetype, String> COUNTER_YTELLSE_METRIC_ÅRSAK_MAP = Map.of(Ytelsetype.FORELDREPENGER, "ftinntektsmelding.inntektsmeldinger.fp.endringsaarsak",
        Ytelsetype.SVANGERSKAPSPENGER, "ftinntektsmelding.inntektsmeldinger.svp.endringsaarsak",
        Ytelsetype.OMSORGSPENGER, "ftinntektsmelding.inntektsmeldinger.omp.endringsaarsak",
        Ytelsetype.PLEIEPENGER_SYKT_BARN, "ftinntektsmelding.inntektsmeldinger.psb.endringsaarsak",
        Ytelsetype.PLEIEPENGER_NÆRSTÅENDE, "ftinntektsmelding.inntektsmeldinger.ppn.endringsaarsak");

    // Måler hvor mange oppgaver som lukkes etter at fagsystem melder om en inntektsmelding ftinntektsmelding ikke kjenner til
    private static final String COUNTER_LUKK_EKSTERN = "ftinntektsmelding.oppgaver.lukk.ekstern";

    public static void loggForespørselOpprettet(Ytelsetype ytelsetype) {
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag("ytelse", ytelsetype.name()));
        Metrics.counter(COUNTER_FORESPØRRSEL, tags).increment();
    }

    public static void loggForespørselLukkEkstern(Ytelsetype ytelsetype) {
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag("ytelse", ytelsetype.name()));
        Metrics.counter(COUNTER_LUKK_EKSTERN, tags).increment();
    }

    public static void loggInnsendtInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        var tags = new ArrayList<Tag>();
        var harOppgittRefusjon = inntektsmelding.getMånedRefusjon() != null && inntektsmelding.getMånedRefusjon().compareTo(BigDecimal.ZERO) > 0;
        var harOppgittEndringerIRefusjon = inntektsmelding.getRefusjonsendringer() != null && !inntektsmelding.getRefusjonsendringer().isEmpty();
        var harOppgittOpphørAvRefusjon = inntektsmelding.getOpphørsdatoRefusjon() != null && !inntektsmelding.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE);
        var harOppgittNaturalytelse = inntektsmelding.getBorfalteNaturalYtelser() != null && !inntektsmelding.getBorfalteNaturalYtelser().isEmpty();

        tags.add(new ImmutableTag("har_oppgitt_refusjon", harOppgittRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_endring_i_refusjon", harOppgittEndringerIRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_opppphør_av_refusjon", harOppgittOpphørAvRefusjon ? "Ja" : "Nei"));
        tags.add(new ImmutableTag("har_oppgitt_naturalytelse", harOppgittNaturalytelse ? "Ja" : "Nei"));
        Metrics.counter(COUNTER_INNTEKTSMELDING, tags).increment();

        if (!inntektsmelding.getEndringsårsaker().isEmpty()) {
            var tagsÅrsaker = new ArrayList<Tag>();
            inntektsmelding.getEndringsårsaker().forEach(endring -> tagsÅrsaker.add(new ImmutableTag("aarsak", endring.getÅrsak().name())));
            Metrics.counter(COUNTER_YTELLSE_METRIC_ÅRSAK_MAP.get(inntektsmelding.getYtelsetype()), tagsÅrsaker).increment();
        }
    }
}
