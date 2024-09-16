alter table IF EXISTS INNTEKTSMELDING
ADD COLUMN OPPRETTET_AV VARCHAR(100);

alter table IF EXISTS INNTEKTSMELDING
    ADD COLUMN KILDESYSTEM VARCHAR(100);

comment on column INNTEKTSMELDING.OPPRETTET_AV is 'Referanse til hvem som opprettet inntektsmeldingen';
comment on column INNTEKTSMELDING.KILDESYSTEM is 'Systemet som stod for innsending av inntektsmeldingen';
