-- Tiempo activo de la tarea: desde cuándo corre el reloj. Solo tiene valor mientras la tarea está IN_PROGRESS;
-- al salir de ese estado (espera, completada, vencida) se suma a time_passed y se limpia.
alter table tasks add column active_since timestamp(6) with time zone;

-- Las tareas que ya están en curso cuentan desde su último cambio.
update tasks set active_since = updated_at where status = 'IN_PROGRESS';
