-- core.chargeback definition

-- Drop table

-- DROP TABLE core.chargeback;

CREATE TABLE core.chargeback (
                                 id uuid NOT NULL,
                                 amount numeric(38, 2) NULL,
                                 created_at timestamp(6) NULL,
                                 reason_code varchar(255) NULL,
                                 status varchar(255) NOT NULL,
                                 transaction_id uuid NOT NULL,
                                 CONSTRAINT chargeback_pkey PRIMARY KEY (id),
                                 CONSTRAINT chargeback_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'UNDER_REVIEW'::character varying, 'REVERSED'::character varying, 'LOST'::character varying, 'REPRESENTMENT'::character varying])::text[])))
);


-- core.merchant definition

-- Drop table

-- DROP TABLE core.merchant;

CREATE TABLE core.merchant (
                               id uuid NOT NULL,
                               created_at timestamp(6) NULL,
                               "document" varchar(255) NOT NULL,
                               legal_name varchar(255) NULL,
                               status varchar(255) NULL,
                               CONSTRAINT merchant_pkey PRIMARY KEY (id),
                               CONSTRAINT merchant_status_check CHECK (((status)::text = ANY ((ARRAY['PROVISIONAL'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'TERMINATED'::character varying, 'AWAITING_ACTIVATION'::character varying])::text[]))),
	CONSTRAINT ukea7monyqr4u7yaasrl058nfsc UNIQUE (document)
);


-- core."transaction" definition

-- Drop table

-- DROP TABLE core."transaction";

CREATE TABLE core."transaction" (
                                    id uuid NOT NULL,
                                    amount numeric(19, 2) NULL,
                                    auth_code varchar(20) NULL,
                                    created_at timestamp(6) NULL,
                                    currency varchar(3) NULL,
                                    merchant_id uuid NULL,
                                    nsu varchar(20) NULL,
                                    product_type varchar(255) NULL,
                                    source_entry_mode varchar(20) NULL,
                                    source_ip_address varchar(45) NULL,
                                    source_software_version varchar(20) NULL,
                                    source_terminal_sn varchar(50) NULL,
                                    status varchar(255) NULL,
                                    CONSTRAINT transaction_pkey PRIMARY KEY (id),
                                    CONSTRAINT transaction_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT_A_VISTA'::character varying, 'CREDIT_PARCELADO'::character varying])::text[]))),
	CONSTRAINT transaction_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'CANCELLED'::character varying, 'REVERSED'::character varying, 'CHARGED_BACK'::character varying])::text[])))
);


-- core.transaction_event definition

-- Drop table

-- DROP TABLE core.transaction_event;

CREATE TABLE core.transaction_event (
                                        id uuid NOT NULL,
                                        description varchar(255) NULL,
                                        metadata jsonb NULL,
                                        occurred_at timestamp(6) NULL,
                                        transaction_id uuid NULL,
                                        "type" varchar(255) NULL,
                                        CONSTRAINT transaction_event_pkey PRIMARY KEY (id),
                                        CONSTRAINT transaction_event_type_check CHECK (((type)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'CANCELLED'::character varying, 'REVERSED'::character varying, 'CHARGED_BACK'::character varying])::text[])))
);


-- core.transaction_source definition

-- Drop table

-- DROP TABLE core.transaction_source;

CREATE TABLE core.transaction_source (
                                         id uuid DEFAULT gen_random_uuid() NOT NULL,
                                         "name" varchar(50) NOT NULL,
                                         created_at timestamp DEFAULT now() NULL,
                                         CONSTRAINT transaction_source_pkey PRIMARY KEY (id)
);


-- core.transaction_status_projection definition

-- Drop table

-- DROP TABLE core.transaction_status_projection;

CREATE TABLE core.transaction_status_projection (
                                                    transaction_id uuid NOT NULL,
                                                    current_status varchar(255) NULL,
                                                    is_fully_settled bool NULL,
                                                    last_update timestamp(6) NULL,
                                                    CONSTRAINT transaction_status_projection_current_status_check CHECK (((current_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'CANCELLED'::character varying, 'REVERSED'::character varying, 'CHARGED_BACK'::character varying])::text[]))),
	CONSTRAINT transaction_status_projection_pkey PRIMARY KEY (transaction_id)
);


-- core.merchant_address definition

-- Drop table

-- DROP TABLE core.merchant_address;

