package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.OffsetDateTime;

public interface ArbeidsgiverNotifikasjon {

    String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke);

    String oppdaterSakTilleggsinformasjon(String id, String overstyrtTillagsinformasjon);

    String ferdigstillSak(String id);

    String opprettOppgave(String grupperingsid,
                          Merkelapp merkelapp,
                          String eksternId,
                          String virksomhetsnummer,
                          String oppgaveTekst,
                          String varselTekst,
                          String påminnelseTekst,
                          URI lenke);

    String oppgaveUtført(String oppgaveId, OffsetDateTime utførtTidspunkt);

    String oppgaveUtgått(String oppgaveId, OffsetDateTime utgåttTidspunkt);

    String slettSak(String id);
}
