(() => {
    const baseUrl = document.body.dataset.approvalBase || '/AIndentListServlet';
    const message = document.getElementById('approvalMessage');

    function showError(text) {
        message.textContent = text || 'The approval could not be completed.';
        message.hidden = false;
        message.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
    function clearError() { message.hidden = true; message.textContent = ''; }
    async function post(path, fields) {
        const body = new URLSearchParams(fields);
        const response = await fetch(baseUrl + path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || 'The approval could not be completed.');
        return result;
    }

    document.querySelectorAll('.approve-l1').forEach(button => button.addEventListener('click', async () => {
        clearError();
        button.disabled = true;
        try {
            const result = await post('/approve-level-one', { id: button.dataset.id });
            const cell = button.closest('.ic-status');
            button.remove();
            const badge = document.createElement('span');
            badge.className = 'approved-badge';
            badge.textContent = '✓ Approved';
            cell.prepend(badge);
            const date = document.createElement('small');
            date.className = 'approval-date';
            date.textContent = result.approvedDate;
            cell.append(date);
            const approvedBy = document.querySelector('.approved-by[data-id="' + button.dataset.id + '"]');
            if (approvedBy) {
                const name = approvedBy.querySelector('span');
                if (name) name.textContent = result.approvedBy;
                else approvedBy.textContent = result.approvedBy;
            }
        } catch (error) {
            button.disabled = false;
            showError(error.message);
        }
    }));

    document.querySelectorAll('.confirm-final').forEach(button => button.addEventListener('click', async () => {
        clearError();
        const select = document.querySelector('.next-step[data-id="' + button.dataset.id + '"]');
        if (!select || !select.value) { showError('Select the next step first.'); return; }
        button.disabled = true;
        try {
            const result = await post('/approve-final', { id: button.dataset.id, indentnext: select.value });
            const row = button.closest('.approval-item');
            if (['Issue', 'PO', 'Cancelled'].includes(result.nextStep)) {
                row.remove();
                const card = button.closest('.approval-group');
                if (!card.querySelector('.approval-item')) card.remove();
            } else {
                const status = row.querySelector('.l2-status');
                status.textContent = result.nextStep;
                status.classList.remove('approved');
                const l2Cell = status.closest('.l2-status-cell');
                let approvedBy = l2Cell.querySelector('.approved-by-name');
                if (!approvedBy) {
                    approvedBy = document.createElement('small');
                    approvedBy.className = 'approved-by-name';
                    l2Cell.append(approvedBy);
                }
                approvedBy.textContent = 'By: ' + result.approvedBy;
                let approvalDate = l2Cell.querySelector('.approval-date');
                if (!approvalDate) {
                    approvalDate = document.createElement('small');
                    approvalDate.className = 'approval-date';
                    l2Cell.append(approvalDate);
                }
                approvalDate.textContent = result.date;
                button.closest('.final-action').remove();
            }
            const count = document.querySelectorAll('.approval-group').length;
            const countValue = document.querySelector('.group-count strong');
            if (countValue) countValue.textContent = count;
            if (!count) {
                const empty = document.createElement('div');
                empty.className = 'empty-approvals';
                empty.textContent = 'No open indents require approval.';
                document.querySelector('.approval-page').append(empty);
            }
        } catch (error) {
            button.disabled = false;
            showError(error.message);
        }
    }));
})();
