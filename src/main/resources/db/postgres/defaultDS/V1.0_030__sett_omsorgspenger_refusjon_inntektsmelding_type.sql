-- Etter opprettelsene av inntektsmelding_type ble alle rader satt til 'ORDINAER'.
-- Denne migreringen identifiserer de som skal være OMSORGSPENGER_REFUSJON og oppdaterer inntektsmelding_type deretter.
UPDATE inntektsmelding
SET inntektsmelding_type = 'OMSORGSPENGER_REFUSJON'
WHERE ytelse_type = 'OMSORGSPENGER'
  AND maaned_refusjon IS NOT NULL;
