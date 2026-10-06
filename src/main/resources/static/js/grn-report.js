(() => {
  const itemPanels = [...document.querySelectorAll('.grn-item-panel')];
  const setExpanded = (panel, expanded) => {
    panel.hidden = !expanded;
    const button = document.querySelector(`.grn-toggle[data-target="${panel.id}"]`);
    if (button) button.setAttribute('aria-expanded', String(expanded));
  };

  document.querySelectorAll('.grn-toggle').forEach(button => {
    button.setAttribute('aria-expanded', 'false');
    button.addEventListener('click', () => {
      const panel = document.getElementById(button.dataset.target);
      if (panel) setExpanded(panel, panel.hidden);
    });
  });
  document.getElementById('expandAllGrn')?.addEventListener('click', () => itemPanels.forEach(panel => setExpanded(panel, true)));
  document.getElementById('collapseAllGrn')?.addEventListener('click', () => itemPanels.forEach(panel => setExpanded(panel, false)));

  const csvCell = value => `"${String(value ?? '').replaceAll('"', '""')}"`;
  document.getElementById('exportGrnCsv')?.addEventListener('click', () => {
    const rows = [['GRN No', 'GRN Date', 'PO Number', 'Vendor', 'Invoice No', 'Invoice Date', 'Received By', 'GRN Remarks',
      'Item Description', 'Qty Ordered', 'Qty Received', 'Qty Accepted', 'Qty Rejected', 'Item Remarks']];
    document.querySelectorAll('.grn-record-card').forEach(card => {
      const master = [card.dataset.grn, card.dataset.date, card.dataset.po, card.dataset.vendor,
        card.dataset.invoice, card.dataset.invoiceDate, card.dataset.receivedBy, card.dataset.remarks];
      const items = [...card.querySelectorAll('.grn-report-table tbody tr')].filter(row => row.querySelector('td.grn-item-name'));
      if (!items.length) rows.push([...master, '', '', '', '', '', '']);
      items.forEach(row => rows.push([...master, ...[...row.cells].map(cell => cell.textContent.trim())]));
    });
    const blob = new Blob(['\ufeff' + rows.map(row => row.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `GRN_export_${new Date().toISOString().slice(0, 10).replaceAll('-', '')}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  });
})();
