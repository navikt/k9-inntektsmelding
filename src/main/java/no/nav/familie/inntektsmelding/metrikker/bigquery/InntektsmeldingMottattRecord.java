package no.nav.familie.inntektsmelding.metrikker.bigquery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.k9.felles.integrasjon.bigquery.tabell.BigQueryRecord;
import no.nav.k9.felles.integrasjon.bigquery.tabell.BigQueryTabellDefinisjon;
import no.nav.vedtak.konfig.Tid;

public record InntektsmeldingMottattRecord(
    UUID uuid,
    LocalDateTime opprettetTidspunkt,
    Ytelsetype ytelsetype,
    Kildesystem kildesystem,
    InntektsmeldingType inntektsmeldingType,
    boolean harEndretInntekt,
    boolean harOppgittRefusjon,
    boolean harOppgittEndringerIRefusjon,
    boolean harOppgittOpphørAvRefusjon,
    boolean harOppgittNaturalytelse
) implements BigQueryRecord {

    public static InntektsmeldingMottattRecord fra(InntektsmeldingEntitet inntektsmelding) {
        var månedRefusjon = inntektsmelding.getMånedRefusjon();
        var opphørsdatoRefusjon = inntektsmelding.getOpphørsdatoRefusjon();
        return new InntektsmeldingMottattRecord(
            inntektsmelding.getUuid(),
            inntektsmelding.getOpprettetTidspunkt(),
            inntektsmelding.getYtelsetype(),
            inntektsmelding.getKildesystem(),
            inntektsmelding.getInntektsmeldingType(),
            !inntektsmelding.getEndringsårsaker().isEmpty(),
            månedRefusjon != null && månedRefusjon.compareTo(BigDecimal.ZERO) > 0,
            !inntektsmelding.getRefusjonsendringer().isEmpty(),
            opphørsdatoRefusjon != null && !opphørsdatoRefusjon.equals(Tid.TIDENES_ENDE),
            !inntektsmelding.getBorfalteNaturalYtelser().isEmpty()
        );
    }

    @Override
    public BigQueryTabellDefinisjon tabellDefinisjon() {
        return InntektsmeldingMottattTabell.INSTANCE;
    }
}