CREATE TABLE core.merchant_address (
                                       id uuid NOT NULL,
                                       city varchar(255) NOT NULL,
                                       complement varchar(255) NULL,
                                       is_main_address bool NULL,
                                       neighborhood varchar(255) NOT NULL,
                                       "number" varchar(10) NOT NULL,
                                       state varchar(2) NOT NULL,
                                       street varchar(255) NOT NULL,
                                       zip_code varchar(8) NOT NULL,
                                       merchant_id uuid NOT NULL,
                                       CONSTRAINT merchant_address_pkey PRIMARY KEY (id),
                                       CONSTRAINT ukk5ryy7v7njye2y3w9dv2f1qut UNIQUE (merchant_id),
                                       CONSTRAINT fkhjj4mc5v3oo3v8bkobyccc353 FOREIGN KEY (merchant_id) REFERENCES core.merchant(id)
);


-- core.terminal definition

-- Drop table

-- DROP TABLE core.terminal;

CREATE TABLE core.terminal (
                               id uuid NOT NULL,
                               created_at timestamp(6) NULL,
                               model varchar(100) NULL,
                               serial_number varchar(50) NOT NULL,
                               status varchar(255) NOT NULL,
                               merchant_id uuid NOT NULL,
                               CONSTRAINT terminal_pkey PRIMARY KEY (id),
                               CONSTRAINT terminal_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying, 'TERMINATED'::character varying, 'IN_TRANSIT'::character varying])::text[]))),
	CONSTRAINT uk4b9sk2ibyw5upn83yu8mblhbb UNIQUE (serial_number),
	CONSTRAINT fkfjidqcv1fnoe1fvxnhxvw2oxc FOREIGN KEY (merchant_id) REFERENCES core.merchant(id)
);



-- ops.clearing_batch definition

-- Drop table

-- DROP TABLE ops.clearing_batch;

CREATE TABLE ops.clearing_batch (
                                    id uuid DEFAULT gen_random_uuid() NOT NULL,
                                    brand varchar(20) NULL,
                                    file_date date NULL,
                                    file_hash varchar(128) NULL,
                                    status varchar(20) NULL,
                                    created_at timestamp DEFAULT now() NULL,
                                    CONSTRAINT clearing_batch_pkey PRIMARY KEY (id)
);


-- ops.idempotency_control definition

-- Drop table

-- DROP TABLE ops.idempotency_control;

CREATE TABLE ops.idempotency_control (
                                         idempotency_key varchar(100) NOT NULL,
                                         request_hash text NULL,
                                         response_hash text NULL,
                                         created_at timestamp DEFAULT now() NULL,
                                         CONSTRAINT idempotency_control_pkey PRIMARY KEY (idempotency_key)
);


-- ops.settlement_batch definition

-- Drop table

-- DROP TABLE ops.settlement_batch;

CREATE TABLE ops.settlement_batch (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      settlement_date date NULL,
                                      total_gross numeric(18, 2) NULL,
                                      total_net numeric(18, 2) NULL,
                                      status varchar(20) NULL,
                                      created_at timestamp DEFAULT now() NULL,
                                      CONSTRAINT settlement_batch_pkey PRIMARY KEY (id)
);


-- ops.settlement_entry definition

-- Drop table

-- DROP TABLE ops.settlement_entry;

CREATE TABLE ops.settlement_entry (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      transaction_id uuid NULL,
                                      settlement_batch_id uuid NULL,
                                      gross_amount numeric(18, 2) NULL,
                                      mdr_amount numeric(18, 2) NULL,
                                      net_amount numeric(18, 2) NULL,
                                      created_at timestamp DEFAULT now() NULL,
                                      CONSTRAINT settlement_entry_pkey PRIMARY KEY (id),
                                      CONSTRAINT settlement_entry_settlement_batch_id_fkey FOREIGN KEY (settlement_batch_id) REFERENCES ops.settlement_batch(id)
);


-- ops.merchant_bank_account definition

-- Drop table

-- DROP TABLE ops.merchant_bank_account;

