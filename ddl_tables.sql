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


-- accounting.payout definition

-- Drop table

-- DROP TABLE accounting.payout;

CREATE TABLE accounting.payout (
                                   id uuid NOT NULL,
                                   amount numeric(38, 2) NOT NULL,
                                   completed_at timestamp(6) NULL,
                                   created_at timestamp(6) NOT NULL,
                                   merchant_id uuid NOT NULL,
                                   pix_key varchar(255) NOT NULL,
                                   status varchar(255) NOT NULL,
                                   updated_at timestamp(6) NULL,
                                   external_reference varchar(255) NULL,
                                   CONSTRAINT payout_pkey PRIMARY KEY (id),
                                   CONSTRAINT payout_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'WAITING_BANK_CONFIRMATION'::character varying, 'COMPLETED'::character varying])::text[])))
);


-- accounting.ledger_account definition

-- Drop table

-- DROP TABLE accounting.ledger_account;

CREATE TABLE accounting.ledger_account (
                                           id uuid NOT NULL,
                                           account_code varchar(255) NOT NULL,
                                           active bool DEFAULT true NOT NULL,
                                           balance numeric(19, 2) NOT NULL,
                                           last_update timestamp(6) NULL,
                                           account_id uuid NOT NULL,
                                           merchant_id uuid NOT NULL,
                                           "version" int8 DEFAULT 0 NOT NULL,
                                           created_at timestamp(6) NULL,
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
                                         "version" int8 NULL,
                                         journal_id uuid NOT NULL,
                                         account_id uuid NULL,
                                         available_at timestamp NOT NULL,
                                         CONSTRAINT ledger_entry_pkey PRIMARY KEY (id)
);


-- accounting.ledger_account foreign keys

ALTER TABLE accounting.ledger_account ADD CONSTRAINT fkivsn8kgufn6mr39b3tkq93bj0 FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- accounting.ledger_entry foreign keys

ALTER TABLE accounting.ledger_entry ADD CONSTRAINT fk832rslhhimdytciwj5fmmbnhh FOREIGN KEY (account_id) REFERENCES accounting.ledger_account(id);
ALTER TABLE accounting.ledger_entry ADD CONSTRAINT fkc0ur4kcuw3drackpqr7haq7iy FOREIGN KEY (journal_id) REFERENCES accounting.journal(id);




-- analytics.daily_kpi_summary definition

-- Drop table

-- DROP TABLE analytics.daily_kpi_summary;

CREATE TABLE analytics.daily_kpi_summary (
                                             summary_date date NOT NULL,
                                             total_volume numeric(38, 2) DEFAULT 0.00 NOT NULL,
                                             total_transaction_count int4 DEFAULT 0 NOT NULL,
                                             approved_transaction_count int4 DEFAULT 0 NOT NULL,
                                             total_mdr_fee numeric(38, 2) DEFAULT 0.00 NOT NULL,
                                             chargeback_count int4 DEFAULT 0 NOT NULL,
                                             chargeback_volume numeric(38, 2) DEFAULT 0.00 NOT NULL,
                                             active_merchants_count int4 DEFAULT 0 NOT NULL,
                                             payment_methods_volume jsonb NULL,
                                             top_merchants_by_volume jsonb NULL,
                                             updated_at timestamptz DEFAULT now() NULL,
                                             CONSTRAINT daily_kpi_summary_pkey PRIMARY KEY (summary_date)
);
CREATE INDEX idx_daily_kpi_summary_date ON analytics.daily_kpi_summary USING btree (summary_date);







-- audit.audit_log definition

-- Drop table

-- DROP TABLE audit.audit_log;

CREATE TABLE audit.audit_log (
                                 "timestamp" timestamp(6) NULL,
                                 id uuid NOT NULL,
                                 "action" varchar(255) NULL,
                                 details text NULL,
                                 ip_address varchar(255) NULL,
                                 resource varchar(255) NULL,
                                 user_email varchar(255) NULL,
                                 CONSTRAINT audit_log_pkey PRIMARY KEY (id)
);




-- core.auth_user definition

-- Drop table

-- DROP TABLE core.auth_user;

