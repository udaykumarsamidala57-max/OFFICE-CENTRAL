(() => {
    const dateFrom = document.getElementById('filterFromDate');
    const dateTo = document.getElementById('filterToDate');
    const table = document.querySelector('.po-legacy-table');
    const emptyMessage = document.getElementById('dateFilterEmpty');

    function toggleItems(id) {
        const row = document.getElementById(`row-nested-${id}`);
        const button = document.getElementById(`btn-${id}`);
        if (!row || !button) return;
        const open = row.style.display === 'none' || row.style.display === '';
        row.style.display = open ? '' : 'none';
        button.textContent = open ? 'Hide Items' : 'Show Items';
    }

    function parseDate(value) {
        if (!value) return null;
        const clean = value.trim();
        let match = clean.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (match) return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
        match = clean.match(/^(\d{2})-(\d{2})-(\d{4})$/);
        if (match) return new Date(Number(match[3]), Number(match[2]) - 1, Number(match[1]));
        const parsed = new Date(clean);
        return Number.isNaN(parsed.getTime()) ? null : new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
    }

    function filterByDates() {
        if (!dateFrom?.value || !dateTo?.value) {
            window.alert('Please select both From and To dates.');
            return;
        }
        const from = parseDate(dateFrom.value);
        const to = parseDate(dateTo.value);
        if (!from || !to || from > to) {
            window.alert('From Date cannot be greater than To Date.');
            return;
        }
        let visible = 0;
        table?.querySelectorAll('.po-master-row').forEach(row => {
            const date = parseDate(row.dataset.rawDate);
            const show = !date || (date >= from && date <= to);
            row.style.display = show ? '' : 'none';
            const nested = document.getElementById(`row-nested-${row.dataset.nestedId}`);
            const button = document.getElementById(`btn-${row.dataset.nestedId}`);
            if (!show && nested) nested.style.display = 'none';
            if (!show && button) button.textContent = 'Show Items';
            if (show) visible++;
        });
        if (emptyMessage) emptyMessage.hidden = visible !== 0;
    }

    function clearDateFilters() {
        if (dateFrom) dateFrom.value = '';
        if (dateTo) dateTo.value = '';
        table?.querySelectorAll('.po-master-row').forEach(row => { row.style.display = ''; });
        if (emptyMessage) emptyMessage.hidden = true;
    }

    function csvCell(value) { return `"${String(value ?? '').replaceAll('"', '""').replace(/\s+/g, ' ').trim()}"`; }
    function exportToExcel() {
        const rows = document.querySelectorAll('.po-master-row');
        if (!rows.length) { window.alert('No records found.'); return; }
        const approvals = table?.classList.contains('po-approval-table');
        const output = ['PURCHASE ORDERS EXPORT REPORT', `Generated On,${new Date().toLocaleString()}`, '',
            approvals
                ? 'PO Number,PO Date,Vendor Name,Total Amount,Approval Status,Item ID,Item Description,PO Qty,Received Qty,Balance Qty,Rate,Discount %,GST %'
                : 'PO Number,PO Date,Vendor Name,Status,Approval Status,Total Amount,Item ID,Item Description,Qty,Rate,Amount,Discount,GST,Net Amount'];
        let count = 0;
        rows.forEach(row => {
            if (row.style.display === 'none') return;
            const value = label => row.querySelector(`[data-label="${label}"]`)?.innerText || '';
            const nestedId = row.dataset.nestedId;
            const items = document.querySelectorAll(`#items-${CSS.escape(nestedId)} .po-items-table tbody tr`);
            if (!items.length) {
                output.push([value('PO Number'), value('PO Date'), value('Vendor Name'), value('Status'), value('Approval Status'), value('Total Amount')].map(csvCell).join(','));
            } else {
                items.forEach(item => {
                    const cells = [...item.querySelectorAll('td')].map(td => td.innerText);
                    if (cells.length < 2 || item.querySelector('.po-empty')) return;
                    const detail = approvals
                        ? [value('PO Number'), value('PO Date'), value('Vendor Name'), value('Total Amount'), value('Approval Status'), cells[0], cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7]]
                        : [value('PO Number'), value('PO Date'), value('Vendor Name'), value('Status'), value('Approval Status'), value('Total Amount'), item.dataset.itemId || '', cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7]];
                    output.push(detail.map(csvCell).join(','));
                });
            }
            count++;
        });
        if (!count) { window.alert('No records found.'); return; }
        const blob = new Blob(['\uFEFF' + output.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Purchase_Orders_Report_${new Date().toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url);
    }

    document.querySelectorAll('.po-expand-button').forEach(button => {
        button.addEventListener('click', () => toggleItems(button.dataset.rowId));
    });
    document.getElementById('applyDateFilter')?.addEventListener('click', filterByDates);
    document.getElementById('clearDateFilter')?.addEventListener('click', clearDateFilters);
    document.getElementById('exportApprovalExcel')?.addEventListener('click', exportToExcel);
    document.getElementById('exportPurchaseOrderReport')?.addEventListener('click', exportToExcel);
})();
