-- Run this on the SRS authentication database (inventory).
-- Registers the Print button shown on the Approve Purchase Orders page.
USE inventory;

INSERT IGNORE INTO app_pages (page_code, page_name, page_url)
VALUES ('APPROVE_PURCHASE_ORDERS', 'Approve Purchase Orders', '/purchase-orders/approvals');

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'VIEW_PO', 'View / print purchase order'
FROM app_pages
WHERE page_code = 'APPROVE_PURCHASE_ORDERS'
  AND page_url = '/purchase-orders/approvals';

-- Confirm the page and button are registered in the catalog.
SELECT p.page_code, p.page_name, p.page_url, b.button_code, b.button_name
FROM app_pages p
JOIN app_page_buttons b ON b.page_id = p.page_id
WHERE p.page_code = 'APPROVE_PURCHASE_ORDERS'
  AND b.button_code = 'VIEW_PO';
