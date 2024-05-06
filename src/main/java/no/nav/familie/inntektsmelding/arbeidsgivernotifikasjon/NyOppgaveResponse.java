package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgaveResultat;

import java.util.Map;

class NyOppgaveResponse extends GraphQLResult<Map<String, NyOppgave>> {

    private static final String OPERATION_NAME = "nyOppgave";

    public NyOppgaveResponse() {
    }

    /**
     * Opprett en ny oppgave.
     */
    public NyOppgaveResultat nyOppgave() {
        Map<String, NyOppgave> data = getData();
        return data != null ? data.get(OPERATION_NAME) : null;
    }
}
