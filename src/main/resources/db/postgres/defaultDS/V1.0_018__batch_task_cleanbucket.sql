insert into prosess_task (id, task_type, status, task_gruppe)
select nextval('SEQ_PROSESS_TASK'), 'partition.cleanBucket', 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE')
where not exists (select 1 from prosess_task where task_type = 'partition.cleanBucket' and status IN ('KLAR', 'FEILET'));
