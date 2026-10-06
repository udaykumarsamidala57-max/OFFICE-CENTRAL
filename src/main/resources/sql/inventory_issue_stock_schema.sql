-- Run this once in EACH company inventory database used by OFFICE_CENTRAL_ERP.
-- Existing PInventory tables required: indent, stock, stock_issues, stock_ledger,
-- po_items, item_master. This table keeps issue numbers sequential under concurrency.
CREATE TABLE IF NOT EXISTS inventory_sequences (
    sequence_name VARCHAR(40) NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (sequence_name)
) ENGINE=InnoDB;

INSERT IGNORE INTO inventory_sequences(sequence_name,last_value)
SELECT 'stock_issue_no', COALESCE(MAX(CAST(issueno AS UNSIGNED)),0) FROM stock_issues;

SELECT DATABASE() AS company_database;
SHOW TABLES LIKE 'inventory_sequences';
