CREATE TABLE BORTFALT_NATURALYTELSE
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint FK_BORTFALT_NATURALYTELSE
            references INNTEKTSMELDING,
    FOM DATE NOT NULL,
    TOM DATE NOT NULL,
    MAANED_BELOEP NUMERIC(19, 2) NOT NULL,
    TYPE VARCHAR(100) NOT NULL
);

create sequence if not exists SEQ_BORTFALT_NATURALYTELSE increment by 50 minvalue 1000000;

create unique index IDX_BORTFALT_NATURALYTELSE_PRIMARY_KEY on BORTFALT_NATURALYTELSE (ID);

comment on table BORTFALT_NATURALYTELSE is 'En naturalytelse som bortfaller for en periode';
comment on column BORTFALT_NATURALYTELSE.ID is 'PK';
comment on column BORTFALT_NATURALYTELSE.INNTEKTSMELDING_ID is 'Foreign Key til inntektsmelding';
comment on column BORTFALT_NATURALYTELSE.FOM is 'Fra og med dato for når naturalytelse bortfaller';
comment on column BORTFALT_NATURALYTELSE.TOM is 'Til og med dato for når naturalytelse bortfaller';
comment on column BORTFALT_NATURALYTELSE.MAANED_BELOEP is 'Det månedlige beløpet som skal kompenseres';
comment on column BORTFALT_NATURALYTELSE.TYPE is 'Type naturalytelse som er bortfalt';


