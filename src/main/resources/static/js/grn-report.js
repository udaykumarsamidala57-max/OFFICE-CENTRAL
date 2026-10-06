/**
 * Goods Received Note (GRN) Report Interactivity
 * Handles expandable item panels, batch expand/collapse, CSV exports,
 * and automatic print triggering for A4 printable views.
 */
document.addEventListener('DOMContentLoaded', () => {

  // ==========================================
  // 1. EXPAND / COLLAPSE RECORD ITEM PANELS
  // ==========================================
  const toggleButtons = document.querySelectorAll('.grn-toggle');
  
  toggleButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      if (!targetId) return;

      const panel = document.getElementById(targetId);
      if (panel) {
        const isHidden = panel.hasAttribute('hidden');
        if (isHidden) {
          panel.removeAttribute('hidden');
          btn.classList.add('active');
        } else {
          panel.setAttribute('hidden', '');
          btn.classList.remove('active');
        }
      }
    });
  });

  // Expand All Records
  const expandAllBtn = document.getElementById('expandAllGrn');
  if (expandAllBtn) {
    expandAllBtn.addEventListener('click', () => {
      document.querySelectorAll('.grn-item-panel').forEach((panel) => {
        panel.removeAttribute('hidden');
      });
      toggleButtons.forEach((btn) => btn.classList.add('active'));
    });
  }

  // Collapse All Records
  const collapseAllBtn = document.getElementById('collapseAllGrn');
  if (collapseAllBtn) {
    collapseAllBtn.addEventListener('click', () => {
      document.querySelectorAll('.grn-item-panel').forEach((panel) => {
        panel.setAttribute('hidden', '');
      });
      toggleButtons.forEach((btn) => btn.classList.remove('active'));
    });
  }

  // ==========================================
  // 2. CSV EXPORT FUNCTIONALITY
  // ==========================================
  const exportCsvBtn = document.getElementById('exportGrnCsv');
  if (exportCsvBtn) {
    exportCsvBtn.addEventListener('click', () => {
      const records = document.querySelectorAll('.grn-record-card');
      if (!records.length) {
        alert('No GRN records available to export.');
        return;
      }

      const csvRows = [];
      // Header row
      csvRows.push([
        'GRN No',
        'GRN Date',
        'PO Number',
        'Vendor Name',
        'Invoice No',
        'Invoice Date',
        'Received By',
        'Remarks',
        'Item Description',
        'Qty Ordered',
        'Qty Received',
        'Qty Accepted',
        'Qty Rejected',
        'Item Remarks'
      ].map(formatCsvValue).join(','));

      // Iterate records and line items
      records.forEach((card) => {
        const grnNo = card.getAttribute('data-grn') || '';
        const grnDate = card.getAttribute('data-date') || '';
        const poNo = card.getAttribute('data-po') || '';
        const vendor = card.getAttribute('data-vendor') || '';
        const invoiceNo = card.getAttribute('data-invoice') || '';
        const invoiceDate = card.getAttribute('data-invoice-date') || '';
        const receivedBy = card.getAttribute('data-received-by') || '';
        const grnRemarks = card.getAttribute('data-remarks') || '';

        const itemRows = card.querySelectorAll('tbody tr');
        let hasItems = false;

        itemRows.forEach((row) => {
          // Skip empty placeholder row
          if (row.querySelector('.grn-empty')) return;

          hasItems = true;
          const cells = row.querySelectorAll('td');
          const description = cells[0] ? cells[0].innerText.trim() : '';
          const qtyOrdered = cells[1] ? cells[1].innerText.trim() : '0.00';
          const qtyReceived = cells[2] ? cells[2].innerText.trim() : '0.00';
          const qtyAccepted = cells[3] ? cells[3].innerText.trim() : '0.00';
          const qtyRejected = cells[4] ? cells[4].innerText.trim() : '0.00';
          const itemRemarks = cells[5] ? cells[5].innerText.trim() : '';

          csvRows.push([
            grnNo,
            grnDate,
            poNo,
            vendor,
            invoiceNo,
            invoiceDate,
            receivedBy,
            grnRemarks,
            description,
            qtyOrdered,
            qtyReceived,
            qtyAccepted,
            qtyRejected,
            itemRemarks
          ].map(formatCsvValue).join(','));
        });

        // If a record has no line items, export summary row
        if (!hasItems) {
          csvRows.push([
            grnNo,
            grnDate,
            poNo,
            vendor,
            invoiceNo,
            invoiceDate,
            receivedBy,
            grnRemarks,
            'No items',
            '0.00',
            '0.00',
            '0.00',
            '0.00',
            ''
          ].map(formatCsvValue).join(','));
        }
      });

      // Trigger Download
      const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + csvRows.join('\n');
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement('a');
      const timestamp = new Date().toISOString().slice(0, 10);
      
      link.setAttribute('href', encodedUri);
      link.setAttribute('download', `GRN_Report_${timestamp}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    });
  }

  // Utility to escape quotes and commas for CSV compatibility
  function formatCsvValue(val) {
    if (val === null || val === undefined) return '""';
    const stringVal = String(val).replace(/"/g, '""');
    return `"${stringVal}"`;
  }

  // ==========================================
  // 3. AUTO-PRINT TRIGGER FOR PRINT MODE
  // ==========================================
  const printSheet = document.getElementById('printGrn');
  if (printSheet && window.location.search.includes('printNumber=')) {
    // Slight delay to ensure fonts and headers render cleanly before print prompt
    setTimeout(() => {
      window.print();
    }, 400);
  }
});