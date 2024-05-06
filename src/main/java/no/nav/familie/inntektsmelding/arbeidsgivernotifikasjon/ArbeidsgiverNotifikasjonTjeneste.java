package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.AltinnMottakerInput;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.DuplikatEksternIdOgMerkelappResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.MetadataInput;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.MottakerInput;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NotifikasjonInput;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyEksterntVarselResultatResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyOppgaveInput;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyOppgaveMutationRequest;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyOppgaveResultatResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyOppgaveVellykketResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.PaaminnelseResultatResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.UgyldigMerkelappResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.UgyldigMottakerResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.UgyldigPaaminnelseTidspunktResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.UkjentProdusentResponseProjection;
import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.UkjentRolleResponseProjection;

public class ArbeidsgiverNotifikasjonTjeneste implements ArbeidsgiverNotifikasjon {

    private static final String SERVICE_CODE = "4936";
    private static final String SERVICE_EDITION_CODE = "1";

    private ArbeidsgiverNotifikasjonKlient klient;

    @Inject
    public ArbeidsgiverNotifikasjonTjeneste(ArbeidsgiverNotifikasjonKlient klient) {
        this.klient = klient;
    }

    @Override
    public String opprettOppgave(String tekst, URI lenke, Merkelapp merkelapp, String virksomhetsnummer) {

        var request = new NyOppgaveMutationRequest();
        var input = new NyOppgaveInput();
        input.setMottaker(new MottakerInput(new AltinnMottakerInput(SERVICE_CODE, SERVICE_EDITION_CODE), null));
        input.setNotifikasjon(new NotifikasjonInput(lenke.toString(), merkelapp.getBeskrivelse(), tekst));
        input.setMetadata(new MetadataInput(UUID.randomUUID().toString(), null, null, LocalDateTime.now().toString(), virksomhetsnummer));
        request.setNyOppgave(input);

        var projection = new NyOppgaveResultatResponseProjection().typename()
            .onNyOppgaveVellykket(new NyOppgaveVellykketResponseProjection().id()
                .paaminnelse(new PaaminnelseResultatResponseProjection().eksterneVarsler(new NyEksterntVarselResultatResponseProjection().id()))
                .eksterneVarsler(new NyEksterntVarselResultatResponseProjection().id()))
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUgyldigPaaminnelseTidspunkt(new UgyldigPaaminnelseTidspunktResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return klient.opprettOppgave(request, projection);
    }
}
