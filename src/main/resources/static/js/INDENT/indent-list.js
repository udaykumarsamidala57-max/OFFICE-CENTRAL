(() => {
    const table = document.getElementById('dataTable');
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

    function applyFilters() {
        const term = keyword.value.trim().toLocaleLowerCase();
        const dept = department.value.trim().toLocaleLowerCase();
        let shown = 0;
        rows.forEach(row => {
            const searchableText = (row.dataset.search || '') + ' ' + row.innerText;
            const textMatch = !term || searchableText.toLocaleLowerCase().includes(term);
            const rowDept = (row.dataset.department || '').trim().toLocaleLowerCase();
            const deptMatch = !dept || rowDept === dept;
            const rowDate = (row.dataset.reportDate || '').trim();
            const dateMatch = (!fromDate.value || rowDate >= fromDate.value)
                && (!toDate.value || rowDate <= toDate.value);
            const visible = textMatch && deptMatch && dateMatch;
            row.hidden = !visible;
            if (visible) shown++;
        });
        groups.forEach(group => {
            group.header.hidden = !group.rows.some(row => !row.hidden);
        });
        visibleCount.textContent = shown;
        noMatches.hidden = shown !== 0 || rows.length === 0;
    }

    const bindButton = (id, action) => {
        const button = document.getElementById(id);
        if (button) button.addEventListener('click', action);
    };
    bindButton('filterButton', applyFilters);
    [keyword, fromDate, toDate, department].forEach(control => {
        control.addEventListener(control === keyword ? 'input' : 'change', applyFilters);
    });
    bindButton('resetButton', () => {
        keyword.value = '';
        fromDate.value = '';
        toDate.value = '';
        department.value = '';
        applyFilters();
    });
    bindButton('printButton', () => window.print());
    bindButton('csvButton', () => {
        const visibleRows = [...tbody.querySelectorAll('.data-row')].filter(row => !row.hidden);
        const data = [
            [...table.tHead.querySelector('.column-header').cells].map(cell => cell.innerText.trim()),
            ...visibleRows.map(row => [...row.cells].map(cell => cell.innerText.replace(/\s+/g, ' ').trim()))
        ];
        const csv = data.map(row => row.map(value => '"' + value.replace(/"/g, '""') + '"').join(',')).join('\r\n');
        const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'Indent_Report.csv';
        link.click();
        URL.revokeObjectURL(link.href);
    });

    const sortableHeaders = table.tHead.querySelectorAll('.column-header th[data-sort]');
    sortableHeaders.forEach((header, columnIndex) => {
        header.addEventListener('click', () => {
            const ascending = header.getAttribute('aria-sort') !== 'ascending';
            sortableHeaders.forEach(th => th.removeAttribute('aria-sort'));
            header.setAttribute('aria-sort', ascending ? 'ascending' : 'descending');
            const type = header.dataset.sort;
            const compareRows = (a, b) => {
                const left = a.cells[columnIndex].innerText.trim();
                const right = b.cells[columnIndex].innerText.trim();
                let result;
                if (type === 'number') result = Number(left) - Number(right);
                else if (type === 'date') result = left.localeCompare(right);
                else result = left.localeCompare(right, undefined, { numeric: true, sensitivity: 'base' });
                return ascending ? result : -result;
            };
            groups.forEach((group, index) => {
                const nextHeader = groups[index + 1]?.header || noMatches;
                group.rows.sort(compareRows).forEach(row => tbody.insertBefore(row, nextHeader));
                markGroupRows(group);
            });
            applyFilters();
        });
    });
    applyFilters();
})();
