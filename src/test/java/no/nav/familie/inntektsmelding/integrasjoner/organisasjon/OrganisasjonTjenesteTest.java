package no.nav.familie.inntektsmelding.integrasjoner.organisasjon;

import static no.nav.familie.inntektsmelding.felles.TestOrganisasjon.TEST_ORGANISASJON_NAVN;
import static no.nav.familie.inntektsmelding.felles.TestOrganisasjon.TEST_ORGANISASJON_NUMMER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganisasjonTjenesteTest {
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private EregKlient eregRestKlient;

    @BeforeEach
    void setUp() {
        organisasjonTjeneste = new OrganisasjonTjeneste(eregRestKlient);
    }

    @Test
    public void respons_blir_mappet() {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(TEST_ORGANISASJON_NUMMER).orElseThrow();

        assertThat(organisasjon.navn()).isEqualTo(TEST_ORGANISASJON_NAVN);
        assertThat(organisasjon.orgnr()).isEqualTo(TEST_ORGANISASJON_NUMMER);
    }
}
