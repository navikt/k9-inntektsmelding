CREATE TABLE SOKNADSPERIODE
(
    ID   BIGINT PRIMARY KEY,
    FORESPOERSEL_ID BIGINT   NOT NULL
        constraint fk_soknadsperiode
            references FORESPOERSEL,
    FOM DATE NOT NULL,
    TOM DATE NOT NULL
);

create sequence if not exists SEQ_SOKNADSPERIODE increment by 50 minvalue 1000000;

create unique index IDX_SOKNADSPERIODE_PRIMARY_KEY on SOKNADSPERIODE (ID);

comment on table SOKNADSPERIODE is 'Søknadsperioder som knyttes til forespørselen om inntektsmelding';
comment on column SOKNADSPERIODE.ID is 'PK';
comment on column SOKNADSPERIODE.FORESPOERSEL_ID is 'Forespørselen som søknadsperioden er knyttet til';
comment on column SOKNADSPERIODE.FOM is 'Søknadsperioden varer fra og med denne datoen';
comment on column SOKNADSPERIODE.TOM is 'Søknadsperioden varer til og med denne datoen';
