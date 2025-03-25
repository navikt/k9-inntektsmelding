alter table IF EXISTS INNTEKTSMELDING ADD COLUMN FORESPORSEL_UUID UUID;

comment on column INNTEKTSMELDING.FORESPORSEL_UUID is 'Referanse til forespørsel som inntektsmeldingen er knyttet til';
