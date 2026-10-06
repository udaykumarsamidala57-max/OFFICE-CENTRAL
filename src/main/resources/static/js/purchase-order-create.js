(() => {
    const vendor = document.getElementById('vendorSelect');
    const form = document.getElementById('purchaseOrderForm');
    const money = value => (Math.round((value + Number.EPSILON) * 100) / 100);
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
            const lineTotal = money(taxable + lineGst);
            row.querySelector('.line-total').textContent = lineTotal.toFixed(2);
            base += amount;
            discount += lineDiscount;
            gst += lineGst;
        });
        const serviceCharge = Number.parseFloat(document.getElementById('serviceCharge')?.value || '0') || 0;
        const serviceRate = Number.parseFloat(document.getElementById('serviceGst')?.value || '0') || 0;
        const serviceGst = money(serviceCharge * serviceRate / 100);
        const serviceTotal = money(serviceCharge + serviceGst);
        gst = money(gst + serviceGst);
        const grand = Math.round(base - discount + gst);
        document.getElementById('subtotal').textContent = money(base).toFixed(2);
        document.getElementById('discountTotal').textContent = money(discount).toFixed(2);
        document.getElementById('gstTotal').textContent = gst.toFixed(2);
        document.getElementById('serviceTotal').textContent = serviceTotal.toFixed(2);
        document.getElementById('grandTotal').textContent = String(grand);
    }

    form.addEventListener('input', event => {
        if (event.target.matches('.po-number-input,#serviceCharge,#serviceGst')) updateTotals();
    });
    form.addEventListener('submit', event => {
        const vendorSelect = form.querySelector('[name="vendorName"]');
        if (!vendorSelect.value) { event.preventDefault(); vendorSelect.focus(); return; }
        for (const row of form.querySelectorAll('.po-line')) {
            const qty = read(row, '.line-qty');
            const max = Number.parseFloat(row.querySelector('.line-qty').max);
            if (!(qty > 0) || qty > max) {
                event.preventDefault();
                row.querySelector('.line-qty').focus();
                row.querySelector('.line-qty').setCustomValidity('Quantity must be positive and cannot exceed the approved indent quantity.');
                row.querySelector('.line-qty').reportValidity();
                return;
            }
            row.querySelector('.line-qty').setCustomValidity('');
        }
    });
    updateTotals();
})();
