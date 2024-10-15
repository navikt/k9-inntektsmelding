package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;

public interface ArbeidsgiverNotifikasjon {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke, String statusTekst);

    HentetSak hentSakMedGrupperingsid(String grupperingsid, Merkelapp merkelapp);

    HentetSak hentSak(String sakId);

    String oppdaterSakStatus(String sakId, SaksStatus status, String overstyrtStatusText);

    String oppdaterSakStatusMedGrupperingsId(String grupperingsid, Merkelapp merkelapp, SaksStatus status, String overstyrtStatusText);


    String opprettOppgave(String grupperingsid,
                          Merkelapp merkelapp,
                          String eksternId,
                          String virksomhetsnummer,
                          String oppgaveTekst,
                          String varseltekst,
                          URI lenke);

    String ferdigstillSak(String id, String statusTekst);

    String oppgaveUtført(String oppgaveId, OffsetDateTime utførtTidspunkt);

    String oppgaveUtførtByEksternId(String eksternId, Merkelapp merkelapp, OffsetDateTime tidspunkt);

    String oppgaveUtgått(String oppgaveId, OffsetDateTime utgåttTidspunkt);

}
