insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select (nextval(SEQ_GLOBAL_PK), 'ny.task', nextval(SEQ_GLOBAL_PK), current_timestamp at time zone 'UTC' + floor(random() * 5400) * '1 second'::interval, 'foresporselId=' || f.id) from FORESPOERSEL f;
