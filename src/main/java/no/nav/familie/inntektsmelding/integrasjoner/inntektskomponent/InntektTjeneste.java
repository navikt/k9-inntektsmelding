package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;
import no.nav.vedtak.exception.IntegrasjonException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InntektTjeneste {
    private InntektskomponentKlient inntektskomponentKlient;

    InntektTjeneste() {
        // CDI
    }

    @Inject
    public InntektTjeneste(InntektskomponentKlient inntektskomponentKlient) {
        this.inntektskomponentKlient = inntektskomponentKlient;
    }

    public void hentInntekt(AktørId aktørId, LocalDate startdato, String organisasjonsnummer) {
        var request = lagRequest(aktørId, startdato);
        var respons = inntektskomponentKlient.finnInntekt(request);
        oversettRespons(respons, aktørId, organisasjonsnummer);


    }

    private void oversettRespons(HentInntektListeBolkResponse response,
                                 AktørId aktørId,
                                 String organisasjonsnummer) {
        if (response.getSikkerhetsavvikListe() != null && !response.getSikkerhetsavvikListe().isEmpty()) {
            throw new IntegrasjonException("FP-535194",
                String.format("Fikk følgende sikkerhetsavvik ved kall til inntektstjenesten: %s.", byggSikkerhetsavvikString(response)));
        }

        List<ArbeidsInntektIdent> inntektListeRespons = response.getArbeidsInntektIdentListe() == null
            ? Collections.emptyList()
            : response.getArbeidsInntektIdentListe();

        var allInntektForSøker = inntektListeRespons.stream()
            .filter(a -> a.getIdent().getIdentifikator().equals(aktørId.getId()))
            .findFirst()
            .map(ArbeidsInntektIdent::getArbeidsInntektMaaned)
            .orElse(List.of());

        allInntektForSøker.stream().filter(innt -> innt.getArbeidsInntektInformasjon().get)
    }

    private FinnInntektRequest lagRequest(AktørId aktørId, LocalDate startdato) {
        var fomDato = startdato.minusMonths(3);
        var tomDato = startdato.minusMonths(1);

        var fomÅrMåned = YearMonth.of(fomDato.getYear(), fomDato.getMonth());
        var tomÅrMåned = YearMonth.of(tomDato.getYear(), tomDato.getMonth());

        return new FinnInntektRequest(aktørId.getId(), fomÅrMåned, tomÅrMåned);
    }

    private String byggSikkerhetsavvikString(HentInntektListeBolkResponse response) {
        var stringBuilder = new StringBuilder();
        var sikkerhetsavvikListe = response.getSikkerhetsavvikListe();
        if (sikkerhetsavvikListe != null && !sikkerhetsavvikListe.isEmpty()) {
            stringBuilder.append(sikkerhetsavvikListe.get(0).getTekst());
            for (int i = 1; i < sikkerhetsavvikListe.size(); i++) {
                stringBuilder.append(", ");
                stringBuilder.append(sikkerhetsavvikListe.get(i).getTekst());
            }
        }
        return stringBuilder.toString();
    }

}
