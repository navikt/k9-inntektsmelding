package no.nav.familie.inntektsmelding.server.audit;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.felles.log.audit.Auditdata;
import no.nav.k9.felles.log.audit.AuditdataHeader;
import no.nav.k9.felles.log.audit.Auditlogger;
import no.nav.k9.felles.log.audit.CefField;
import no.nav.k9.felles.log.audit.CefFieldName;
import no.nav.k9.felles.log.audit.EventClassId;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Dependent
public class SporingsloggTjeneste {
    private Auditlogger auditlogger;
    private static final String SAKSNUMMER_TEXT = "Saksnummer";

    public SporingsloggTjeneste() {
    }

    @Inject
    public SporingsloggTjeneste(Auditlogger auditlogger) {
        this.auditlogger = auditlogger;
    }

    public void logg(String url, String brukerId, String saksnummer) {
        String saksbehandlerIdent = finnSaksbehandlerIdent();

        AuditdataHeader header = new AuditdataHeader.Builder()
            .medVendor(auditlogger.getDefaultVendor())
            .medProduct(auditlogger.getDefaultProduct())
            .medSeverity("INFO")
            .medEventClassId(EventClassId.AUDIT_ACCESS)
            .build();

        Set<CefField> fields = Set.of(
            new CefField(CefFieldName.EVENT_TIME, System.currentTimeMillis()),
            new CefField(CefFieldName.REQUEST, url),
            new CefField(CefFieldName.USER_ID, saksbehandlerIdent),
            new CefField(CefFieldName.BERORT_BRUKER_ID, brukerId),
            new CefField(CefFieldName.SAKSNUMMER_VERDI, saksnummer),
            new CefField(CefFieldName.SAKSNUMMER_LABEL, SAKSNUMMER_TEXT)
        );

        auditlogger.logg(new Auditdata(header, fields));
    }

    private String finnSaksbehandlerIdent() {
        if (!KontekstHolder.harKontekst() || !IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }

        return KontekstHolder.getKontekst().getUid();
    }
}