CREATE TABLE core.auth_user (
                                id uuid NOT NULL,
                                created_at timestamp(6) NOT NULL,
                                email varchar(255) NOT NULL,
                                enabled bool NOT NULL,
                                merchant_id uuid NULL,
                                password_hash varchar(255) NOT NULL,
                                "role" varchar(255) NOT NULL,
                                updated_at timestamp(6) NULL,
                                CONSTRAINT auth_user_pkey PRIMARY KEY (id),
                                CONSTRAINT auth_user_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_MERCHANT'::character varying, 'ROLE_ADMIN'::character varying])::text[]))),
	CONSTRAINT ukklvc3dss72qnlrjp2bai055mw UNIQUE (email)
);


-- core.backoffice_users definition

-- Drop table

-- DROP TABLE core.backoffice_users;

CREATE TABLE core.backoffice_users (
                                       id uuid NOT NULL,
                                       email varchar(255) NOT NULL,
                                       "password" varchar(255) NOT NULL,
                                       "role" varchar(255) NOT NULL,
                                       enabled bool NOT NULL,
                                       created_at timestamptz NOT NULL,
                                       CONSTRAINT backoffice_users_email_key UNIQUE (email),
                                       CONSTRAINT backoffice_users_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_backoffice_users_email ON core.backoffice_users USING btree (email);


-- core.batch_job_instance definition

-- Drop table

-- DROP TABLE core.batch_job_instance;

CREATE TABLE core.batch_job_instance (
                                         job_instance_id int8 NOT NULL,
                                         "version" int8 NULL,
                                         job_name varchar(100) NOT NULL,
                                         job_key varchar(32) NOT NULL,
                                         CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id),
                                         CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
);


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


-- core.outbox definition

-- Drop table

-- DROP TABLE core.outbox;

CREATE TABLE core.outbox (
                             id uuid NOT NULL,
                             aggregate_id uuid NOT NULL,
                             created_at timestamp(6) NOT NULL,
                             payload text NOT NULL,
                             processed_at timestamp(6) NULL,
                             status varchar(255) NOT NULL,
                             "type" varchar(255) NOT NULL,
                             CONSTRAINT outbox_pkey PRIMARY KEY (id),
                             CONSTRAINT outbox_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSED'::character varying, 'FAILED'::character varying])::text[])))
);


-- core.refresh_token definition

-- Drop table

-- DROP TABLE core.refresh_token;

CREATE TABLE core.refresh_token (
                                    id uuid NOT NULL,
                                    created_at timestamp(6) NOT NULL,
                                    expires_at timestamp(6) NOT NULL,
                                    revoked bool NOT NULL,
                                    "token" varchar(512) NOT NULL,
                                    user_id uuid NOT NULL,
                                    CONSTRAINT refresh_token_pkey PRIMARY KEY (id),
                                    CONSTRAINT ukr4k4edos30bx9neoq81mdvwph UNIQUE (token)
);


-- core.shedlock definition

-- Drop table

-- DROP TABLE core.shedlock;

CREATE TABLE core.shedlock (
                               "name" varchar(64) NOT NULL,
                               lock_until timestamptz NULL,
                               locked_at timestamptz NULL,
                               locked_by varchar(255) NULL,
                               CONSTRAINT shedlock_pkey PRIMARY KEY (name)
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


-- core.batch_job_execution definition

-- Drop table

-- DROP TABLE core.batch_job_execution;

CREATE TABLE core.batch_job_execution (
                                          job_execution_id int8 NOT NULL,
                                          "version" int8 NULL,
                                          job_instance_id int8 NOT NULL,
                                          create_time timestamp NOT NULL,
                                          start_time timestamp NULL,
                                          end_time timestamp NULL,
                                          status varchar(10) NULL,
                                          exit_code varchar(2500) NULL,
                                          exit_message varchar(2500) NULL,
                                          last_updated timestamp NULL,
                                          CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id),
                                          CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES core.batch_job_instance(job_instance_id)
);


-- core.batch_job_execution_context definition

-- Drop table

-- DROP TABLE core.batch_job_execution_context;

CREATE TABLE core.batch_job_execution_context (
                                                  job_execution_id int8 NOT NULL,
                                                  short_context varchar(2500) NOT NULL,
                                                  serialized_context text NULL,
                                                  CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id),
                                                  CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES core.batch_job_execution(job_execution_id)
);


