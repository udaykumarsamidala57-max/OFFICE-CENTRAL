-- Run after page_catalog_schema.sql on the inventory database.
USE inventory;

INSERT IGNORE INTO app_pages (page_code, page_name, page_url) VALUES
    ('CREATE_INDENT', 'Create Indent', '/IndentServlet'),
    ('INDENT_REPORT', 'Indent Report', '/IndentlistServlet'),
    ('INDENT_PRINT', 'Indent Print', '/IndentPrint'),
    ('INDENT_APPROVAL', 'Approve Indent', '/AIndentListServlet'),
    ('PAGE_CATALOG', 'Page & Button Setup', '/admin/page-catalog'),
    ('PAGE_ACCESS', 'Page Access', '/admin/page-access'),
    ('CREATE_PURCHASE_ORDER', 'Create Purchase Order', '/purchase-orders/create'),
    ('APPROVE_PURCHASE_ORDERS', 'Approve Purchase Orders', '/purchase-orders/approvals'),
    ('PURCHASE_ORDER_REPORT', 'Purchase Order Report', '/purchase-orders/report'),
    ('GRN_ENTRY', 'GRN Entry', '/grn/entry'),
    ('GRN_REPORT', 'GRN Report', '/grn/report'),
    ('ISSUE_STOCK', 'Issue Stock', '/issues/entry'),
    ('ISSUE_REPORT', 'Stock Issue Report', '/issues/report'),
    ('STOCK_ON_HAND', 'Stock on Hand', '/stock'),
    ('STOCK_REPORT', 'Stock Summary Report', '/stock/report');

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'ADD_ITEM', '+ Add Item' FROM app_pages WHERE page_code='CREATE_INDENT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'REMOVE_ITEM', 'Remove Item' FROM app_pages WHERE page_code='CREATE_INDENT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SAVE_INDENT', 'Save Indent' FROM app_pages WHERE page_code='CREATE_INDENT';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_REPORT', 'Apply' FROM app_pages WHERE page_code='INDENT_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'RESET_FILTER', 'Reset' FROM app_pages WHERE page_code='INDENT_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_CSV', 'Download CSV' FROM app_pages WHERE page_code='INDENT_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PRINT_REPORT', 'Print Report' FROM app_pages WHERE page_code='INDENT_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'VIEW_PRINT', 'View / Print' FROM app_pages WHERE page_code='INDENT_REPORT';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PRINT_INDENT', 'Print Indent' FROM app_pages WHERE page_code='INDENT_PRINT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'RETURN_TO_REPORT', 'Back to Report' FROM app_pages WHERE page_code='INDENT_PRINT';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'APPROVE_L1', 'Approve L1' FROM app_pages WHERE page_code='INDENT_APPROVAL';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'CONFIRM_L2', 'Confirm L2' FROM app_pages WHERE page_code='INDENT_APPROVAL';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'CREATE_PAGE', 'Create Page' FROM app_pages WHERE page_code='PAGE_CATALOG';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EDIT_PAGE', 'Edit Page' FROM app_pages WHERE page_code='PAGE_CATALOG';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'DELETE_PAGE', 'Delete Page' FROM app_pages WHERE page_code='PAGE_CATALOG';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'ADD_BUTTON', 'Add Button' FROM app_pages WHERE page_code='PAGE_CATALOG';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EDIT_BUTTON', 'Edit Button' FROM app_pages WHERE page_code='PAGE_CATALOG';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'DELETE_BUTTON', 'Delete Button' FROM app_pages WHERE page_code='PAGE_CATALOG';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SAVE_ACCESS', 'Save Access' FROM app_pages WHERE page_code='PAGE_ACCESS';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PREPARE_PO', 'Continue to PO details' FROM app_pages WHERE page_code='CREATE_PURCHASE_ORDER';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SAVE_PO', 'Save and submit PO' FROM app_pages WHERE page_code='CREATE_PURCHASE_ORDER';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_PO_APPROVALS', 'Search purchase orders' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_PO_APPROVALS', 'Download Excel Report' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'VIEW_PO', 'View / print purchase order' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SHOW_PO_ITEMS', 'Show / hide PO items' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'DELETE_PO', 'Delete pending PO' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'APPROVE_PO', 'Approve purchase order' FROM app_pages WHERE page_code='APPROVE_PURCHASE_ORDERS';
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

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PROCESS_ISSUE', 'Post stock issue' FROM app_pages WHERE page_code='ISSUE_STOCK';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_ISSUE_REPORT', 'Filter stock issue report' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_ISSUE_REPORT', 'Export stock issue report' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PRINT_ISSUE_VOUCHER', 'Print issue voucher' FROM app_pages WHERE page_code='ISSUE_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_STOCK', 'Filter stock on hand' FROM app_pages WHERE page_code='STOCK_ON_HAND';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_STOCK', 'Export stock on hand' FROM app_pages WHERE page_code='STOCK_ON_HAND';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_STOCK_REPORT', 'Filter stock summary report' FROM app_pages WHERE page_code='STOCK_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_STOCK_REPORT', 'Export stock summary report' FROM app_pages WHERE page_code='STOCK_REPORT';

INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'FILTER_PO', 'Apply report filters' FROM app_pages WHERE page_code='PURCHASE_ORDER_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'EXPORT_PO_REPORT', 'Export PO report CSV' FROM app_pages WHERE page_code='PURCHASE_ORDER_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'PRINT_PO', 'View / print purchase order' FROM app_pages WHERE page_code='PURCHASE_ORDER_REPORT';
INSERT IGNORE INTO app_page_buttons (page_id, button_code, button_name)
SELECT page_id, 'SHOW_PO_ITEMS', 'Show / hide PO items' FROM app_pages WHERE page_code='PURCHASE_ORDER_REPORT';
