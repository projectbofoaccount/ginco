--Add role to hierarchical relationships
ALTER TABLE hierarchical_relationship ADD COLUMN role integer NOT NULL DEFAULT 0;

--Add parent to a group concept
ALTER TABLE concept_group ADD COLUMN parentgroupid text;

ALTER TABLE concept_group
  ADD CONSTRAINT fk_concept_group FOREIGN KEY (parentgroupid)
      REFERENCES concept_group (identifier) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;
      

ALTER TABLE thesaurus_array_concept ADD COLUMN arrayorder integer NOT NULL DEFAULT 0;

CREATE TABLE split_nonpreferredterm
(
  identifier text NOT NULL,
  lexicalvalue text NOT NULL,
  created timestamp without time zone NOT NULL DEFAULT now(),
  modified timestamp without time zone NOT NULL DEFAULT now(),
  source text,
  status integer,
  thesaurusid text NOT NULL,
  lang character(5) NOT NULL,
  CONSTRAINT pk_snpt_identifier PRIMARY KEY (identifier),
  CONSTRAINT fk_snpt_languages_iso639 FOREIGN KEY (lang)
      REFERENCES languages_iso639 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_snpt_thesaurus FOREIGN KEY (thesaurusid)
      REFERENCES thesaurus (identifier) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX idx_split_nonpreferredterm_thesaurusid
  ON split_nonpreferredterm
  USING btree
  (thesaurusid COLLATE pg_catalog."default");

CREATE TABLE compound_equivalence
(
  id_split_nonpreferredterm text NOT NULL,
  id_preferredterm text NOT NULL,
  CONSTRAINT compound_equivalence_pk PRIMARY KEY (id_split_nonpreferredterm, id_preferredterm),
  CONSTRAINT fk_preferredterm FOREIGN KEY (id_preferredterm)
      REFERENCES thesaurus_term (identifier) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_split_nonpreferredterm FOREIGN KEY (id_split_nonpreferredterm)
      REFERENCES split_nonpreferredterm (identifier) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

ALTER TABLE concept_group DROP CONSTRAINT fk_concept_group;

ALTER TABLE concept_group
  ADD CONSTRAINT fk_concept_group FOREIGN KEY (parentgroupid)
      REFERENCES concept_group (identifier) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE SET NULL;