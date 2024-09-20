package no.nav.familie.inntektsmelding.imdialog.tjenester;

import no.nav.familie.inntektsmelding.imdialog.rest.SendOverstyrtInntektsmeldingRequestDto;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;

import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;

import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InntektsmeldingOverstyringMapperTest {

    @Test
    void skal_teste_overstyring_mapping() {
        // Arrange
        var stp = LocalDate.now();
        var foventedeRefusjonsendringer = List.of(new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(10), BigDecimal.valueOf(4000)),
            new SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto(stp.plusDays(15), BigDecimal.ZERO));
        var request = new SendOverstyrtInntektsmeldingRequestDto(new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER, new ArbeidsgiverDto("999999999"), stp, BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
            foventedeRefusjonsendringer, Collections.emptyList(), "Truls Test");

        // Act
        var entitet = InntektsmeldingOverstyringMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet).isNotNull();
        assertThat(entitet.getRefusjonsendringer()).hasSize(1);
        assertThat(entitet.getRefusjonsendringer().get(0).getFom()).isEqualTo(stp.plusDays(10));
        assertThat(entitet.getRefusjonsendringer().get(0).getRefusjonPrMnd()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(stp.plusDays(15));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

}
