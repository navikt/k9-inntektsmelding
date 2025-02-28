package no.nav.familie.inntektsmelding.imdialog.typer;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OmsorgspengerRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidFraværsPerioder() {
        List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværsPeriodeRequestDto> fraværsPerioder = new ArrayList<>();
        fraværsPerioder.add(new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværsPeriodeRequestDto(LocalDate.now(), LocalDate.now().plusDays(1)));

        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, fraværsPerioder, new ArrayList<>());

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testValidDelvisFraværsPerioder() {
        List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.DelvisFraværsPeriodeRequestDto> delvisFraværsPerioder = new ArrayList<>();
        delvisFraværsPerioder.add(new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.DelvisFraværsPeriodeRequestDto(LocalDate.now(), new BigDecimal("2.5")));

        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, new ArrayList<>(), delvisFraværsPerioder);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioder() {
        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, new ArrayList<>(), new ArrayList<>());

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioderSomErNull() {
        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, null, null);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioderSomErNullOgTomListe() {
        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, null, new ArrayList<>());

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }
}

