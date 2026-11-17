CREATE SEQUENCE IF NOT EXISTS file_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE file
(
    id           BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size         BIGINT       NOT NULL,
    status       VARCHAR(255) NOT NULL,
    s3_key       VARCHAR(255) NOT NULL,
    s3_bucket    VARCHAR(255) NOT NULL,
    e_tag        VARCHAR(256),
    upload_id    VARCHAR(256),
    CONSTRAINT pk_file PRIMARY KEY (id)
);

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

ALTER TABLE file
    ADD CONSTRAINT uc_dad2025681a1c77d0e5ebf914 UNIQUE (s3_key, s3_bucket);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);