-- core.batch_job_execution_params definition

-- Drop table

-- DROP TABLE core.batch_job_execution_params;

CREATE TABLE core.batch_job_execution_params (
                                                 job_execution_id int8 NOT NULL,
                                                 parameter_name varchar(100) NOT NULL,
                                                 parameter_type varchar(100) NOT NULL,
                                                 parameter_value varchar(2500) NULL,
                                                 identifying bpchar(1) NOT NULL,
                                                 CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES core.batch_job_execution(job_execution_id)
);


-- core.batch_step_execution definition

-- Drop table

-- DROP TABLE core.batch_step_execution;

CREATE TABLE core.batch_step_execution (
                                           step_execution_id int8 NOT NULL,
                                           "version" int8 NOT NULL,
                                           step_name varchar(100) NOT NULL,
                                           job_execution_id int8 NOT NULL,
                                           create_time timestamp NOT NULL,
                                           start_time timestamp NULL,
                                           end_time timestamp NULL,
                                           status varchar(10) NULL,
                                           commit_count int8 NULL,
                                           read_count int8 NULL,
                                           filter_count int8 NULL,
                                           write_count int8 NULL,
                                           read_skip_count int8 NULL,
                                           write_skip_count int8 NULL,
                                           process_skip_count int8 NULL,
                                           rollback_count int8 NULL,
                                           exit_code varchar(2500) NULL,
                                           exit_message varchar(2500) NULL,
                                           last_updated timestamp NULL,
                                           CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id),
                                           CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES core.batch_job_execution(job_execution_id)
);


-- core.batch_step_execution_context definition

-- Drop table

-- DROP TABLE core.batch_step_execution_context;

CREATE TABLE core.batch_step_execution_context (
                                                   step_execution_id int8 NOT NULL,
                                                   short_context varchar(2500) NOT NULL,
                                                   serialized_context text NULL,
                                                   CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id),
                                                   CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES core.batch_step_execution(step_execution_id)
);


-- core.merchant definition

-- Drop table

-- DROP TABLE core.merchant;

CREATE TABLE core.merchant (
                               id uuid NOT NULL,
                               created_at timestamp(6) NULL,
                               "document" varchar(255) NOT NULL,
                               email varchar(255) NULL,
                               legal_name varchar(255) NULL,
                               status varchar(255) NULL,
                               bank_account_id uuid NULL,
                               CONSTRAINT merchant_pkey PRIMARY KEY (id),
                               CONSTRAINT merchant_status_check CHECK (((status)::text = ANY (ARRAY[('PROVISIONAL'::character varying)::text, ('ACTIVE'::character varying)::text, ('SUSPENDED'::character varying)::text, ('TERMINATED'::character varying)::text, ('AWAITING_ACTIVATION'::character varying)::text]))),
	CONSTRAINT ukea7monyqr4u7yaasrl058nfsc UNIQUE (document)
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
                                       CONSTRAINT ukk5ryy7v7njye2y3w9dv2f1qut UNIQUE (merchant_id)
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
                               CONSTRAINT terminal_status_check CHECK (((status)::text = ANY (ARRAY[('AVAILABLE'::character varying)::text, ('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('TERMINATED'::character varying)::text, ('IN_TRANSIT'::character varying)::text]))),
	CONSTRAINT uk4b9sk2ibyw5upn83yu8mblhbb UNIQUE (serial_number)
);


-- core."transaction" definition

-- Drop table

-- DROP TABLE core."transaction";

CREATE TABLE core."transaction" (
                                    id uuid NOT NULL,
                                    amount numeric(19, 2) NULL,
                                    auth_code varchar(20) NULL,
                                    card_bin varchar(6) NULL,
                                    card_brand varchar(20) NULL,
                                    card_holder_name varchar(255) NULL,
                                    card_last_four varchar(4) NULL,
                                    created_at timestamp(6) NULL,
                                    currency varchar(3) NULL,
                                    net_amount numeric(19, 2) NULL,
                                    nsu varchar(20) NULL,
                                    product_type varchar(255) NULL,
                                    source_entry_mode varchar(20) NULL,
                                    source_ip_address varchar(45) NULL,
                                    source_software_version varchar(20) NULL,
                                    source_terminal_sn varchar(50) NULL,
                                    status varchar(255) NULL,
                                    merchant_id uuid NULL,
                                    authorization_code varchar(50) NULL,
                                    external_reference varchar(100) NULL,
                                    terminal_id uuid NULL,
                                    ip_address varchar(255) NULL,
                                    terminal_serial_number varchar(255) NULL,
                                    CONSTRAINT transaction_pkey PRIMARY KEY (id),
                                    CONSTRAINT transaction_product_type_check CHECK (((product_type)::text = ANY (ARRAY[('DEBIT'::character varying)::text, ('CREDIT_A_VISTA'::character varying)::text, ('CREDIT_PARCELADO'::character varying)::text]))),
	CONSTRAINT transaction_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'CANCELLED'::character varying, 'REVERSED'::character varying, 'CHARGED_BACK'::character varying, 'SETTLEMENT_SENT'::character varying])::text[])))
);


