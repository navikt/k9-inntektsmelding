INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('SEQ_PROSESS_TASK'),
       'forvaltning.populerInntektsmeldingMedForespoersel',
       nextval('SEQ_PROSESS_TASK_GRUPPE'),
       current_timestamp at time zone 'UTC' + floor(random() * 3600) * '1 second'::interval,
    'forespoerselUuid=' || f.uuid || ''
FROM FORESPOERSEL f
