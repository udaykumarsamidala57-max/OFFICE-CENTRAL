(() => {
    const vendor = document.getElementById('vendorSelect');
    const form = document.getElementById('purchaseOrderForm');
    const money = value => Math.round((value + Number.EPSILON) * 100) / 100;
    const read = (row, selector) => Number.parseFloat(row.querySelector(selector)?.value || '0') || 0;

    if (vendor) {
        vendor.addEventListener('change', () => {
            const option = vendor.selectedOptions[0];
            document.getElementById('vendorGstin').value = option?.dataset.gstin || '';
            document.getElementById('vendorAddress').value = option?.dataset.address || '';
        });
    }
    if (!form) return;

    function updateTotals() {
        let base = 0, discount = 0, gst = 0;
        form.querySelectorAll('.po-line').forEach(row => {
            const amount = money(read(row, '.line-qty') * read(row, '.line-rate'));
            const lineDiscount = money(amount * read(row, '.line-discount') / 100);
            const taxable = money(amount - lineDiscount);
            const lineGst = money(taxable * read(row, '.line-gst') / 100);
            row.querySelector('.line-total').textContent = money(taxable + lineGst).toFixed(2);
            base += amount;
            discount += lineDiscount;
            gst += lineGst;
        });
        const serviceCharge = Number.parseFloat(document.getElementById('serviceCharge')?.value || '0') || 0;
        const serviceRate = Number.parseFloat(document.getElementById('serviceGst')?.value || '0') || 0;
        const serviceTotal = money(serviceCharge + money(serviceCharge * serviceRate / 100));
        gst = money(gst + money(serviceCharge * serviceRate / 100));
        document.getElementById('subtotal').textContent = money(base).toFixed(2);
        document.getElementById('discountTotal').textContent = money(discount).toFixed(2);
        document.getElementById('gstTotal').textContent = gst.toFixed(2);
        document.getElementById('serviceTotal').textContent = serviceTotal.toFixed(2);
        document.getElementById('grandTotal').textContent = String(Math.round(base - discount + gst));
    }

    const recalculateForField = event => {
        if (event.target.matches('.line-qty,.line-rate,.line-discount,.line-gst,#serviceCharge,#serviceGst')) {
            event.target.setCustomValidity('');
            updateTotals();
        }
    };
    form.addEventListener('input', recalculateForField);
    form.addEventListener('change', recalculateForField);

    form.addEventListener('submit', event => {
        const vendorSelect = form.querySelector('[name="vendorName"]');
        if (!vendorSelect.value) { event.preventDefault(); vendorSelect.focus(); return; }
        for (const row of form.querySelectorAll('.po-line')) {
            const qtyField = row.querySelector('.line-qty');
            const qty = Number.parseFloat(qtyField.value);
            if (!Number.isFinite(qty) || qty <= 0) {
                event.preventDefault();
                qtyField.setCustomValidity('Enter a quantity greater than zero.');
                qtyField.reportValidity();
                qtyField.focus();
                return;
            }
            qtyField.setCustomValidity('');
        }
    });

    updateTotals();
})();

