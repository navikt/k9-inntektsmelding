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
        List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværHeleDagerRequestDto> fraværsPerioder = new ArrayList<>();
        fraværsPerioder.add(new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværHeleDagerRequestDto(LocalDate.now(), LocalDate.now().plusDays(1)));

        SendInntektsmeldingRequestDto.OmsorgspengerRequestDto dto = new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(true, fraværsPerioder, new ArrayList<>());

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testValidDelvisFraværsPerioder() {
        List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto> delvisFraværsPerioder = new ArrayList<>();
        delvisFraværsPerioder.add(new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto(LocalDate.now(), new BigDecimal("2.5")));

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

    @Test
    public void ingenOverlappMellomFraværHeleDager() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(1), LocalDate.now()));
        var dto = lagOmsorgspengerRequestDto(true, fraværHeleDager, null);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }

    @Test
    public void overlappendeFraværHeleDagerSkalFeile() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(2), LocalDate.now()));
        var dto = lagOmsorgspengerRequestDto(true, fraværHeleDager, null);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void ingenOverlappMellomDelvisFraværsdagerOgFraværHeleDager() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(3)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(1), LocalDate.now()));

        var fraværDelerAvDagen = List.of(lagFraværDelerAvDagen(LocalDate.now().plusWeeks(1), new BigDecimal("2.5")));
        var dto = lagOmsorgspengerRequestDto(true, fraværHeleDager, fraværDelerAvDagen);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }

    @Test
    public void sammeDagKanIkkeFinnesIDelvisFraværsdagerOgFraværHeleDager() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(1), LocalDate.now()));

        var fraværDelerAvDagen = List.of(
            lagFraværDelerAvDagen(LocalDate.now().minusWeeks(2), new BigDecimal("2.5")),
            lagFraværDelerAvDagen(LocalDate.now(), new BigDecimal("2.5")));

        var dto = lagOmsorgspengerRequestDto(true, fraværHeleDager, fraværDelerAvDagen);

        Set<ConstraintViolation<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    private SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværHeleDagerRequestDto lagFraværHeleDager(LocalDate fom, LocalDate tom) {
        return new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværHeleDagerRequestDto(fom, tom);
    }

    private SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto lagFraværDelerAvDagen(LocalDate dato, BigDecimal timer) {
        return new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto(dato, timer);
    }

    private SendInntektsmeldingRequestDto.OmsorgspengerRequestDto lagOmsorgspengerRequestDto(Boolean harUtbetaltPliktigeDager,
                                                                                                  List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværHeleDagerRequestDto> fraværHeleDager,
                                                                                                  List<SendInntektsmeldingRequestDto.OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto> fraværDelerAvDagen) {
        return new SendInntektsmeldingRequestDto.OmsorgspengerRequestDto(harUtbetaltPliktigeDager, fraværHeleDager, fraværDelerAvDagen);
    }
}

