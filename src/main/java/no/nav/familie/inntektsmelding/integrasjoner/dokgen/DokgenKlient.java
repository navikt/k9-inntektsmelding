package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

public interface DokgenKlient {

    byte[] genererPdfInntektsmelding(InntektsmeldingPdfRequest pdfRequest) throws Exception;

    byte[] genererPdfRefusjonskravNyansatt(RefusjonskravNyansattData pdfRequest) throws Exception;

    byte[] genererPdfOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfRequest pdfRequest) throws Exception;

    byte[] genererPdfOmsorgspengerInntektsmelding(OmsorgspengerInntektsmeldingPdfRequest pdfRequest) throws Exception;
}

