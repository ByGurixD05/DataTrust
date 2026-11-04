-- 1. La versión vigente debe pertenecer al propio dataset.
--    La FK anterior solo comprobaba que la versión existía, no de qué dataset es.
ALTER TABLE dataset_versions
    ADD CONSTRAINT uq_dataset_versions_dataset_id_id UNIQUE (dataset_id, id);

ALTER TABLE datasets
    DROP CONSTRAINT fk_datasets_current_version;

ALTER TABLE datasets
    ADD CONSTRAINT fk_datasets_current_version
    FOREIGN KEY (id, current_version_id) REFERENCES dataset_versions (dataset_id, id)
    DEFERRABLE INITIALLY DEFERRED;

-- 2. Inmutabilidad de las versiones también en la base de datos.
--    Se bloquean UPDATE y DELETE. El borrado en cascada al eliminar el dataset padre sí se permite:
--    en ese momento la fila de datasets ya no existe.
CREATE FUNCTION dataset_versions_solo_lectura() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM datasets WHERE id = OLD.dataset_id) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'dataset_versions es inmutable (% sobre la versión %)', TG_OP, OLD.id
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_dataset_versions_solo_lectura
    BEFORE UPDATE OR DELETE ON dataset_versions
    FOR EACH ROW EXECUTE FUNCTION dataset_versions_solo_lectura();

-- 3. El número de versión tiene que ser x.y.z con enteros sin ceros a la izquierda.
ALTER TABLE dataset_versions
    ADD CONSTRAINT ck_dataset_versions_number_format
    CHECK (version_number ~ '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$');

-- 4. Fecha de creación del dataset (la tabla no la tenía).
--    Se rellena con la fecha de su primera versión.
ALTER TABLE datasets
    ADD COLUMN created_at TIMESTAMPTZ;

UPDATE datasets d
   SET created_at = COALESCE(
           (SELECT min(v.created_at) FROM dataset_versions v WHERE v.dataset_id = d.id),
           now());

ALTER TABLE datasets
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX ix_datasets_created_at ON datasets (created_at, id);