-- Run this script on EACH company inventory database used by OFFICE_CENTRAL_ERP:
-- inventory, SANPOLY_INVENTORY, SANPOLY_INVENTORY2, and SRS_HOSTEL (if enabled).
-- The selected company's database must already contain po_master, po_items,
-- item_master, stock, and stock_ledger from PInventory.

CREATE TABLE IF NOT EXISTS grn_master (
    grn_id INT NOT NULL AUTO_INCREMENT,
    grn_no VARCHAR(50) NOT NULL,
    grn_date DATE NOT NULL,
    po_id INT NOT NULL,
    po_number VARCHAR(50) NOT NULL,
    vendor_name VARCHAR(150) NOT NULL,
    vendor_gstin VARCHAR(45) NULL,
    vendor_address VARCHAR(500) NULL,
    invoice_no VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    received_by VARCHAR(100) NOT NULL,
    remarks VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (grn_id),
    UNIQUE KEY uq_grn_master_number (grn_no),
    KEY idx_grn_master_po (po_id),
    KEY idx_grn_master_date (grn_date),
    KEY idx_grn_master_invoice (invoice_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS grn_items (
    grn_item_id INT NOT NULL AUTO_INCREMENT,
    grn_id INT NOT NULL,
    po_item_id INT NOT NULL,
    item_id INT NOT NULL,
    item_description VARCHAR(255) NOT NULL,
    qty_received DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    qty_accepted DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    qty_rejected DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    remarks VARCHAR(500) NULL,
    PRIMARY KEY (grn_item_id),
    KEY idx_grn_items_master (grn_id),
    KEY idx_grn_items_po_item (po_item_id),
    KEY idx_grn_items_item (item_id),
    CONSTRAINT fk_grn_items_master FOREIGN KEY (grn_id)
        REFERENCES grn_master(grn_id) ON DELETE CASCADE
) ENGINE=InnoDB;

SELECT DATABASE() AS company_database;
SHOW TABLES LIKE 'grn_master';
SHOW TABLES LIKE 'grn_items';
