package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

class ArbeidsgiverNotifikasjonTjeneste implements ArbeidsgiverNotifikasjon {

    static final String SERVICE_CODE = "4936";
    static final String SERVICE_EDITION_CODE = "1";

    private ArbeidsgiverNotifikasjonKlient klient;

    @Inject
    public ArbeidsgiverNotifikasjonTjeneste(ArbeidsgiverNotifikasjonKlient klient) {
        this.klient = klient;
    }

    @Override
    public String opprettNyOppgave(String eksternId, String tekst, URI lenke, Merkelapp merkelapp, String virksomhetsnummer, LocalDateTime tidspunkt) {

        var request = new NyOppgaveMutationRequest();
        var input = new NyOppgaveInput();
        input.setMottaker(new MottakerInput(new AltinnMottakerInput(SERVICE_CODE, SERVICE_EDITION_CODE), null));
        input.setNotifikasjon(new NotifikasjonInput(lenke.toString(), merkelapp.getBeskrivelse(), tekst));
        input.setMetadata(new MetadataInput(eksternId, null, null, tidspunkt.toString(), virksomhetsnummer));
        request.setNyOppgave(input);

        var projection = new NyOppgaveResultatResponseProjection().typename()
            .onNyOppgaveVellykket(new NyOppgaveVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUgyldigPaaminnelseTidspunkt(new UgyldigPaaminnelseTidspunktResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return klient.opprettNyOppgave(request, projection);
    }

    @Override
    public String lukkOppgave(String id, LocalDateTime tidspunkt) {

        var request = new OppgaveUtfoertMutationRequest();
        request.setId(id);
        request.setUtfoertTidspunkt(tidspunkt.toString());

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding());
        return klient.lukkOppgave(request, projection);
    }
}