CREATE TABLE ops.merchant_bank_account (
                                           id uuid NOT NULL,
                                           account varchar(20) NOT NULL,
                                           account_digit varchar(2) NULL,
                                           account_type varchar(255) NOT NULL,
                                           bank_code varchar(3) NOT NULL,
                                           branch varchar(10) NOT NULL,
                                           verified bool NOT NULL,
                                           merchant_id uuid NOT NULL,
                                           CONSTRAINT merchant_bank_account_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['CHECKING'::character varying, 'SAVINGS'::character varying, 'PAYMENT'::character varying])::text[]))),
	CONSTRAINT merchant_bank_account_pkey PRIMARY KEY (id),
	CONSTRAINT ukc6j9s96vq2a4jcu2b3adqhuh UNIQUE (merchant_id)
);


-- ops.merchant_pricing definition

-- Drop table

-- DROP TABLE ops.merchant_pricing;

CREATE TABLE ops.merchant_pricing (
                                      id uuid NOT NULL,
                                      brand varchar(255) NULL,
                                      effective_date date NULL,
                                      mdr_percentage numeric(38, 2) NULL,
                                      product_type varchar(255) NULL,
                                      merchant_id uuid NULL,
                                      CONSTRAINT merchant_pricing_pkey PRIMARY KEY (id),
                                      CONSTRAINT merchant_pricing_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT_A_VISTA'::character varying, 'CREDIT_PARCELADO'::character varying])::text[])))
);


-- ops.merchant_bank_account foreign keys

ALTER TABLE ops.merchant_bank_account ADD CONSTRAINT fksff2ang5y3ii7godqjefo2qch FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.merchant_pricing foreign keys

ALTER TABLE ops.merchant_pricing ADD CONSTRAINT fk9ay31si3sulan27xj3q31a07n FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.clearing_batch definition

-- Drop table

-- DROP TABLE ops.clearing_batch;

CREATE TABLE ops.clearing_batch (
                                    id uuid DEFAULT gen_random_uuid() NOT NULL,
                                    brand varchar(20) NULL,
                                    file_date date NULL,
                                    file_hash varchar(128) NULL,
                                    status varchar(20) NULL,
                                    created_at timestamp DEFAULT now() NULL,
                                    CONSTRAINT clearing_batch_pkey PRIMARY KEY (id)
);


-- ops.idempotency_control definition

-- Drop table

-- DROP TABLE ops.idempotency_control;

CREATE TABLE ops.idempotency_control (
                                         idempotency_key varchar(100) NOT NULL,
                                         request_hash text NULL,
                                         response_hash text NULL,
                                         created_at timestamp DEFAULT now() NULL,
                                         CONSTRAINT idempotency_control_pkey PRIMARY KEY (idempotency_key)
);


-- ops.settlement_batch definition

-- Drop table

-- DROP TABLE ops.settlement_batch;

CREATE TABLE ops.settlement_batch (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      settlement_date date NULL,
                                      total_gross numeric(18, 2) NULL,
                                      total_net numeric(18, 2) NULL,
                                      status varchar(20) NULL,
                                      created_at timestamp DEFAULT now() NULL,
                                      CONSTRAINT settlement_batch_pkey PRIMARY KEY (id)
);


-- ops.settlement_entry definition

-- Drop table

-- DROP TABLE ops.settlement_entry;

CREATE TABLE ops.settlement_entry (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      transaction_id uuid NULL,
                                      settlement_batch_id uuid NULL,
                                      gross_amount numeric(18, 2) NULL,
                                      mdr_amount numeric(18, 2) NULL,
                                      net_amount numeric(18, 2) NULL,
                                      created_at timestamp DEFAULT now() NULL,
                                      CONSTRAINT settlement_entry_pkey PRIMARY KEY (id),
                                      CONSTRAINT settlement_entry_settlement_batch_id_fkey FOREIGN KEY (settlement_batch_id) REFERENCES ops.settlement_batch(id)
);


-- ops.merchant_bank_account definition

-- Drop table

-- DROP TABLE ops.merchant_bank_account;

CREATE TABLE ops.merchant_bank_account (
                                           id uuid NOT NULL,
                                           account varchar(20) NOT NULL,
                                           account_digit varchar(2) NULL,
                                           account_type varchar(255) NOT NULL,
                                           bank_code varchar(3) NOT NULL,
                                           branch varchar(10) NOT NULL,
                                           verified bool NOT NULL,
                                           merchant_id uuid NOT NULL,
                                           CONSTRAINT merchant_bank_account_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['CHECKING'::character varying, 'SAVINGS'::character varying, 'PAYMENT'::character varying])::text[]))),
	CONSTRAINT merchant_bank_account_pkey PRIMARY KEY (id),
	CONSTRAINT ukc6j9s96vq2a4jcu2b3adqhuh UNIQUE (merchant_id)
);


