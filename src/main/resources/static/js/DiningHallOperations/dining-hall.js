window.toggleModal = function (modalId, isOpen) {
  const modalTarget = document.getElementById(modalId);
  if (modalTarget) {
    if (isOpen) {
      modalTarget.classList.add('is-open');
      document.body.classList.add('modal-open');
    } else {
      modalTarget.classList.remove('is-open');
      document.body.classList.remove('modal-open');
    }
  }
};

(() => {
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
      document.querySelectorAll('.custom-modal.is-open').forEach(modal => {
        if (window.toggleModal) {
          window.toggleModal(modal.id, false);
        }
      });
    }
  });

  const data = window.diningData;
  const tableBody = document.querySelector('#diningItems tbody');
  const formatMoney = value => Number(value || 0).toLocaleString('en-IN', {minimumFractionDigits: 2, maximumFractionDigits: 2});

  if (data && tableBody) {
    const addRow = () => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td><select class="dh-category"><option value="">Category</option></select></td>
        <td><select class="dh-subcategory" disabled><option value="">Subcategory</option></select></td>
        <td><select class="dh-item" name="itemId" required disabled><option value="">Item</option></select></td>
        <td class="dh-uom" data-label="UOM">-</td><td class="dh-stock" data-label="Stock">-</td>
        <td><input class="dh-quantity" type="number" name="quantity" min="0.0001" step="0.0001" required disabled></td>
        <td><input type="text" name="remarks" maxlength="500" placeholder="Optional"></td>
        <td><button type="button" class="dh-button secondary dh-remove">Remove</button></td>`;
      tableBody.appendChild(tr);
      const category = tr.querySelector('.dh-category');
      const subcategory = tr.querySelector('.dh-subcategory');
      const item = tr.querySelector('.dh-item');
      const quantity = tr.querySelector('.dh-quantity');
      tr.querySelector('.dh-remove').hidden = !data.canRemove;
      [...new Set((data.categories || []).map(row => row.name).filter(Boolean))].forEach(name => category.add(new Option(name, name)));
      category.addEventListener('change', () => {
        subcategory.replaceChildren(new Option('Subcategory', ''));
        item.replaceChildren(new Option('Item', ''));
        const options = (data.subcategories || []).filter(row => row.category === category.value);
        [...new Set(options.map(row => row.name).filter(Boolean))].forEach(name => subcategory.add(new Option(name, name)));
        subcategory.disabled = !category.value;
        item.disabled = true; quantity.disabled = true;
        resetLine(tr);
      });
      subcategory.addEventListener('change', () => {
        item.replaceChildren(new Option('Item', ''));
        (data.items || []).filter(row => row.category === category.value && row.subcategory === subcategory.value)
          .forEach(row => { const opt = new Option(row.name, row.id); opt.dataset.stock = row.stock; opt.dataset.uom = row.uom || ''; item.add(opt); });
        item.disabled = !subcategory.value; quantity.disabled = true; resetLine(tr);
      });
      item.addEventListener('change', () => {
        const option = item.selectedOptions[0];
        if (!option || !option.value) { resetLine(tr); quantity.disabled = true; recalc(); return; }
        const stock = Number(option.dataset.stock || 0); tr.querySelector('.dh-uom').textContent = option.dataset.uom || '-';
        tr.querySelector('.dh-stock').textContent = formatMoney(stock); tr.querySelector('.dh-stock').classList.toggle('low', stock <= 0);
        quantity.max = stock; quantity.disabled = stock <= 0; quantity.value = '';
        recalc();
      });
      quantity.addEventListener('input', () => { const max = Number(quantity.max || 0); if (Number(quantity.value) > max) quantity.setCustomValidity(`Available stock is ${formatMoney(max)}.`); else quantity.setCustomValidity(''); recalc(); });
      tr.querySelector('.dh-remove').addEventListener('click', () => { tr.remove(); recalc(); });
    };
    const resetLine = tr => { tr.querySelector('.dh-uom').textContent='-';tr.querySelector('.dh-stock').textContent='-'; };
    const recalc = () => {};
    document.getElementById('addDiningItem')?.addEventListener('click', addRow);
    addRow();
  }

  const selectAll=document.getElementById('selectAllDiningEdit');
  selectAll?.addEventListener('click',()=>{const checks=[...document.querySelectorAll('.dh-edit-check')];const select=checks.some(c=>!c.checked);checks.forEach(c=>c.checked=select);});
  document.querySelectorAll('[data-check]').forEach(input=>input.addEventListener('input',()=>{const check=document.getElementById(input.dataset.check);if(check)check.checked=true;}));

  document.getElementById('exportDiningReport')?.addEventListener('click',()=>{
    const table=document.getElementById('diningReportTable');if(!table)return;
    const quote=value=>`"${String(value??'').replaceAll('"','""')}"`;
    const csv=[...table.rows].map(row=>[...row.cells].map(cell=>cell.innerText.trim())).map(row=>row.map(quote).join(',')).join('\r\n');
    const url=URL.createObjectURL(new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'}));const link=document.createElement('a');link.href=url;link.download='Dining_Hall_Consumption_Report.csv';link.click();URL.revokeObjectURL(url);
  });
})();
