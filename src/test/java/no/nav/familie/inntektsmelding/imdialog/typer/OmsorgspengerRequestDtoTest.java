package no.nav.familie.inntektsmelding.imdialog.typer;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import no.nav.familie.inntektsmelding.imdialog.rest.OmsorgspengerRequestDto;

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
        List<OmsorgspengerRequestDto.FraværHeleDagerRequestDto> fraværsPerioder = new ArrayList<>();
        fraværsPerioder.add(new OmsorgspengerRequestDto.FraværHeleDagerRequestDto(LocalDate.now(), LocalDate.now().plusDays(1)));

        OmsorgspengerRequestDto dto = new OmsorgspengerRequestDto(true, fraværsPerioder, new ArrayList<>());

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testValidDelvisFraværsPerioder() {
        List<OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto> delvisFraværsPerioder = new ArrayList<>();
        delvisFraværsPerioder.add(new OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto(LocalDate.now(), new BigDecimal("2.5")));

        OmsorgspengerRequestDto dto = new OmsorgspengerRequestDto(true, new ArrayList<>(), delvisFraværsPerioder);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioder() {
        OmsorgspengerRequestDto dto = new OmsorgspengerRequestDto(true, new ArrayList<>(), new ArrayList<>());

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioderSomErNull() {
        OmsorgspengerRequestDto dto = new OmsorgspengerRequestDto(true, null, null);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void testInvalidFraværsPerioderAndDelvisFraværsPerioderSomErNullOgTomListe() {
        OmsorgspengerRequestDto dto = new OmsorgspengerRequestDto(true, null, new ArrayList<>());

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void fraværHeleDagerMåVæreKorrekt() {
        var fraværHeleDager = List.of(lagFraværHeleDager(LocalDate.now(), LocalDate.now().minusWeeks(1)));
        var dto = new OmsorgspengerRequestDto(true, fraværHeleDager, null);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());

    }

    @Test
    public void ingenOverlappMellomFraværHeleDager() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(1), LocalDate.now()));
        var dto = new OmsorgspengerRequestDto(true, fraværHeleDager, null);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }

    @Test
    public void overlappendeFraværHeleDagerSkalFeile() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(2)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(2), LocalDate.now()));
        var dto = new OmsorgspengerRequestDto(true, fraværHeleDager, null);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void dupliserteFraværDelerAvDagenSkalFeile() {
        var fraværDelerAvDagen = List.of(
            lagFraværDelerAvDagen(LocalDate.now(), new BigDecimal("2.5")),
            lagFraværDelerAvDagen(LocalDate.now().minusDays(2), new BigDecimal("2.5")),
            lagFraværDelerAvDagen(LocalDate.now(), new BigDecimal("2.5")));
        var dto = new OmsorgspengerRequestDto(true, null, fraværDelerAvDagen);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    public void ingenOverlappMellomDelvisFraværsdagerOgFraværHeleDager() {
        var fraværHeleDager = List.of(
            lagFraværHeleDager(LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(3)),
            lagFraværHeleDager(LocalDate.now().minusWeeks(1), LocalDate.now()));

        var fraværDelerAvDagen = List.of(lagFraværDelerAvDagen(LocalDate.now().plusWeeks(1), new BigDecimal("2.5")));
        var dto = new OmsorgspengerRequestDto(true, fraværHeleDager, fraværDelerAvDagen);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
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

        var dto = new OmsorgspengerRequestDto(true, fraværHeleDager, fraværDelerAvDagen);

        Set<ConstraintViolation<OmsorgspengerRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    private OmsorgspengerRequestDto.FraværHeleDagerRequestDto lagFraværHeleDager(LocalDate fom, LocalDate tom) {
        return new OmsorgspengerRequestDto.FraværHeleDagerRequestDto(fom, tom);
    }

    private OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto lagFraværDelerAvDagen(LocalDate dato, BigDecimal timer) {
        return new OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto(dato, timer);
    }
}

