-- Run on the inventory authentication database after page_catalog_schema.sql.
-- Registers the Dining Hall pages and controls; grant them to users/roles in Page Access.
USE inventory;

INSERT IGNORE INTO app_pages(page_code,page_name,page_url) VALUES
 ('DINING_CONSUMPTION_ENTRY','Dining Hall Consumption Entry','/dining-hall/consumption/entry'),
 ('DINING_DASHBOARD','Dining Hall Dashboard','/dining-hall/dashboard'),
 ('DINING_CONSUMPTION_REPORT','Dining Hall Consumption Report','/dining-hall/consumption/report'),
 ('DINING_CONSUMPTION_EDIT','Edit Dining Hall Consumption','/dining-hall/consumption/edit');

INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'ADD_DINING_ITEM','Add consumption item row' FROM app_pages WHERE page_code='DINING_CONSUMPTION_ENTRY';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'REMOVE_DINING_ITEM','Remove consumption item row' FROM app_pages WHERE page_code='DINING_CONSUMPTION_ENTRY';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'SAVE_DINING_CONSUMPTION','Save Dining Hall consumption' FROM app_pages WHERE page_code='DINING_CONSUMPTION_ENTRY';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'FILTER_DINING_DASHBOARD','Filter dining dashboard dates' FROM app_pages WHERE page_code='DINING_DASHBOARD';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'FILTER_DINING_REPORT','Filter Dining Hall consumption report' FROM app_pages WHERE page_code='DINING_CONSUMPTION_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'EXPORT_DINING_REPORT','Export Dining Hall consumption report' FROM app_pages WHERE page_code='DINING_CONSUMPTION_REPORT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'LOAD_DINING_EDIT','Load entries for editing' FROM app_pages WHERE page_code='DINING_CONSUMPTION_EDIT';
INSERT IGNORE INTO app_page_buttons(page_id,button_code,button_name)
 SELECT page_id,'SAVE_DINING_EDIT','Save selected consumption changes' FROM app_pages WHERE page_code='DINING_CONSUMPTION_EDIT';

SELECT p.page_name,p.page_url,b.button_code,b.button_name
FROM app_pages p LEFT JOIN app_page_buttons b ON b.page_id=p.page_id
WHERE p.page_code IN ('DINING_CONSUMPTION_ENTRY','DINING_DASHBOARD','DINING_CONSUMPTION_REPORT','DINING_CONSUMPTION_EDIT')
ORDER BY p.page_name,b.button_code;
