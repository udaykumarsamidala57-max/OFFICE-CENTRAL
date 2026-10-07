-- Run on EACH company inventory database used by OFFICE_CENTRAL_ERP.
-- PInventory Dining Hall tables are expected to exist: dining_hall_consumption,
-- stock_issues, stock_ledger, stock, item_master, category, dept_cate, po_items.
-- This creates a transactional issue-number sequence; existing issue numbers seed it.
CREATE TABLE IF NOT EXISTS inventory_sequences (
    sequence_name VARCHAR(40) NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (sequence_name)
) ENGINE=InnoDB;

INSERT IGNORE INTO inventory_sequences(sequence_name,last_value)
SELECT 'dining_hall_issue_no',COALESCE(MAX(CAST(SUBSTRING(issueno,4) AS UNSIGNED)),0)
FROM dining_hall_consumption;

SELECT DATABASE() AS company_database;
SHOW TABLES LIKE 'dining_hall_consumption';
SHOW TABLES LIKE 'inventory_sequences';
