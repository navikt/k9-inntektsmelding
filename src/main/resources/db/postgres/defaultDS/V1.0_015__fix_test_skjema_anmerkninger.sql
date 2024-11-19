
COMMENT ON COLUMN forespoersel.FORSTE_UTTAKSDATO IS 'Dat med første frævarsdag.';
COMMENT ON COLUMN forespoersel.STATUS IS 'Status på forespørselen.';

ALTER TABLE forespoersel RENAME CONSTRAINT "forespoersel_pkey" TO "pk_foresporsel_id";
ALTER TABLE refusjon_endring RENAME CONSTRAINT "refusjon_endring_pkey" TO "pk_refusjon_endring_id";
ALTER TABLE bortfalt_naturalytelse RENAME CONSTRAINT "bortfalt_naturalytelse_pkey" TO "pk_bortfalt_naturalytelse_id";
ALTER TABLE endringsaarsak RENAME CONSTRAINT "endringsaarsak_pkey" TO "pk_endringsaarsak_id";
ALTER TABLE kontaktperson RENAME CONSTRAINT "kontaktperson_pkey" TO "pk_kontaktperson_id";
ALTER TABLE inntektsmelding RENAME CONSTRAINT "inntektsmelding_pkey" TO "pk_inntektsmelding_id";

DROP INDEX idx_bortfalt_naturalytelse_primary_key;
DROP INDEX idx_endringsaarsak_primary_key;
DROP INDEX idx_forespoersel_primary_key;
DROP INDEX idx_inntektsmelding_primary_key;
DROP INDEX idx_refusjon_endring_primary_key;
DROP INDEX idx_kontaktperson_primary_key;

CREATE INDEX idx_kontaktperson_inntektsmelding_id ON kontaktperson(inntektsmelding_id);
CREATE INDEX idx_bortfalt_naturalytelse_inntektsmelding_id ON bortfalt_naturalytelse(inntektsmelding_id);
CREATE INDEX idx_endringsaarsak_inntektsmelding_id ON endringsaarsak(inntektsmelding_id);
CREATE INDEX idx_refusjon_endring_inntektsmelding_id ON refusjon_endring(inntektsmelding_id);

ALTER INDEX idx_forespoersel_uuid RENAME TO "uidx_forespoersel_uuid";

