package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;

@Dependent
public class DokgenKlientSelector {
    private final K9DokgenKlient k9DokgenKlient;
    private final K9PdfgenKlient k9PdfgenKlient;
    private final boolean usePdfgen;

    @Inject
    public DokgenKlientSelector(
        K9DokgenKlient k9DokgenKlient,
        K9PdfgenKlient k9PdfgenKlient) {
        this.k9DokgenKlient = k9DokgenKlient;
        this.k9PdfgenKlient = k9PdfgenKlient;
        this.usePdfgen = Environment.current().getProperty("bruk.pdfgen", boolean.class, false);
    }

    public DokgenKlient getDokgenKlient() {
        return usePdfgen ? k9PdfgenKlient : k9DokgenKlient;
    }
}

