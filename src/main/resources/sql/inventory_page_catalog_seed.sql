-- Run once on the inventory authentication database after page_catalog_schema.sql.
-- Adds the Issue and Stock pages and their page/button catalog entries; it does not grant access.
USE inventory;

INSERT IGNORE INTO app_pages(page_code,page_name,page_url) VALUES
 ('ISSUE_STOCK','Issue Stock','/issues/entry'),
 ('ISSUE_REPORT','Stock Issue Report','/issues/report'),
 ('STOCK_ON_HAND','Stock on Hand','/stock'),
 ('STOCK_REPORT','Stock Summary Report','/stock/report');

INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'PROCESS_ISSUE','Post stock issue' FROM app_pages WHERE page_code='ISSUE_STOCK';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'FILTER_ISSUE_REPORT','Filter stock issue report' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'EXPORT_ISSUE_REPORT','Export stock issue report' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'PRINT_ISSUE_VOUCHER','Print issue voucher' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'FILTER_STOCK','Filter stock on hand' FROM app_pages WHERE page_code='STOCK_ON_HAND';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'EXPORT_STOCK','Export stock on hand' FROM app_pages WHERE page_code='STOCK_ON_HAND';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'FILTER_STOCK_REPORT','Filter stock summary report' FROM app_pages WHERE page_code='STOCK_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'EXPORT_STOCK_REPORT','Export stock summary report' FROM app_pages WHERE page_code='STOCK_REPORT';

SELECT p.page_name,p.page_url,b.button_code,b.button_name
FROM app_pages p LEFT JOIN app_page_buttons b ON b.page_id=p.page_id
WHERE p.page_code IN ('ISSUE_STOCK','ISSUE_REPORT','STOCK_ON_HAND','STOCK_REPORT')
ORDER BY p.page_name,b.button_code;
