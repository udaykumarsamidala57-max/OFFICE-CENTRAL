-- Run on the SRS authentication database (inventory) after page_catalog_schema.sql.
-- Registers GRN pages and buttons in the access catalog; this does not grant them.
USE inventory;

INSERT IGNORE INTO app_pages (page_code, page_name, page_url) VALUES
    ('APPROVE_PURCHASE_ORDERS', 'Approve Purchase Orders', '/purchase-orders/approvals'),
    ('GRN_ENTRY', 'GRN Entry', '/grn/entry'),
    ('GRN_REPORT', 'GRN Report', '/grn/report');

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'CREATE_GRN', 'Create GRN' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SAVE_GRN', 'Save GRN' FROM app_pages WHERE page_code='GRN_ENTRY';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_GRN', 'Apply GRN filters' FROM app_pages WHERE page_code='GRN_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_GRN', 'Download GRN CSV' FROM app_pages WHERE page_code='GRN_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'VIEW_GRN_ITEMS', 'Show / hide GRN items' FROM app_pages WHERE page_code='GRN_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PRINT_GRN', 'Print GRN' FROM app_pages WHERE page_code='GRN_REPORT';

SELECT p.page_name, p.page_url, b.button_code, b.button_name
FROM app_pages p LEFT JOIN app_page_buttons b ON b.page_id=p.page_id
WHERE p.page_code IN ('APPROVE_PURCHASE_ORDERS','GRN_ENTRY','GRN_REPORT')
ORDER BY p.page_name, b.button_code;
