package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDateTime;

public interface ArbeidsgiverNotifikasjon {

    String opprettSak(String grupperingsid, String virksomhetsnummer, String saksTittel, URI lenke, Merkelapp merkelapp);

    String opprettOppgave(String eksternId,
                          String grupperingsid,
                          String virksomhetsnummer,
                          String notifikasjonsTekst,
                          URI lenke,
                          Merkelapp merkelapp);

    String lukkOppgave(String id, LocalDateTime utfoertTidspunkt);
}
