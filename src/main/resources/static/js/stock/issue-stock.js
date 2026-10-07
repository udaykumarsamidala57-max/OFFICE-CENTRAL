(() => {
  const csv = value => `"${String(value ?? '').replaceAll('"', '""')}"`;
  const exportTable = (id, fileName, skipLast = false) => {
    const table = document.getElementById(id);
    if (!table) return;
    const lines = [...table.rows].filter(row => row.offsetParent !== null || row.closest('.inv-table-scroll'))
      .map(row => [...row.cells].slice(0, skipLast ? -1 : undefined).map(cell => cell.innerText.replace(/\s+/g, ' ').trim()));
    const blob = new Blob(['\ufeff' + lines.map(line => line.map(csv).join(',')).join('\r\n')], {type: 'text/csv;charset=utf-8'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a'); link.href = url; link.download = fileName;
    document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url);
  };
  document.getElementById('exportIssueCsv')?.addEventListener('click', () => exportTable('issueReportTable', 'Stock_Issue_Report.csv', true));
  document.getElementById('exportStockCsv')?.addEventListener('click', () => exportTable('stockTable', 'Stock_On_Hand.csv'));
  document.getElementById('exportStockReportCsv')?.addEventListener('click', () => exportTable('stockReportTable', 'Stock_Summary_Report.csv'));
})();
