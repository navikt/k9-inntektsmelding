CREATE TABLE INNTEKTSMELDING
(
    ID   BIGINT PRIMARY KEY,
    START_DATO_PERMISJON DATE NOT NULL,
    ARBEIDSGIVER_IDENT VARCHAR(13) NOT NULL,
    AKTOER_ID VARCHAR(13) NOT NULL,
    YTELSE_TYPE VARCHAR(100) NOT NULL,
    MAANED_INNTEKT NUMERIC(19, 2) NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

create sequence if not exists SEQ_INNTEKTSMELDING increment by 50 minvalue 1000000;

create unique index IDX_INNTEKTSMELDING_PRIMARY_KEY on INNTEKTSMELDING (ID);
create index IDX_INNTEKTSMELDING_START_DATO_ARBEIDSGIVER_IDENT_BRUKER_YTELSE_SAKSNR on INNTEKTSMELDING (START_DATO_PERMISJON, ARBEIDSGIVER_IDENT, AKTOER_ID, YTELSE_TYPE);

comment on table INNTEKTSMELDING is 'En inntektsmelding fra en arbeidsgiver';
comment on column INNTEKTSMELDING.ID is 'PK';
comment on column INNTEKTSMELDING.START_DATO_PERMISJON is 'Startdato for ytelsen';
comment on column INNTEKTSMELDING.ARBEIDSGIVER_IDENT is 'Identifikator for arbeidsgiver, orgnr for bedrifter og aktørId for privatpersoner';
comment on column INNTEKTSMELDING.AKTOER_ID is 'Aktørid for arbeidstaker';
comment on column INNTEKTSMELDING.YTELSE_TYPE is 'Ytelsetype som inntektsmeldingen gjelder for';
comment on column INNTEKTSMELDING.MAANED_INNTEKT is 'Arbeidstakers månedsinntekt';
comment on column INNTEKTSMELDING.OPPRETTET_TID is 'Timestamp da inntektsmeldingen ble lagret i databasen';

CREATE TABLE KONTAKTPERSON
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint fk_kontaktperson
        references INNTEKTSMELDING,
    TELEFONNUMMER VARCHAR(100) NOT NULL,
    NAVN VARCHAR(100) NOT NULL
);

create sequence if not exists SEQ_KONTAKTPERSON increment by 50 minvalue 1000000;

create unique index IDX_KONTAKTPERSON_PRIMARY_KEY on KONTAKTPERSON (ID);

comment on table KONTAKTPERSON is 'En kontaktperson for inntektsmeldingen';
comment on column KONTAKTPERSON.ID is 'PK';
comment on column KONTAKTPERSON.INNTEKTSMELDING_ID is 'Foreign Key til inntektsmelding';
comment on column KONTAKTPERSON.NAVN is 'Navn på kontaktperson';
comment on column KONTAKTPERSON.TELEFONNUMMER is 'Telefonnummer til kontakperson';

CREATE TABLE REFUSJON_PERIODE
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint FK_REFUSJON_PERIODE
            references INNTEKTSMELDING,
    FOM DATE NOT NULL,
    TOM DATE NOT NULL,
    BELOEP NUMERIC(19, 2) NOT NULL
);

create sequence if not exists SEQ_REFUSJON_PERIODE increment by 50 minvalue 1000000;

create unique index IDX_REFUSJON_PERIODE_PRIMARY_KEY on REFUSJON_PERIODE (ID);

comment on table REFUSJON_PERIODE is 'En refusjonsperiode oppgitt i inntektsmeldingen';
comment on column REFUSJON_PERIODE.ID is 'PK';
comment on column REFUSJON_PERIODE.INNTEKTSMELDING_ID is 'Foreign Key til inntektsmelding';
comment on column REFUSJON_PERIODE.FOM is 'Fra og med dato for refusjonsperiode';
comment on column REFUSJON_PERIODE.TOM is 'Til og med dato for refusjonsperiode';
comment on column REFUSJON_PERIODE.BELOEP is 'Refusjonsbeløp';

CREATE TABLE NATURALYTELSE
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint FK_NATURALYTELSE
            references INNTEKTSMELDING,
    FOM DATE NOT NULL,
    TOM DATE NOT NULL,
    BELOEP NUMERIC(19, 2) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    ER_BORTFALT BOOLEAN NOT NULL
);

create sequence if not exists SEQ_NATURALYTELSE increment by 50 minvalue 1000000;

create unique index IDX_NATURALYTELSE_PRIMARY_KEY on NATURALYTELSE (ID);

comment on table NATURALYTELSE is 'En endring i naturalytelse for en periode';
comment on column NATURALYTELSE.ID is 'PK';
comment on column NATURALYTELSE.INNTEKTSMELDING_ID is 'Foreign Key til inntektsmelding';
comment on column NATURALYTELSE.FOM is 'Fra og med dato for endring i naturalytelse';
comment on column NATURALYTELSE.TOM is 'Til og med dato for endring i naturalytelse';
comment on column NATURALYTELSE.BELOEP is 'Beløpet som er endret';
comment on column NATURALYTELSE.TYPE is 'Typen naturalytelse som er endret';
comment on column NATURALYTELSE.ER_BORTFALT is 'Om naturalytelse er bortfalt eller tilkommet';
