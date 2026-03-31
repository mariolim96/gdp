-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

-- GDP Decodifica / Initial Data/ if not exists
INSERT INTO GDP_TEMA (COD_TEMA, TEMA) VALUES (1,'Cronaca Locale'), (2,'Pubblica Amministrazione'), (3,'Lavoro'), (4,'Cinema'), (5,'Cultura') ON CONFLICT DO NOTHING;

INSERT INTO GDP_PROVINCE (SIGLA, PROVINCIA) VALUES ('AL','Alessandria'), ('AT','Asti'), ('BI','Biella'), ('CN','Cuneo'), ('NO','Novara'), ('TO','Torino'), ('VB','Verbano Cusio Ossola'), ('VC','Vercelli') ON CONFLICT DO NOTHING;

INSERT INTO GDP_TIPO_EDIZIONE (COD_TIPO, TIPO_EDIZIONE) VALUES ('OK', 'corrispondente'), ('SO', 'sospesa'), ('AN', 'anticipataria'), ('PO', 'posticipataria'), ('AA', 'anomalia edizione attesa'), ('ST', 'edizione storica'), ('AS', 'edizione storica con anomalia') ON CONFLICT DO NOTHING;

INSERT INTO GDP_TIPO_FILE (COD_TIPO, TIPO_FILE) VALUES ('NP', 'PDF multi-pagina'), ('NL', 'file non leggibile'), ('NF', 'file formato errato'), ('DA', 'data attesa'), ('PP', 'prima pagina') ON CONFLICT DO NOTHING;