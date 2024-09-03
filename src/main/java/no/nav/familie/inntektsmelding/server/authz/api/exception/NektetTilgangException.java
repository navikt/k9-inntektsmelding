package no.nav.familie.inntektsmelding.server.authz.api.exception;

import no.nav.vedtak.exception.ManglerTilgangException;

public class NektetTilgangException extends ManglerTilgangException {
    public NektetTilgangException(String kode, String msg) {
        super(kode, msg);
    }

    public NektetTilgangException(String kode, String msg, Throwable cause) {
        super(kode, msg, cause);
    }
}
