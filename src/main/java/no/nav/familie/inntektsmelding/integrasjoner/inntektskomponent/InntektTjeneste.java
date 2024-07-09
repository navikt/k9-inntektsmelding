package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.tjenester.aordningen.inntektsinformasjon.ArbeidsInntektIdent;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.Inntekt;
import no.nav.tjenester.aordningen.inntektsinformasjon.inntekt.InntektType;
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeBolkResponse;
import no.nav.vedtak.exception.IntegrasjonException;

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

    public List<Månedsinntekt> hentInntekt(AktørIdEntitet aktørId, LocalDate startdato, String organisasjonsnummer) {
        var request = lagRequest(aktørId, startdato);
        var respons = inntektskomponentKlient.finnInntekt(request);
        return oversettRespons(respons, aktørId, organisasjonsnummer);
    }

    private List<Månedsinntekt> oversettRespons(HentInntektListeBolkResponse response, AktørIdEntitet aktørId, String organisasjonsnummer) {
        if (response.getSikkerhetsavvikListe() != null && !response.getSikkerhetsavvikListe().isEmpty()) {
            throw new IntegrasjonException("FP-535194",
                String.format("Fikk følgende sikkerhetsavvik ved kall til inntektstjenesten: %s.", byggSikkerhetsavvikString(response)));
        }

        List<ArbeidsInntektIdent> inntektListeRespons =
            response.getArbeidsInntektIdentListe() == null ? Collections.emptyList() : response.getArbeidsInntektIdentListe();

        var inntektPerMånedForBruker = inntektListeRespons.stream()
            .filter(a -> a.getIdent().getIdentifikator().equals(aktørId.getAktørId()))
            .findFirst()
            .map(ArbeidsInntektIdent::getArbeidsInntektMaaned)
            .orElse(List.of());

        List<Månedsinntekt> månedsInntektListe = new ArrayList<>();

        inntektPerMånedForBruker.forEach(inntektMåned -> inntektMåned.getArbeidsInntektInformasjon()
            .getInntektListe()
            .stream()
            .filter(inntekt -> InntektType.LOENNSINNTEKT.equals(inntekt.getInntektType()) && organisasjonsnummer.equals(
                inntekt.getVirksomhet().getIdentifikator()))
            .findFirst()
            .map(this::mapMånedsInntekt)
            .ifPresent(månedsInntektListe::add));

        return månedsInntektListe;
    }

    private Månedsinntekt mapMånedsInntekt(Inntekt månedsInntekt) {
        return new Månedsinntekt(månedsInntekt.getUtbetaltIMaaned(), månedsInntekt.getBeloep(), månedsInntekt.getVirksomhet().getIdentifikator());
    }

    public record Månedsinntekt(YearMonth måned, BigDecimal beløp, String organisasjonsnummer) {
    }

    private FinnInntektRequest lagRequest(AktørIdEntitet aktørId, LocalDate startdato) {
        var fomDato = startdato.minusMonths(3);
        var tomDato = startdato.minusMonths(1);

        var fomÅrMåned = YearMonth.of(fomDato.getYear(), fomDato.getMonth());
        var tomÅrMåned = YearMonth.of(tomDato.getYear(), tomDato.getMonth());

        return new FinnInntektRequest(aktørId.getAktørId(), fomÅrMåned, tomÅrMåned);
    }

    private String byggSikkerhetsavvikString(HentInntektListeBolkResponse response) {
        var stringBuilder = new StringBuilder();
        var sikkerhetsavvikListe = response.getSikkerhetsavvikListe();
        if (sikkerhetsavvikListe != null && !sikkerhetsavvikListe.isEmpty()) {
            stringBuilder.append(sikkerhetsavvikListe.getFirst().getTekst());
            for (int i = 1; i < sikkerhetsavvikListe.size(); i++) {
                stringBuilder.append(", ");
                stringBuilder.append(sikkerhetsavvikListe.get(i).getTekst());
            }
        }
        return stringBuilder.toString();
    }

}