-- core.merchant foreign keys

ALTER TABLE core.merchant ADD CONSTRAINT fkpwiyge5r8x58hscrushpp6qqa FOREIGN KEY (bank_account_id) REFERENCES ops.merchant_bank_account(id);


-- core.merchant_address foreign keys

ALTER TABLE core.merchant_address ADD CONSTRAINT fkhjj4mc5v3oo3v8bkobyccc353 FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- core.terminal foreign keys

ALTER TABLE core.terminal ADD CONSTRAINT fkfjidqcv1fnoe1fvxnhxvw2oxc FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- core."transaction" foreign keys

ALTER TABLE core."transaction" ADD CONSTRAINT fkcwlugq1eich0rct1w83pewkqc FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);
ALTER TABLE core."transaction" ADD CONSTRAINT fkhsb9kiwnb5cluw6dijgxm51mm FOREIGN KEY (terminal_id) REFERENCES core.terminal(id);









-- ops.daily_merchant_summary definition

-- Drop table

-- DROP TABLE ops.daily_merchant_summary;

CREATE TABLE ops.daily_merchant_summary (
                                            id uuid NOT NULL,
                                            approved_count int8 NOT NULL,
                                            summary_date date NOT NULL,
                                            merchant_id uuid NOT NULL,
                                            total_count int8 NOT NULL,
                                            total_net_revenue numeric(19, 4) NOT NULL,
                                            total_tpv numeric(19, 4) NOT NULL,
                                            CONSTRAINT daily_merchant_summary_pkey PRIMARY KEY (id),
                                            CONSTRAINT uksqi3h1jaxhxra9oy8uk3d135k UNIQUE (merchant_id, summary_date)
);


-- ops.payout_instruction definition

-- Drop table

-- DROP TABLE ops.payout_instruction;

CREATE TABLE ops.payout_instruction (
                                        id uuid NOT NULL,
                                        merchant_id uuid NOT NULL,
                                        total_amount numeric(19, 4) NOT NULL,
                                        payout_date date DEFAULT CURRENT_DATE NOT NULL,
                                        external_reference varchar(255) NOT NULL,
                                        status varchar(50) NOT NULL,
                                        bank_payout_id varchar(255) NULL,
                                        error_message text NULL,
                                        created_at timestamp DEFAULT CURRENT_DATE NULL,
                                        updated_at timestamp DEFAULT CURRENT_DATE NULL,
                                        CONSTRAINT payout_instruction_pkey PRIMARY KEY (id),
                                        CONSTRAINT uk_merchant_payout_day UNIQUE (merchant_id, payout_date)
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
                                           account_number varchar(30) NULL,
                                           branch_number varchar(20) NULL,
                                           created_at timestamp(6) NULL,
                                           is_primary bool NULL,
                                           CONSTRAINT merchant_bank_account_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['CHECKING'::character varying, 'SAVINGS'::character varying, 'PAYMENT'::character varying])::text[]))),
	CONSTRAINT merchant_bank_account_pkey PRIMARY KEY (id),
	CONSTRAINT ukc6j9s96vq2a4jcu2b3adqhuh UNIQUE (merchant_id)
);


-- ops.merchant_price definition

-- Drop table

-- DROP TABLE ops.merchant_price;

