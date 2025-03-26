alter table IF EXISTS INNTEKTSMELDING ADD COLUMN FORESPORSEL_UUID UUID;

CREATE INDEX IF NOT EXISTS IDX_INNTEKTSMELDING_FORESPORSEL_UUID ON INNTEKTSMELDING(FORESPORSEL_UUID);

comment on column INNTEKTSMELDING.FORESPORSEL_UUID is 'Referanse til forespørsel som inntektsmeldingen er knyttet til';
