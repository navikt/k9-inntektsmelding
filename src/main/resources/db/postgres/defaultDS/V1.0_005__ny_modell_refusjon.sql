alter table INNTEKTSMELDING add column maaned_refusjon NUMERIC(19, 2);
alter table INNTEKTSMELDING add column refusjon_opphoersdato DATE;

comment on column INNTEKTSMELDING.maaned_refusjon is 'Refusjonskrav pr måned fra arbeidsgiver';
comment on column INNTEKTSMELDING.refusjon_opphoersdato is 'Siste dag med refusjon';

CREATE TABLE REFUSJON_ENDRING
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint fk_refusjon_endring
            references INNTEKTSMELDING,
    FOM DATE NOT NULL,
    MAANED_REFUSJON NUMERIC(19, 2) NOT NULL
);

create sequence if not exists SEQ_REFUSJON_ENDRING increment by 50 minvalue 1000000;

create unique index IDX_REFUSJON_ENDRING_PRIMARY_KEY on REFUSJON_ENDRING (ID);

comment on table REFUSJON_ENDRING is 'Endringer i refusjon';
comment on column REFUSJON_ENDRING.ID is 'PK';
comment on column REFUSJON_ENDRING.INNTEKTSMELDING_ID is 'Inntektsmeldingen endringene gjelder for';
comment on column REFUSJON_ENDRING.FOM is 'Endringen gjelder fra og med denne datoen';
comment on column REFUSJON_ENDRING.MAANED_REFUSJON is 'Hva refusjonsbeløpet pr mnd endres til';
