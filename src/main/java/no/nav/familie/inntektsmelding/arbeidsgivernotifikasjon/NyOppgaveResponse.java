package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import java.util.Map;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

class NyOppgaveResponse extends GraphQLResult<Map<String, NyOppgave>> {

    private static final String OPERATION_NAME = "nyOppgave";

    public NyOppgaveResponse() {
        // Empty
    }

    /**
     * Opprett en ny oppgave.
     */
    public NyOppgaveResultat nyOppgave() {
        var data = getData();
        return data != null ? data.get(OPERATION_NAME) : null;
    }
}
