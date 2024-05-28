package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ArbeidsgiverNotifikasjonTjeneste implements ArbeidsgiverNotifikasjon {

    static final String SERVICE_CODE = "4936";
    static final String SERVICE_EDITION_CODE = "1";

    private ArbeidsgiverNotifikasjonKlient klient;

    public ArbeidsgiverNotifikasjonTjeneste() {
    }

    @Inject
    public ArbeidsgiverNotifikasjonTjeneste(ArbeidsgiverNotifikasjonKlient klient) {
        this.klient = klient;
    }

    @Override
    public String opprettSak(String grupperingsid, String virksomhetsnummer, String saksTittel, URI lenke, Merkelapp merkelapp) {

        var request = new NySakMutationRequest();

        request.setGrupperingsid(grupperingsid);
        request.setTittel(saksTittel);
        request.setVirksomhetsnummer(virksomhetsnummer);
        request.setMerkelapp(merkelapp.getBeskrivelse());
        request.setLenke(lenke.toString());
        request.setInitiellStatus(SaksStatus.MOTTATT);


        var projection = new NySakResultatResponseProjection().typename()
            .onNySakVellykket(new NySakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatGrupperingsid(new DuplikatGrupperingsidResponseProjection().feilmelding())
            .onDuplikatGrupperingsidEtterDelete(new DuplikatGrupperingsidEtterDeleteResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return klient.opprettSak(request, projection);
    }

    @Override
    public String opprettOppgave(String eksternId,
                                 String grupperingsid,
                                 String virksomhetsnummer,
                                 String notifikasjonTekst,
                                 URI notifikasjonLenke,
                                 Merkelapp notifikasjonMerkelapp) {

        var request = new NyOppgaveMutationRequest();
        var input = new NyOppgaveInput();
        input.setMottaker(new MottakerInput(new AltinnMottakerInput(SERVICE_CODE, SERVICE_EDITION_CODE), null));
        input.setNotifikasjon(new NotifikasjonInput(notifikasjonLenke.toString(), notifikasjonMerkelapp.getBeskrivelse(), notifikasjonTekst));
        input.setMetadata(new MetadataInput(eksternId, grupperingsid, null, null, virksomhetsnummer));
        request.setNyOppgave(input);


        var projection = new NyOppgaveResultatResponseProjection().typename()
            .onNyOppgaveVellykket(new NyOppgaveVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding())
            .onUgyldigPaaminnelseTidspunkt(new UgyldigPaaminnelseTidspunktResponseProjection().feilmelding());

        return klient.opprettOppgave(request, projection);
    }

    @Override
    public String lukkOppgave(String id, LocalDateTime tidspunkt) {

        var request = new OppgaveUtfoertMutationRequest();
        request.setId(id);
        request.setUtfoertTidspunkt(tidspunkt.toString());

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return klient.lukkOppgave(request, projection);
    }
}
