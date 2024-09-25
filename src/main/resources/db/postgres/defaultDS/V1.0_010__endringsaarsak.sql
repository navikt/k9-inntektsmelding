CREATE TABLE ENDRINGSAARSAK
(
    ID   BIGINT PRIMARY KEY,
    INNTEKTSMELDING_ID BIGINT   NOT NULL
        constraint fk_endringsaarsak
            references INNTEKTSMELDING,
    AARSAK VARCHAR(100) NOT NULL,
    FOM DATE,
    TOM DATE,
    BLE_KJENT_FOM DATE
);

create sequence if not exists SEQ_ENDRINGSAARSAK increment by 50 minvalue 1000000;

create unique index IDX_ENDRINGSAARSAK_PRIMARY_KEY on ENDRINGSAARSAK (ID);

comment on table ENDRINGSAARSAK is 'Endringsårsaker som begrunner hvorfor snittlønn de siste tre måneder ikke kan brukes i inntektsmeldingen';
comment on column ENDRINGSAARSAK.ID is 'PK';
comment on column ENDRINGSAARSAK.INNTEKTSMELDING_ID is 'Inntektsmeldingen endringsårsaken er oppgitt i';
comment on column ENDRINGSAARSAK.AARSAK is 'Endringsårsaken som er oppgitt';
comment on column ENDRINGSAARSAK.FOM is 'Endringsårsaken gjelder fra og med denne datoen';
comment on column ENDRINGSAARSAK.TOM is 'Endringsårsaken gjelder til og med denne datoen';
comment on column ENDRINGSAARSAK.BLE_KJENT_FOM is 'Endringsårsaken ble kjent denne datoen';