-- ops.merchant_pricing definition

-- Drop table

-- DROP TABLE ops.merchant_pricing;

CREATE TABLE ops.merchant_pricing (
                                      id uuid NOT NULL,
                                      brand varchar(255) NULL,
                                      effective_date date NULL,
                                      mdr_percentage numeric(38, 2) NULL,
                                      product_type varchar(255) NULL,
                                      merchant_id uuid NULL,
                                      CONSTRAINT merchant_pricing_pkey PRIMARY KEY (id),
                                      CONSTRAINT merchant_pricing_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT_A_VISTA'::character varying, 'CREDIT_PARCELADO'::character varying])::text[])))
);


-- ops.merchant_bank_account foreign keys

ALTER TABLE ops.merchant_bank_account ADD CONSTRAINT fksff2ang5y3ii7godqjefo2qch FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.merchant_pricing foreign keys

ALTER TABLE ops.merchant_pricing ADD CONSTRAINT fk9ay31si3sulan27xj3q31a07n FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);

-- audit.audit_log definition

-- Drop table

-- DROP TABLE audit.audit_log;

CREATE TABLE audit.audit_log (
                                 id uuid NOT NULL,
                                 "action" varchar(255) NULL,
                                 details text NULL,
                                 ip_address varchar(255) NULL,
                                 resource varchar(255) NULL,
                                 "timestamp" timestamp(6) NULL,
                                 user_email varchar(255) NULL,
                                 CONSTRAINT audit_log_pkey PRIMARY KEY (id)
);


-- accounting.journal definition

-- Drop table

-- DROP TABLE accounting.journal;

CREATE TABLE accounting.journal (
                                    id uuid NOT NULL,
                                    created_at timestamp(6) NOT NULL,
                                    description varchar(255) NOT NULL,
                                    reference_id uuid NOT NULL,
                                    reference_type varchar(255) NOT NULL,
                                    CONSTRAINT journal_pkey PRIMARY KEY (id)
);


-- accounting.ledger_account definition

-- Drop table

-- DROP TABLE accounting.ledger_account;

CREATE TABLE accounting.ledger_account (
                                           id uuid NOT NULL,
                                           account_code varchar(255) NOT NULL,
                                           account_id uuid NOT NULL,
                                           active bool DEFAULT true NOT NULL,
                                           balance numeric(19, 2) NOT NULL,
                                           last_update timestamp(6) NULL,
                                           merchant_id uuid NOT NULL,
                                           "version" int8 NULL,
                                           CONSTRAINT ledger_account_pkey PRIMARY KEY (id),
                                           CONSTRAINT uk4iesi8r5qjtidfox3ufe90eol UNIQUE (account_id),
                                           CONSTRAINT uk8rtucpo4987nextswp2uf57x2 UNIQUE (merchant_id),
                                           CONSTRAINT ukh06d528xa0tv9xt30lv6k3eyd UNIQUE (account_code)
);


-- accounting.ledger_entry definition

-- Drop table

-- DROP TABLE accounting.ledger_entry;

CREATE TABLE accounting.ledger_entry (
                                         id uuid NOT NULL,
                                         amount numeric(38, 2) NULL,
                                         correlation_id uuid NULL,
                                         created_at timestamp(6) NULL,
                                         description varchar(255) NULL,
                                         "type" varchar(255) NULL,
                                         account_id uuid NULL,
                                         journal_id uuid NOT NULL,
                                         CONSTRAINT ledger_entry_pkey PRIMARY KEY (id),
                                         CONSTRAINT ledger_entry_type_check CHECK (((type)::text = ANY ((ARRAY['CREDIT'::character varying, 'DEBIT'::character varying])::text[]))),
	CONSTRAINT fk832rslhhimdytciwj5fmmbnhh FOREIGN KEY (account_id) REFERENCES accounting.ledger_account(id),
	CONSTRAINT fkc0ur4kcuw3drackpqr7haq7iy FOREIGN KEY (journal_id) REFERENCES accounting.journal(id),
	CONSTRAINT fkyswiwt3792cax3vbvn9n280w FOREIGN KEY () REFERENCES <?>()
);