package no.nav.familie.inntektsmelding.metrikker.bigquery;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;

import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableInfo;

import no.nav.k9.felles.integrasjon.bigquery.tabell.BigQueryRecord;
import no.nav.k9.felles.integrasjon.bigquery.tabell.BigQueryTabellDefinisjon;

public final class InntektsmeldingMottattTabell implements BigQueryTabellDefinisjon {
    public static final InntektsmeldingMottattTabell INSTANCE = new InntektsmeldingMottattTabell();

    static final String UUID = "uuid";
    static final String OPPRETTET_TID = "opprettet_tid";
    static final String YTELSE_TYPE = "ytelse_type";
    static final String KILDESYSTEM = "kildesystem";
    static final String INNTEKTSMELDING_TYPE = "inntektsmelding_type";
    static final String HAR_ENDRET_INNTEKT = "har_endret_inntekt";
    static final String HAR_OPPGITT_REFUSJON = "har_oppgitt_refusjon";
    static final String HAR_OPPGITT_ENDRING_I_REFUSJON = "har_oppgitt_endring_i_refusjon";
    static final String HAR_OPPGITT_OPPHOER_AV_REFUSJON = "har_oppgitt_opphoer_av_refusjon";
    static final String HAR_OPPGITT_NATURALYTELSE = "har_oppgitt_naturalytelse";

    private static final ZoneId TIDSSONE = ZoneId.of("Europe/Oslo");

    private InntektsmeldingMottattTabell() {
    }

    @Override
    public String getTabellNavn() {
        return "inntektsmelding_mottatt_v1";
    }

    @Override
    public Schema getSchema() {
        return Schema.of(
            BigQueryTabellDefinisjon.newRequiredField(UUID, StandardSQLTypeName.STRING),
            BigQueryTabellDefinisjon.newRequiredField(OPPRETTET_TID, StandardSQLTypeName.TIMESTAMP),
            BigQueryTabellDefinisjon.newRequiredField(YTELSE_TYPE, StandardSQLTypeName.STRING),
            BigQueryTabellDefinisjon.newRequiredField(KILDESYSTEM, StandardSQLTypeName.STRING),
            BigQueryTabellDefinisjon.newRequiredField(INNTEKTSMELDING_TYPE, StandardSQLTypeName.STRING),
            BigQueryTabellDefinisjon.newRequiredField(HAR_ENDRET_INNTEKT, StandardSQLTypeName.BOOL),
            BigQueryTabellDefinisjon.newRequiredField(HAR_OPPGITT_REFUSJON, StandardSQLTypeName.BOOL),
            BigQueryTabellDefinisjon.newRequiredField(HAR_OPPGITT_ENDRING_I_REFUSJON, StandardSQLTypeName.BOOL),
            BigQueryTabellDefinisjon.newRequiredField(HAR_OPPGITT_OPPHOER_AV_REFUSJON, StandardSQLTypeName.BOOL),
            BigQueryTabellDefinisjon.newRequiredField(HAR_OPPGITT_NATURALYTELSE, StandardSQLTypeName.BOOL)
        );
    }

    @Override
    public Function<BigQueryRecord, Map<String, ?>> getRowMapper(Instant now) {
        return rec -> {
            var r = (InntektsmeldingMottattRecord) rec;
            return Map.of(
                UUID, r.uuid().toString(),
                OPPRETTET_TID, r.opprettetTidspunkt().atZone(TIDSSONE).toInstant().toString(),
                YTELSE_TYPE, r.ytelsetype().name(),
                KILDESYSTEM, r.kildesystem().name(),
                INNTEKTSMELDING_TYPE, r.inntektsmeldingType().name(),
                HAR_ENDRET_INNTEKT, r.harEndretInntekt(),
                HAR_OPPGITT_REFUSJON, r.harOppgittRefusjon(),
                HAR_OPPGITT_ENDRING_I_REFUSJON, r.harOppgittEndringerIRefusjon(),
                HAR_OPPGITT_OPPHOER_AV_REFUSJON, r.harOppgittOpphørAvRefusjon(),
                HAR_OPPGITT_NATURALYTELSE, r.harOppgittNaturalytelse()
            );
        };
    }

    @Override
    public TableInfo tilTableInfo(String dataset) {
        return BigQueryTabellDefinisjon.partisjonertTabell(dataset, getTabellNavn(), getSchema(), OPPRETTET_TID);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof BigQueryTabellDefinisjon other
            && getTabellNavn().equals(other.getTabellNavn()));
    }

    @Override
    public int hashCode() {
        return getTabellNavn().hashCode();
    }
}