CREATE TABLE ops.merchant_price (
                                    id uuid NOT NULL,
                                    anticipation_fee numeric(38, 2) NULL,
                                    brand varchar(255) NULL,
                                    effective_date date NULL,
                                    mdr_percentage numeric(38, 2) NULL,
                                    product_type varchar(255) NULL,
                                    merchant_id uuid NOT NULL,
                                    CONSTRAINT merchant_price_brand_check CHECK (((brand)::text = ANY ((ARRAY['VISA'::character varying, 'MASTER_CARD'::character varying])::text[]))),
	CONSTRAINT merchant_price_pkey PRIMARY KEY (id),
	CONSTRAINT merchant_price_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['CREDIT_A_VISTA'::character varying, 'CREDIT_PARCELADO'::character varying, 'DEBIT'::character varying])::text[])))
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
                                      anticipation_fee numeric(38, 2) NULL,
                                      created_at timestamp(6) NULL,
                                      CONSTRAINT merchant_pricing_pkey PRIMARY KEY (id),
                                      CONSTRAINT merchant_pricing_product_type_check CHECK (((product_type)::text = ANY (ARRAY[('DEBIT'::character varying)::text, ('CREDIT_A_VISTA'::character varying)::text, ('CREDIT_PARCELADO'::character varying)::text])))
);


-- ops.settlement_entry definition

-- Drop table

-- DROP TABLE ops.settlement_entry;

CREATE TABLE ops.settlement_entry (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      transaction_id uuid NOT NULL,
                                      merchant_id uuid NOT NULL,
                                      terminal_id varchar(255) NULL,
                                      mdr_amount numeric(19, 4) NOT NULL,
                                      net_amount numeric(19, 4) NOT NULL,
                                      expected_settlement_date timestamp(6) NOT NULL,
                                      paid_at timestamp NULL,
                                      status varchar(255) DEFAULT 'SCHEDULED'::character varying NOT NULL,
                                      created_at timestamp DEFAULT now() NOT NULL,
                                      updated_at timestamp DEFAULT now() NOT NULL,
                                      amount numeric(19, 4) NOT NULL,
                                      mdr_rate numeric(5, 4) NULL,
                                      processed_at timestamp(6) NULL,
                                      mdr_percentage numeric(5, 4) NULL,
                                      original_amount numeric(19, 4) NULL,
                                      is_anticipated bool NULL,
                                      is_blocked bool NULL,
                                      installment_number int4 NULL,
                                      total_installments int4 NULL,
                                      retry_count int4 DEFAULT 0 NULL,
                                      blocked_reason varchar(255) NULL,
                                      is_collateral bool NULL,
                                      prepayment_batch_id uuid NULL,
                                      interchange_cost numeric(19, 4) NULL,
                                      scheme_fee_cost numeric(19, 4) NULL,
                                      orion_markup numeric(19, 4) NULL,
                                      CONSTRAINT settlement_entry_pkey PRIMARY KEY (id),
                                      CONSTRAINT uk_settlement_transaction_installment UNIQUE (transaction_id, installment_number)
);
CREATE INDEX idx_settlement_expected_date ON ops.settlement_entry USING btree (expected_settlement_date);
CREATE INDEX idx_settlement_merchant_status ON ops.settlement_entry USING btree (merchant_id, status);
CREATE INDEX idx_settlement_pending_recovery ON ops.settlement_entry USING btree (status, created_at) WHERE ((status)::text = 'PENDING'::text);


-- ops.merchant_bank_account foreign keys

ALTER TABLE ops.merchant_bank_account ADD CONSTRAINT fksff2ang5y3ii7godqjefo2qch FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.merchant_price foreign keys

ALTER TABLE ops.merchant_price ADD CONSTRAINT fkij2uniamfcil5xio7qi9mjlli FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.merchant_pricing foreign keys

ALTER TABLE ops.merchant_pricing ADD CONSTRAINT fk9ay31si3sulan27xj3q31a07n FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);


-- ops.settlement_entry foreign keys

ALTER TABLE ops.settlement_entry ADD CONSTRAINT fk_settlement_merchant FOREIGN KEY (merchant_id) REFERENCES core.merchant(id);
ALTER TABLE ops.settlement_entry ADD CONSTRAINT fk_settlement_transaction FOREIGN KEY (transaction_id) REFERENCES core."transaction"(id);