(() => {
    const subjectType = document.getElementById('subjectType');
    const userChoice = document.getElementById('userChoice');
    const roleChoice = document.getElementById('roleChoice');

    function syncSubjectFields() {
        const isRole = subjectType.value === 'ROLE';
        userChoice.hidden = isRole;
        roleChoice.hidden = !isRole;
        userChoice.querySelector('select').disabled = isRole;
        roleChoice.querySelector('select').disabled = !isRole;
    }
    subjectType.addEventListener('change', syncSubjectFields);
    syncSubjectFields();

    document.querySelectorAll('.permission-form').forEach(form => {
        const pageGrant = form.querySelector('.page-grant-checkbox');
        pageGrant.addEventListener('change', () => {
            form.querySelectorAll('input[name="buttonIds"]').forEach(button => {
                button.disabled = !pageGrant.checked;
                if (!pageGrant.checked) button.checked = false;
            });
        });
    });
})();
