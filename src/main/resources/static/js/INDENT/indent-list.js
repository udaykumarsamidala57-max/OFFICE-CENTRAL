(() => {
    const table = document.getElementById('dataTable');
    if (!table) return;

    const rows = [...document.querySelectorAll('.data-row')];
    const keyword = document.getElementById('keywordSearch');
    const fromDate = document.getElementById('fromDate');
    const toDate = document.getElementById('toDate');
    const department = document.getElementById('deptFilter');
    const noMatches = document.getElementById('noMatches');
    const visibleCount = document.getElementById('visibleCount');
    const tbody = table.tBodies[0];
    const groups = [];
    let currentGroup = null;

    // Grouping setup
    [...tbody.children].forEach(row => {
        if (row.classList.contains('indent-group-header')) {
            currentGroup = { header: row, rows: [] };
            groups.push(currentGroup);
        } else if (row.classList.contains('data-row') && currentGroup) {
            currentGroup.rows.push(row);
        }
    });

    function markGroupRows(group) {
        group.rows.forEach((row, index) => {
            row.classList.toggle('group-first', index === 0);
            row.classList.toggle('group-last', index === group.rows.length - 1);
        });
    }
    groups.forEach(markGroupRows);

    // Filtering logic
    function applyFilters() {
        const term = keyword ? keyword.value.trim().toLocaleLowerCase() : '';
        const dept = department ? department.value.trim().toLocaleLowerCase() : '';
        let shown = 0;

        rows.forEach(row => {
            const searchableText = (row.dataset.search || '') + ' ' + row.innerText;
            const textMatch = !term || searchableText.toLocaleLowerCase().includes(term);
            const rowDept = (row.dataset.department || '').trim().toLocaleLowerCase();
            const deptMatch = !dept || rowDept === dept;
            const rowDate = (row.dataset.reportDate || '').trim();
            const dateMatch = (!fromDate || !fromDate.value || rowDate >= fromDate.value)
                && (!toDate || !toDate.value || rowDate <= toDate.value);

            const visible = textMatch && deptMatch && dateMatch;
            row.hidden = !visible;
            if (visible) shown++;
        });

        groups.forEach(group => {
            group.header.hidden = !group.rows.some(row => !row.hidden);
        });

        if (visibleCount) visibleCount.textContent = shown;
        if (noMatches) noMatches.hidden = shown !== 0 || rows.length === 0;
    }

    const bindButton = (id, action) => {
        const button = document.getElementById(id);
        if (button) button.addEventListener('click', action);
    };

    bindButton('filterButton', applyFilters);

    [keyword, fromDate, toDate, department].forEach(control => {
        if (control) {
            control.addEventListener(control === keyword ? 'input' : 'change', applyFilters);
        }
    });

    bindButton('resetButton', () => {
        if (keyword) keyword.value = '';
        if (fromDate) fromDate.value = '';
        if (toDate) toDate.value = '';
        if (department) department.value = '';
        applyFilters();
    });

    bindButton('printButton', () => window.print());

    // CSV Exporting
    bindButton('csvButton', () => {
        const headerRow = table.tHead.querySelector('tr');
        if (!headerRow) return;

        const headers = [...headerRow.cells].map(cell => cell.innerText.trim());
        const exportData = [headers];

        groups.forEach(group => {
            const visibleRows = group.rows.filter(row => !row.hidden);
            if (visibleRows.length > 0) {
                // Include Group Header info in CSV output if visible
                const groupTitle = group.header.innerText.replace(/\s+/g, ' ').trim();
                exportData.push([`--- ${groupTitle} ---`]);

                visibleRows.forEach(row => {
                    const rowCells = [...row.cells].map(cell => cell.innerText.replace(/\s+/g, ' ').trim());
                    exportData.push(rowCells);
                });
            }
        });

        const csv = exportData
            .map(row => row.map(val => '"' + String(val).replace(/"/g, '""') + '"').join(','))
            .join('\r\n');

        const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'Indent_Report.csv';
        link.click();
        URL.revokeObjectURL(link.href);
    });

    // In-Group Table Sorting
    const sortableHeaders = table.tHead.querySelectorAll('th[data-sort]');
    sortableHeaders.forEach((header) => {
        header.addEventListener('click', () => {
            // Find correct cell index dynamically
            const columnIndex = header.cellIndex;
            const ascending = header.getAttribute('aria-sort') !== 'ascending';

            sortableHeaders.forEach(th => th.removeAttribute('aria-sort'));
            header.setAttribute('aria-sort', ascending ? 'ascending' : 'descending');

            const type = header.dataset.sort;
            const compareRows = (a, b) => {
                const left = (a.cells[columnIndex]?.innerText || '').trim();
                const right = (b.cells[columnIndex]?.innerText || '').trim();

                let result;
                if (type === 'number') {
                    const numA = parseFloat(left.replace(/[^0-9.-]+/g, '')) || 0;
                    const numB = parseFloat(right.replace(/[^0-9.-]+/g, '')) || 0;
                    result = numA - numB;
                } else if (type === 'date') {
                    result = left.localeCompare(right);
                } else {
                    result = left.localeCompare(right, undefined, { numeric: true, sensitivity: 'base' });
                }
                return ascending ? result : -result;
            };

            groups.forEach((group) => {
                group.rows.sort(compareRows);
                // Re-append rows directly after group header in correct order
                let referenceNode = group.header;
                group.rows.forEach(row => {
                    referenceNode.after(row);
                    referenceNode = row;
                });
                markGroupRows(group);
            });

            applyFilters();
        });
    });

    applyFilters();
})();