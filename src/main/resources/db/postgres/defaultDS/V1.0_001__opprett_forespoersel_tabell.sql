CREATE TABLE FORESPOERSEL
(
    ID   BIGINT PRIMARY KEY,
    UUID UUID NOT NULL,
    SAK_ID VARCHAR(36),
    OPPGAVE_ID VARCHAR(36),
    SKJAERINGSTIDSPUNKT DATE,
    ORGNR VARCHAR(12),
    BRUKER_AKTOER_ID VARCHAR(9),
    YTELSE_TYPE VARCHAR(100),
    FAGSYSTEM_SAKSNUMMER VARCHAR(19),
    OPPRETTET_AV VARCHAR(20) DEFAULT 'FT-INNTEKTSMELDING' NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV VARCHAR(20),
    ENDRET_TID TIMESTAMP(3)
);

create sequence if not exists SEQ_FORESPOERSEL increment by 50 minvalue 1000000;

create unique index IDX_FORESPOERSEL_PRIMARY_KEY on FORESPOERSEL (ID);
create unique index IDX_FORESPOERSEL_UUID on FORESPOERSEL (UUID);
create index IDX_FORESPOERSEL_ORGNR_BRUKER_YTELSE_SAKSNR on FORESPOERSEL (SKJAERINGSTIDSPUNKT, ORGNR, BRUKER_AKTOER_ID, YTELSE_TYPE);

comment on table FORESPOERSEL is 'En forespørsel til arbeidsgiver';
comment on column FORESPOERSEL.ID is 'PK';
comment on column FORESPOERSEL.UUID is 'Forespørsel UUID som kan eksponeres';
comment on column FORESPOERSEL.SAK_ID is 'Ekstern sak-id hos arbeidsgivernotifikasjon';
comment on column FORESPOERSEL.OPPGAVE_ID is 'Ekstern oppgave-id hos arbeidsgivernotifikasjon';
comment on column FORESPOERSEL.SKJAERINGSTIDSPUNKT is 'Skjæringstidspunkt forespørselen gjelder for';
comment on column FORESPOERSEL.ORGNR is 'Orgnr for arbeidsgiver';
comment on column FORESPOERSEL.BRUKER_AKTOER_ID is 'Aktørid for bruker';
comment on column FORESPOERSEL.YTELSE_TYPE is 'Ytelsetype som forespørsel gjelder for';
comment on column FORESPOERSEL.FAGSYSTEM_SAKSNUMMER is 'Saksnummer fra fagsystem';


