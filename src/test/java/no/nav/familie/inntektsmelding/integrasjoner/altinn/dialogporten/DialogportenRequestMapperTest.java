package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class DialogportenRequestMapperTest {

    @Test
    void inntektsmeldingPurringMelding() {
        var purringTekst = "Vi har ennå ikke mottatt inntektsmelding for Motivert Hare.";
        var purringPatchRequest = DialogportenRequestMapper.inntektsmeldingPurringMelding(purringTekst);

        assertThat(purringPatchRequest.op()).isEqualTo(DialogportenPatchRequest.OP_ADD);
        assertThat(purringPatchRequest.path()).isEqualTo(DialogportenPatchRequest.PATH_TRANSMISSIONS);

        assertThat(purringPatchRequest.value())
            .asInstanceOf(InstanceOfAssertFactories.list(DialogportenRequest.Transmission.class))
            .singleElement()
            .satisfies(transmission -> {
                var content = transmission.content();
                assertThat(content.summary().value().getFirst().value()).isEqualTo(purringTekst);
                assertThat(content.title().value().getFirst().value()).isNotEqualTo(purringTekst);
            });
    }
}
