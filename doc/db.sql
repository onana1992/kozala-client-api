-- Core Banking - Client/KYC schema (MySQL)

-- Clients
CREATE TABLE IF NOT EXISTS clients (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type ENUM('PERSON','BUSINESS') NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  first_name VARCHAR(150) NULL,
  last_name VARCHAR(150) NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  status ENUM('DRAFT','PENDING_REVIEW','VERIFIED','REJECTED','BLOCKED') NOT NULL DEFAULT 'DRAFT',
  risk_score INT NULL,
  pep_flag BOOLEAN NOT NULL DEFAULT FALSE,
  rejection_reason TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_clients_email (email),
  UNIQUE KEY uk_clients_phone (phone),
  INDEX idx_clients_email (email),
  INDEX idx_clients_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Addresses
CREATE TABLE IF NOT EXISTS addresses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  type ENUM('RESIDENTIAL','BUSINESS','MAILING') NOT NULL,
  line1 VARCHAR(255) NOT NULL,
  line2 VARCHAR(255) NULL,
  city VARCHAR(100) NOT NULL,
  state VARCHAR(100) NULL,
  postal_code VARCHAR(30) NULL,
  country VARCHAR(2) NOT NULL,
  primary_address BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_addresses_client (client_id),
  CONSTRAINT fk_addresses_client
    FOREIGN KEY (client_id) REFERENCES clients(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Documents
CREATE TABLE IF NOT EXISTS documents (
  id BIGINT NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  type ENUM('ID_CARD','PASSPORT','PROOF_OF_ADDRESS','REGISTRATION_DOC','SELFIE') NOT NULL,
  file_name VARCHAR(255) NULL,
  content_type VARCHAR(100) NULL,
  storage_key VARCHAR(500) NOT NULL,
  checksum VARCHAR(128) NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  reviewer_note TEXT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX idx_documents_client (client_id),
  INDEX idx_documents_type (type),
  CONSTRAINT fk_documents_client
    FOREIGN KEY (client_id) REFERENCES clients(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Related Persons (for BUSINESS clients: UBOs, directors, etc.)
CREATE TABLE IF NOT EXISTS related_persons (
  id BIGINT NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  role ENUM('UBO','DIRECTOR','SIGNATORY') NOT NULL,
  first_name VARCHAR(150) NOT NULL,
  last_name VARCHAR(150) NOT NULL,
  date_of_birth DATE NULL,
  national_id VARCHAR(64) NULL,
  pep_flag BOOLEAN NOT NULL DEFAULT FALSE,
  ownership_percent DECIMAL(5,2) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_related_persons_client (client_id),
  INDEX idx_related_persons_role (role),
  CONSTRAINT fk_related_persons_client
    FOREIGN KEY (client_id) REFERENCES clients(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- KYC Checks (results of screenings/verifications)
CREATE TABLE IF NOT EXISTS kyc_checks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  type ENUM('ID_VERIFICATION','SANCTIONS_SCREENING','PEP_SCREENING','ADDRESS_VALIDATION') NOT NULL,
  provider VARCHAR(100) NULL,
  request_ref VARCHAR(100) NULL,
  result ENUM('PASS','FAIL','REVIEW') NOT NULL,
  score INT NULL,
  raw_json LONGTEXT NULL,
  checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_kyc_checks_client (client_id),
  INDEX idx_kyc_checks_type (type),
  INDEX idx_kyc_checks_result (result),
  CONSTRAINT fk_kyc_checks_client
    FOREIGN KEY (client_id) REFERENCES clients(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- PRODUCT CATALOG MODULE
-- ============================================

-- Products (comptes courants, épargne, terme, prêts, cartes)
CREATE TABLE IF NOT EXISTS products (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  category ENUM('CURRENT_ACCOUNT','SAVINGS_ACCOUNT','TERM_DEPOSIT','LOAN','CARD') NOT NULL,
  status ENUM('ACTIVE','INACTIVE','DRAFT') NOT NULL DEFAULT 'DRAFT',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  min_balance DECIMAL(15,2) NULL,
  max_balance DECIMAL(15,2) NULL,
  default_interest_rate DECIMAL(8,4) NULL COMMENT 'Taux d''intérêt par défaut (en pourcentage)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_products_code (code),
  INDEX idx_products_category (category),
  INDEX idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Interest Rates (taux d'intérêt variables selon montant, période, etc.)
CREATE TABLE IF NOT EXISTS product_interest_rates (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  rate_type ENUM('DEPOSIT','LENDING','PENALTY') NOT NULL,
  rate_value DECIMAL(8,4) NOT NULL COMMENT 'Taux en pourcentage (ex: 2.5 pour 2.5%)',
  min_amount DECIMAL(15,2) NULL COMMENT 'Montant minimum pour ce taux',
  max_amount DECIMAL(15,2) NULL COMMENT 'Montant maximum pour ce taux',
  min_period_days INT NULL COMMENT 'Période minimum en jours',
  max_period_days INT NULL COMMENT 'Période maximum en jours',
  calculation_method ENUM('SIMPLE','COMPOUND','FLOATING') NOT NULL DEFAULT 'SIMPLE',
  compounding_frequency ENUM('DAILY','MONTHLY','QUARTERLY','YEARLY') NULL,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_interest_rates_product (product_id),
  INDEX idx_product_interest_rates_type (rate_type),
  INDEX idx_product_interest_rates_dates (effective_from, effective_to),
  CONSTRAINT fk_product_interest_rates_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Fees (frais associés aux produits)
CREATE TABLE IF NOT EXISTS product_fees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  fee_type ENUM('OPENING','MONTHLY','ANNUAL','TRANSACTION','WITHDRAWAL','CARD_ISSUANCE','CARD_RENEWAL','OTHER') NOT NULL,
  transaction_type ENUM('DEPOSIT','WITHDRAWAL','TRANSFER','FEE','INTEREST','ADJUSTMENT','REVERSAL') NULL COMMENT 'Type de transaction associé (NULL = s''applique à tous les types)',
  fee_name VARCHAR(255) NOT NULL,
  fee_amount DECIMAL(15,2) NULL COMMENT 'Montant fixe',
  fee_percentage DECIMAL(8,4) NULL COMMENT 'Pourcentage (ex: 1.5 pour 1.5%)',
  fee_calculation_base ENUM('FIXED','BALANCE','TRANSACTION_AMOUNT','OUTSTANDING_BALANCE') NOT NULL DEFAULT 'FIXED',
  min_fee DECIMAL(15,2) NULL COMMENT 'Frais minimum',
  max_fee DECIMAL(15,2) NULL COMMENT 'Frais maximum',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  is_waivable BOOLEAN NOT NULL DEFAULT FALSE,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_fees_product (product_id),
  INDEX idx_product_fees_type (fee_type),
  INDEX idx_product_fees_transaction_type (transaction_type),
  INDEX idx_product_fees_dates (effective_from, effective_to),
  -- Note: L'unicité des frais mensuels et annuels est gérée au niveau applicatif
  -- pour permettre une gestion flexible des dates d'effet qui se chevauchent
  CONSTRAINT fk_product_fees_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Limits (limites de montant, transactions, etc.)
CREATE TABLE IF NOT EXISTS product_limits (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  limit_type ENUM('MIN_BALANCE','MAX_BALANCE','MIN_TRANSACTION','MAX_TRANSACTION','DAILY_LIMIT','WEEKLY_LIMIT','MONTHLY_LIMIT','ANNUAL_LIMIT','CARD_LIMIT') NOT NULL,
  transaction_type VARCHAR(50) NULL,
  limit_value DECIMAL(15,2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  period_type ENUM('TRANSACTION','DAILY','WEEKLY','MONTHLY','ANNUAL','LIFETIME') NULL,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_limits_product (product_id),
  INDEX idx_product_limits_type (limit_type),
  INDEX idx_product_limits_transaction_type (transaction_type),
  INDEX idx_product_limits_dates (effective_from, effective_to),
  CONSTRAINT fk_product_limits_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Periods (périodes pour dépôts à terme, prêts, etc.)
CREATE TABLE IF NOT EXISTS product_periods (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  period_name VARCHAR(100) NOT NULL,
  period_days INT NOT NULL COMMENT 'Durée en jours',
  period_months INT NULL COMMENT 'Durée en mois (pour affichage)',
  period_years INT NULL COMMENT 'Durée en années (pour affichage)',
  interest_rate DECIMAL(8,4) NULL COMMENT 'Taux d''intérêt spécifique pour cette période',
  min_amount DECIMAL(15,2) NULL,
  max_amount DECIMAL(15,2) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  display_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_periods_product (product_id),
  CONSTRAINT fk_product_periods_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Penalties (pénalités: retrait anticipé, dépassement, etc.)
CREATE TABLE IF NOT EXISTS product_penalties (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  penalty_type ENUM('EARLY_WITHDRAWAL','OVERDRAFT','LATE_PAYMENT','MIN_BALANCE_VIOLATION','EXCESS_TRANSACTION','PREPAYMENT','OTHER') NOT NULL,
  penalty_name VARCHAR(255) NOT NULL,
  penalty_amount DECIMAL(15,2) NULL COMMENT 'Montant fixe',
  penalty_percentage DECIMAL(8,4) NULL COMMENT 'Pourcentage (ex: 2.0 pour 2%)',
  calculation_base ENUM('FIXED','PRINCIPAL','INTEREST','BALANCE','TRANSACTION_AMOUNT') NOT NULL DEFAULT 'FIXED',
  min_penalty DECIMAL(15,2) NULL,
  max_penalty DECIMAL(15,2) NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  grace_period_days INT NULL COMMENT 'Période de grâce en jours',
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_penalties_product (product_id),
  INDEX idx_product_penalties_type (penalty_type),
  INDEX idx_product_penalties_dates (effective_from, effective_to),
  CONSTRAINT fk_product_penalties_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product Eligibility Rules (règles d'éligibilité)
CREATE TABLE IF NOT EXISTS product_eligibility_rules (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  rule_type ENUM('MIN_AGE','MAX_AGE','MIN_INCOME','MIN_BALANCE','CLIENT_TYPE','CLIENT_STATUS','RESIDENCY','KYC_LEVEL','RISK_SCORE','PEP_FLAG','OTHER') NOT NULL,
  rule_name VARCHAR(255) NOT NULL,
  operator ENUM('EQUALS','NOT_EQUALS','GREATER_THAN','GREATER_THAN_OR_EQUAL','LESS_THAN','LESS_THAN_OR_EQUAL','IN','NOT_IN','CONTAINS') NOT NULL,
  rule_value VARCHAR(500) NOT NULL COMMENT 'Valeur de la règle (peut être JSON pour valeurs multiples)',
  data_type ENUM('STRING','NUMBER','BOOLEAN','DATE','ENUM') NOT NULL DEFAULT 'STRING',
  is_mandatory BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Si TRUE, la règle doit être satisfaite. Si FALSE, c''est une recommandation.',
  error_message TEXT NULL COMMENT 'Message d''erreur si la règle n''est pas satisfaite',
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_product_eligibility_rules_product (product_id),
  INDEX idx_product_eligibility_rules_type (rule_type),
  INDEX idx_product_eligibility_rules_dates (effective_from, effective_to),
  CONSTRAINT fk_product_eligibility_rules_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- ACCOUNTS MODULE (Comptes clients)
-- ============================================

-- Accounts (comptes clients liés aux produits)
CREATE TABLE IF NOT EXISTS accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  account_number VARCHAR(50) NOT NULL UNIQUE,
  status ENUM('ACTIVE','CLOSED','FROZEN','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  available_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  opening_amount DECIMAL(15,2) NULL,
  interest_rate DECIMAL(8,4) NULL,
  period_id BIGINT NULL COMMENT 'Référence à product_periods pour dépôts à terme/prêts',
  maturity_date DATE NULL COMMENT 'Date d''échéance pour dépôts à terme/prêts',
  opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at TIMESTAMP NULL,
  closed_reason TEXT NULL,
  opened_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_accounts_account_number (account_number),
  INDEX idx_accounts_client (client_id),
  INDEX idx_accounts_product (product_id),
  INDEX idx_accounts_status (status),
  INDEX idx_accounts_account_number_lookup (account_number),
  CONSTRAINT fk_accounts_client
    FOREIGN KEY (client_id) REFERENCES clients(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_accounts_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Échéancier des prêts (plan d'amortissement)
CREATE TABLE IF NOT EXISTS loan_schedule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL COMMENT 'Compte prêt (accounts.id)',
  installment_number INT NOT NULL,
  due_date DATE NOT NULL,
  principal_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  interest_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  total_amount DECIMAL(15,2) NOT NULL,
  outstanding_principal DECIMAL(15,2) NOT NULL,
  paid_principal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  paid_interest DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  status ENUM('PENDING','PAID','OVERDUE','PARTIAL') NOT NULL DEFAULT 'PENDING',
  paid_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_loan_schedule_account (account_id),
  INDEX idx_loan_schedule_due_date (due_date),
  CONSTRAINT fk_loan_schedule_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Demandes de prêt (workflow UC-L07 / UC-L08)
CREATE TABLE IF NOT EXISTS loan_applications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  application_number VARCHAR(50) NOT NULL UNIQUE,
  client_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  period_id BIGINT NOT NULL,
  requested_amount DECIMAL(15,2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
  source_account_id BIGINT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  requested_by BIGINT NULL,
  decided_at TIMESTAMP NULL,
  decided_by BIGINT NULL,
  rejection_reason TEXT NULL,
  account_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_loan_applications_number (application_number),
  INDEX idx_loan_applications_client (client_id),
  INDEX idx_loan_applications_status (status),
  INDEX idx_loan_applications_requested_at (requested_at),
  CONSTRAINT fk_loan_applications_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_period FOREIGN KEY (period_id) REFERENCES product_periods(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_requested_by FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_decided_by FOREIGN KEY (decided_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_loan_applications_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TRANSACTION ENGINE MODULE (Moteur de Transactions)
-- ============================================

-- Transactions (opérations financières avec double écriture)
CREATE TABLE IF NOT EXISTS transactions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  transaction_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Numéro unique de transaction',
  type ENUM('DEPOSIT','WITHDRAWAL','TRANSFER','FEE','INTEREST','ADJUSTMENT','REVERSAL','LOAN_REPAYMENT') NOT NULL COMMENT 'Type de transaction',
  status ENUM('PENDING','PROCESSING','COMPLETED','FAILED','REVERSED') NOT NULL DEFAULT 'PENDING' COMMENT 'Statut de la transaction',
  amount DECIMAL(15,2) NOT NULL COMMENT 'Montant de la transaction',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  account_id BIGINT NOT NULL COMMENT 'Compte concerné',
  reference_type VARCHAR(50) NULL COMMENT 'Type de référence externe (ex: TRANSFER, FEE, QR_CODE_PAYMENT)',
  reference_id BIGINT NULL COMMENT 'ID de la référence externe',
  description VARCHAR(500) NULL COMMENT 'Description de la transaction',
  metadata JSON NULL COMMENT 'Métadonnées supplémentaires (JSON)',
  value_date DATE NOT NULL COMMENT 'Date de valeur (date comptable)',
  transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date/heure de la transaction',
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transactions_transaction_number (transaction_number),
  INDEX idx_transactions_account (account_id),
  INDEX idx_transactions_type (type),
  INDEX idx_transactions_status (status),
  INDEX idx_transactions_reference (reference_type, reference_id),
  INDEX idx_transactions_value_date (value_date),
  INDEX idx_transactions_transaction_date (transaction_date),
  CONSTRAINT fk_transactions_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transactions_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transaction Entries (écritures comptables - double écriture)
CREATE TABLE IF NOT EXISTS transaction_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL COMMENT 'Référence à la transaction',
  ledger_account_id BIGINT NULL COMMENT 'ID du compte du Grand Livre (nullable jusqu''à implémentation du GL)',
  entry_type ENUM('DEBIT','CREDIT') NOT NULL COMMENT 'Type d''écriture (débit ou crédit)',
  amount DECIMAL(15,2) NOT NULL COMMENT 'Montant de l''écriture',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_transaction_entries_transaction (transaction_id),
  INDEX idx_transaction_entries_ledger_account (ledger_account_id),
  INDEX idx_transaction_entries_type (entry_type),
  CONSTRAINT fk_transaction_entries_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Accrued Interest (intérêts courus pour capitalisation quotidienne)
CREATE TABLE IF NOT EXISTS accrued_interest (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL COMMENT 'Compte concerné',
  accrual_date DATE NOT NULL COMMENT 'Date de calcul des intérêts courus',
  interest_amount DECIMAL(15,2) NOT NULL COMMENT 'Montant des intérêts courus',
  principal_amount DECIMAL(15,2) NOT NULL COMMENT 'Montant du principal utilisé pour le calcul',
  interest_rate DECIMAL(8,4) NOT NULL COMMENT 'Taux d''intérêt utilisé',
  calculation_method ENUM('SIMPLE','COMPOUND','FLOATING') NOT NULL DEFAULT 'SIMPLE' COMMENT 'Méthode de calcul',
  compounding_frequency ENUM('DAILY','MONTHLY','QUARTERLY','YEARLY') NULL COMMENT 'Fréquence de capitalisation',
  is_capitalized BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Indique si les intérêts ont été capitalisés',
  capitalized_date DATE NULL COMMENT 'Date de capitalisation (versement)',
  capitalized_transaction_id BIGINT NULL COMMENT 'ID de la transaction de capitalisation',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_accrued_interest_account (account_id),
  INDEX idx_accrued_interest_date (accrual_date),
  INDEX idx_accrued_interest_capitalized (is_capitalized),
  INDEX idx_accrued_interest_account_date (account_id, accrual_date),
  UNIQUE KEY uk_accrued_interest_account_date (account_id, accrual_date),
  CONSTRAINT fk_accrued_interest_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_accrued_interest_transaction
    FOREIGN KEY (capitalized_transaction_id) REFERENCES transactions(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Holds (réservations de fonds)
CREATE TABLE IF NOT EXISTS holds (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL COMMENT 'Compte concerné', amount DECIMAL(15,2) NOT NULL COMMENT 'Montant réservé',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  reason VARCHAR(255) NOT NULL COMMENT 'Raison de la réservation',
  status ENUM('PENDING','RELEASED','APPLIED','EXPIRED') NOT NULL DEFAULT 'PENDING' COMMENT 'Statut de la réservation',
  expires_at TIMESTAMP NULL COMMENT 'Date/heure d''expiration de la réservation',
  transaction_id BIGINT NULL COMMENT 'Transaction liée (si applicable)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  released_at TIMESTAMP NULL COMMENT 'Date/heure de libération',
  PRIMARY KEY (id),
  INDEX idx_holds_account (account_id),
  INDEX idx_holds_status (status),
  INDEX idx_holds_transaction (transaction_id),
  INDEX idx_holds_expires_at (expires_at),
  CONSTRAINT fk_holds_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_holds_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reversals (annulations de transactions)
CREATE TABLE IF NOT EXISTS reversals (
  id BIGINT NOT NULL AUTO_INCREMENT,
  original_transaction_id BIGINT NOT NULL COMMENT 'Transaction originale à annuler',
  reversal_transaction_id BIGINT NOT NULL COMMENT 'Transaction de réversal créée',
  reason VARCHAR(500) NOT NULL COMMENT 'Raison de l''annulation',
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur ayant créé le réversal',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reversals_original_transaction (original_transaction_id),
  INDEX idx_reversals_reversal (reversal_transaction_id),
  INDEX idx_reversals_created_by (created_by),
  CONSTRAINT fk_reversals_original_transaction
    FOREIGN KEY (original_transaction_id) REFERENCES transactions(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_reversals_reversal_transaction
    FOREIGN KEY (reversal_transaction_id) REFERENCES transactions(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_reversals_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Idempotency Keys (gestion de l'idempotence des transactions)
CREATE TABLE IF NOT EXISTS idempotency_keys (
  id BIGINT NOT NULL AUTO_INCREMENT,
  `key` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Clé d''idempotence unique',
  transaction_id BIGINT NOT NULL COMMENT 'ID de la transaction associée',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotency_key (`key`),
  INDEX idx_idempotency_transaction (transaction_id),
  CONSTRAINT fk_idempotency_transaction
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- AUTHENTICATION & SECURITY MODULE
-- ============================================

-- Users (utilisateurs du système)
CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status ENUM('ACTIVE','INACTIVE','LOCKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  first_name VARCHAR(150) NULL,
  last_name VARCHAR(150) NULL,
  last_login_at TIMESTAMP NULL,
  failed_login_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  INDEX idx_users_username (username),
  INDEX idx_users_email (email),
  INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Roles (rôles des utilisateurs)
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_name (name),
  INDEX idx_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permissions (permissions du système)
CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  resource VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  description TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_permissions_name (name),
  INDEX idx_permissions_resource (resource),
  INDEX idx_permissions_action (action),
  INDEX idx_permissions_resource_action (resource, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Roles (table de liaison ManyToMany entre users et roles)
CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  INDEX idx_user_roles_user (user_id),
  INDEX idx_user_roles_role (role_id),
  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role Permissions (table de liaison ManyToMany entre roles et permissions)
CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  INDEX idx_role_permissions_role (role_id),
  INDEX idx_role_permissions_permission (permission_id),
  CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES roles(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit Events (événements d'audit pour traçabilité)
CREATE TABLE IF NOT EXISTS audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id BIGINT NULL,
  details TEXT NULL,
  ip_address VARCHAR(45) NULL,
  user_agent VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_audit_events_user (user_id),
  INDEX idx_audit_events_action (action),
  INDEX idx_audit_events_resource (resource_type, resource_id),
  INDEX idx_audit_events_created_at (created_at),
  CONSTRAINT fk_audit_events_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transfers (virements internes entre comptes)
CREATE TABLE IF NOT EXISTS transfers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  transfer_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Numéro unique de transfert',
  from_account_id BIGINT NOT NULL COMMENT 'Compte source',
  to_account_id BIGINT NOT NULL COMMENT 'Compte destination',
  amount DECIMAL(15,2) NOT NULL COMMENT 'Montant du transfert',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  status ENUM('PENDING','PROCESSING','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT 'Statut du transfert',
  description VARCHAR(500) NULL COMMENT 'Description du transfert',
  reference VARCHAR(255) NULL COMMENT 'Référence externe du transfert',
  fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Montant des frais de transfert',
  fee_transaction_id BIGINT NULL COMMENT 'ID de la transaction FEE associée',
  from_transaction_id BIGINT NULL COMMENT 'ID de la transaction de débit (compte source)',
  to_transaction_id BIGINT NULL COMMENT 'ID de la transaction de crédit (compte destination)',
  metadata JSON NULL COMMENT 'Métadonnées supplémentaires (JSON)',
  value_date DATE NOT NULL COMMENT 'Date de valeur (date comptable)',
  execution_date TIMESTAMP NULL COMMENT 'Date/heure d''exécution effective',
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transfers_transfer_number (transfer_number),
  INDEX idx_transfers_from_account (from_account_id),
  INDEX idx_transfers_to_account (to_account_id),
  INDEX idx_transfers_status (status),
  INDEX idx_transfers_value_date (value_date),
  INDEX idx_transfers_execution_date (execution_date),
  INDEX idx_transfers_reference (reference),
  INDEX idx_transfers_from_transaction (from_transaction_id),
  INDEX idx_transfers_to_transaction (to_transaction_id),
  INDEX idx_transfers_fee_transaction (fee_transaction_id),
  CONSTRAINT fk_transfers_from_account
    FOREIGN KEY (from_account_id) REFERENCES accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transfers_to_account
    FOREIGN KEY (to_account_id) REFERENCES accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transfers_fee_transaction
    FOREIGN KEY (fee_transaction_id) REFERENCES transactions(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transfers_from_transaction
    FOREIGN KEY (from_transaction_id) REFERENCES transactions(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transfers_to_transaction
    FOREIGN KEY (to_transaction_id) REFERENCES transactions(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT,
  CONSTRAINT fk_transfers_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- GRAND LIVRE MODULE (Partie 1)
-- ============================================

-- Chart of Accounts (Plan comptable)
CREATE TABLE IF NOT EXISTS chart_of_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(20) NOT NULL COMMENT 'Code unique du compte comptable (ex: 1000, 1100, 1101)',
  name VARCHAR(255) NOT NULL COMMENT 'Nom du compte comptable',
  description TEXT NULL COMMENT 'Description détaillée du compte',
  account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL COMMENT 'Type de compte comptable',
  category VARCHAR(50) NULL COMMENT 'Catégorie du compte (ex: CURRENT_ASSETS, FIXED_ASSETS)',
  parent_code VARCHAR(20) NULL COMMENT 'Code du compte parent pour la hiérarchie',
  level INT NOT NULL DEFAULT 1 COMMENT 'Niveau dans la hiérarchie (1 = racine, 2 = sous-compte, etc.)',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Indique si le compte est actif',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  PRIMARY KEY (id),
  UNIQUE KEY uk_chart_of_accounts_code (code),
  INDEX idx_chart_of_accounts_code (code),
  INDEX idx_chart_of_accounts_parent (parent_code),
  INDEX idx_chart_of_accounts_type (account_type),
  INDEX idx_chart_of_accounts_active (is_active),
  CONSTRAINT fk_chart_of_accounts_parent
    FOREIGN KEY (parent_code) REFERENCES chart_of_accounts(code)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_chart_of_accounts_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger Accounts (Comptes du Grand Livre)
CREATE TABLE IF NOT EXISTS ledger_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL COMMENT 'Code unique du compte GL',
  name VARCHAR(255) NOT NULL COMMENT 'Nom du compte GL',
  chart_of_account_code VARCHAR(20) NOT NULL COMMENT 'Code du compte comptable associé',
  account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL COMMENT 'Type de compte GL',
  currency VARCHAR(3) NOT NULL DEFAULT 'USD' COMMENT 'Code devise ISO-3',
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Statut du compte GL',
  balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Solde du compte GL (calculé à partir des écritures)',
  available_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Solde disponible (après déduction des réservations)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ledger_accounts_code (code),
  INDEX idx_ledger_accounts_code (code),
  INDEX idx_ledger_accounts_chart (chart_of_account_code),
  INDEX idx_ledger_accounts_type (account_type),
  INDEX idx_ledger_accounts_status (status),
  INDEX idx_ledger_accounts_currency (currency),
  CONSTRAINT fk_ledger_accounts_chart_of_account
    FOREIGN KEY (chart_of_account_code) REFERENCES chart_of_accounts(code)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_ledger_accounts_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Product GL Mappings (Mappings entre produits bancaires et comptes GL)
CREATE TABLE IF NOT EXISTS product_gl_mappings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL COMMENT 'ID du produit bancaire',
  mapping_type ENUM('ASSET_ACCOUNT','LIABILITY_ACCOUNT','FEE_ACCOUNT','INTEREST_ACCOUNT','REVENUE_ACCOUNT','EXPENSE_ACCOUNT') NOT NULL COMMENT 'Type de mapping',
  ledger_account_id BIGINT NOT NULL COMMENT 'ID du compte GL cible',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_gl_mappings_product_type (product_id, mapping_type) COMMENT 'Un seul mapping par type pour un produit',
  INDEX idx_product_gl_mappings_product (product_id),
  INDEX idx_product_gl_mappings_ledger (ledger_account_id),
  INDEX idx_product_gl_mappings_type (mapping_type),
  CONSTRAINT fk_product_gl_mappings_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT fk_product_gl_mappings_ledger_account
    FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_product_gl_mappings_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- GRAND LIVRE MODULE (Partie 2)
-- ============================================

-- Journal Batches (Lots de journalisation)
CREATE TABLE IF NOT EXISTS journal_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_number VARCHAR(50) NOT NULL COMMENT 'Numéro unique du lot (ex: JRNL-2024-001)',
  batch_date DATE NOT NULL COMMENT 'Date du lot de journalisation',
  status ENUM('DRAFT','POSTED','CLOSED') NOT NULL DEFAULT 'DRAFT' COMMENT 'Statut du lot de journalisation',
  total_debit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des débits du lot',
  total_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des crédits du lot',
  description VARCHAR(500) NULL COMMENT 'Description du lot',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  posted_at TIMESTAMP NULL COMMENT 'Date/heure de posting du lot',
  closed_at TIMESTAMP NULL COMMENT 'Date/heure de clôture du lot',
  PRIMARY KEY (id),
  UNIQUE KEY uk_journal_batches_number (batch_number),
  INDEX idx_journal_batches_date (batch_date),
  INDEX idx_journal_batches_status (status),
  CONSTRAINT fk_journal_batches_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger Entries (Écritures du Grand Livre)
CREATE TABLE IF NOT EXISTS ledger_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  journal_batch_id BIGINT NULL COMMENT 'Référence au lot de journalisation (nullable)',
  ledger_account_id BIGINT NOT NULL COMMENT 'ID du compte GL concerné',
  entry_date DATE NOT NULL COMMENT 'Date d''enregistrement de l''écriture',
  value_date DATE NOT NULL COMMENT 'Date de valeur (date effective de l''opération)',
  debit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Montant au débit (0 si crédit)',
  credit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Montant au crédit (0 si débit)',
  currency VARCHAR(3) NOT NULL COMMENT 'Code devise ISO-3 (doit correspondre à la devise du compte GL)',
  description VARCHAR(500) NULL COMMENT 'Description de l''écriture',
  reference_type VARCHAR(50) NULL COMMENT 'Type de référence (TRANSACTION, TRANSFER, FEE, INTEREST, etc.)',
  reference_id BIGINT NULL COMMENT 'ID de la référence source (transaction, virement, etc.)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  PRIMARY KEY (id),
  INDEX idx_ledger_entries_journal_batch (journal_batch_id),
  INDEX idx_ledger_entries_ledger_account (ledger_account_id),
  INDEX idx_ledger_entries_entry_date (entry_date),
  INDEX idx_ledger_entries_value_date (value_date),
  INDEX idx_ledger_entries_reference (reference_type, reference_id),
  CONSTRAINT fk_ledger_entries_journal_batch
    FOREIGN KEY (journal_batch_id) REFERENCES journal_batches(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT,
  CONSTRAINT fk_ledger_entries_ledger_account
    FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT fk_ledger_entries_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Balance Snapshots (Snapshots de solde des comptes GL)
CREATE TABLE IF NOT EXISTS balance_snapshots (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ledger_account_id BIGINT NOT NULL COMMENT 'ID du compte GL',
  snapshot_date DATE NOT NULL COMMENT 'Date du snapshot',
  currency VARCHAR(3) NOT NULL COMMENT 'Code devise ISO-3',
  opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Solde d''ouverture à la date',
  closing_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Solde de clôture à la date',
  total_debit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des débits sur la période',
  total_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des crédits sur la période',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_balance_snapshots_account_date (ledger_account_id, snapshot_date),
  INDEX idx_balance_snapshots_ledger_account (ledger_account_id),
  INDEX idx_balance_snapshots_date (snapshot_date),
  CONSTRAINT fk_balance_snapshots_ledger_account
    FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Closures (Clôtures journalières, mensuelles, annuelles)
CREATE TABLE IF NOT EXISTS closures (
  id BIGINT NOT NULL AUTO_INCREMENT,
  closure_date DATE NOT NULL COMMENT 'Date de clôture',
  closure_type ENUM('DAILY','MONTHLY','YEARLY') NOT NULL COMMENT 'Type de clôture',
  status ENUM('IN_PROGRESS','COMPLETED','FAILED') NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'Statut de la clôture',
  total_debit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des débits',
  total_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Total des crédits',
  balance_check BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Indique si l''équilibre est vérifié (débits = crédits)',
  description TEXT NULL COMMENT 'Description de la clôture',
  error_message TEXT NULL COMMENT 'Message d''erreur si échec',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date/heure de création',
  completed_at TIMESTAMP NULL COMMENT 'Date/heure de finalisation',
  created_by BIGINT NULL COMMENT 'ID de l''utilisateur créateur',
  PRIMARY KEY (id),
  UNIQUE KEY uk_closures_date_type (closure_date, closure_type),
  INDEX idx_closures_date (closure_date),
  INDEX idx_closures_type (closure_type),
  INDEX idx_closures_status (status),
  INDEX idx_closures_date_type (closure_date, closure_type),
  CONSTRAINT fk_closures_created_by
    FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


