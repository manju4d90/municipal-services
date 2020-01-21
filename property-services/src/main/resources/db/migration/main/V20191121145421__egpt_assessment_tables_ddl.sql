CREATE TABLE eg_pt_asmt_assessments (

  id character varying(256) NOT NULL,
  tenantId character varying(256) NOT NULL,
  assessmentNumber character varying(64) NOT NULL,
  financialyear character varying(256) NOT NULL,
  propertyId character varying(256) NOT NULL,
  status character varying(64) NOT NULL,
  source character varying(64),
  channel character varying(256),
  assessmentDate bigint NOT NULL,
  additionalDetails jsonb,
  createdby character varying(64) NOT NULL,
  createdtime bigint NOT NULL,
  lastmodifiedby character varying(64) NOT NULL,
  lastmodifiedtime bigint NOT NULL,

  CONSTRAINT pk_eg_pt_assessments PRIMARY KEY (id),
  CONSTRAINT uk_eg_pt_assessments UNIQUE (assessmentNumber, tenantId)

);

CREATE INDEX IF NOT EXISTS index_eg_pt_assessments_assessmentNumber ON  eg_pt_assessments (assessmentNumber);
CREATE INDEX IF NOT EXISTS index_eg_pt_assessments_propertyId ON  eg_pt_assessments (propertyId);


CREATE TABLE eg_pt_asmt_unitusage (

  tenantId character varying(256) NOT NULL,
  id character varying(256) NOT NULL,
  assessmentId character varying(256) NOT NULL,
  unitId character varying(64) NOT NULL,
  usageCategory character varying(256) NOT NULL,
  occupancyType character varying(64) NOT NULL,
  occupancyDate bigint,
  active BOOLEAN,
  createdby character varying(64) NOT NULL,
  createdtime bigint NOT NULL,
  lastmodifiedby character varying(64) NOT NULL,
  lastmodifiedtime bigint NOT NULL,

  CONSTRAINT pk_eg_pt_unitusage PRIMARY KEY (id),
  CONSTRAINT fk_eg_pt_unitusage FOREIGN KEY (assessmentId) REFERENCES eg_pt_assessments (id)
  
  ON UPDATE CASCADE
  ON DELETE CASCADE
);


CREATE TABLE eg_pt_asmt_assessments_audit (

   id character varying(256) NOT NULL,
   tenantId character varying(256) NOT NULL,
   assessmentNumber character varying(64) NOT NULL,
   financialyear character varying(256) NOT NULL,
   propertyId character varying(256) NOT NULL,
   status character varying(64) NOT NULL,
   source character varying(64),
   channel character varying(256),
   assessmentDate bigint NOT NULL,
   additionalDetails jsonb,
   createdby character varying(64) NOT NULL,
   createdtime bigint NOT NULL,
   lastmodifiedby character varying(64) NOT NULL,
   lastmodifiedtime bigint NOT NULL,
   auditcreatedtime bigint NOT NULL
);



CREATE TABLE eg_pt_asmt_unitusage_audit (

    tenantId character varying(256) NOT NULL,
    id character varying(256) NOT NULL,
    assessmentId character varying(256) NOT NULL,
    unitId character varying(64) NOT NULL,
    usageCategory character varying(256) NOT NULL,
    occupancyType character varying(64) NOT NULL,
    occupancyDate bigint,
    active BOOLEAN,
    createdby character varying(64) NOT NULL,
    createdtime bigint NOT NULL,
    lastmodifiedby character varying(64) NOT NULL,
    lastmodifiedtime bigint NOT NULL,
    auditcreatedtime bigint NOT NULL

);


CREATE TABLE eg_pt_asmt_document (

  id               CHARACTER VARYING (128) NOT NULL,
  tenantId         CHARACTER VARYING (256) NOT NULL,
  entityid         CHARACTER VARYING (128) NOT NULL,
  documentType     CHARACTER VARYING (128) NOT NULL,
  fileStoreId      CHARACTER VARYING (128) NOT NULL,
  documentuid      CHARACTER VARYING (128) NOT NULL,
  status           CHARACTER VARYING (128) NOT NULL,
  createdBy        CHARACTER VARYING (128) NOT NULL,
  lastModifiedBy   CHARACTER VARYING (128),
  createdTime      BIGINT NOT NULL,
  lastModifiedTime BIGINT  NOT NULL,

CONSTRAINT pk_eg_pt_asmt_document_id PRIMARY KEY(id)
);