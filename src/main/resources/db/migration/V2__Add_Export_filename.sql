ALTER TABLE EXPORT ADD COLUMN  FILE_NAME character varying(255);
ALTER TABLE EXPORT ADD COLUMN DRY_RUN boolean default 'f' NOT NULL;