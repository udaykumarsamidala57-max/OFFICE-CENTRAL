/**
 * Page & Button Catalog - Pure AJAX Module
 */

document.addEventListener('DOMContentLoaded', () => {
  const createForm = document.getElementById('createPageForm');
  if (createForm) {
    createForm.addEventListener('submit', createPage);
  }
});

/* --- UI HELPERS --- */
function toggleAccordion(id) {
  const drawer = document.getElementById(`drawer-${id}`);
  const chevron = document.getElementById(`chevron-${id}`);
  if (drawer) drawer.classList.toggle('hidden');
  if (chevron) chevron.classList.toggle('open');
}

function showAlert(message, isSuccess = true) {
  const alertBox = document.getElementById('catalogAlert');
  alertBox.className = `catalog-alert ${isSuccess ? 'success' : 'error'}`;
  alertBox.textContent = message;
  alertBox.classList.remove('hidden');
  setTimeout(() => alertBox.classList.add('hidden'), 3500);
}

function filterPages() {
  const query = document.getElementById('catalogSearch').value.toLowerCase();
  const rows = document.querySelectorAll('.page-row');
  let visible = 0;

  rows.forEach(row => {
    const code = row.getAttribute('data-code').toLowerCase();
    const name = row.getAttribute('data-name').toLowerCase();
    const url = row.getAttribute('data-url').toLowerCase();
    const id = row.id.replace('page-row-', '');
    const drawer = document.getElementById(`drawer-${id}`);

    if (code.includes(query) || name.includes(query) || url.includes(query)) {
      row.classList.remove('hidden');
      visible++;
    } else {
      row.classList.add('hidden');
      if (drawer) drawer.classList.add('hidden');
    }
  });

  document.getElementById('pageCounter').textContent = `${visible} pages`;
}

/* --- AJAX API CALLS --- */

// 1. CREATE PAGE
async function createPage(e) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);

  try {
    const response = await fetch('/admin/page-catalog/page/save', {
      method: 'POST',
      body: new URLSearchParams(formData)
    });

    if (response.ok) {
      showAlert('Page created successfully!');
      setTimeout(() => window.location.reload(), 400); // Dynamic row insert or clean refresh
    } else {
      showAlert('Failed to create page.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}

// 2. UPDATE PAGE META
async function updatePage(e, pageId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  formData.append('id', pageId);

  try {
    const response = await fetch('/admin/page-catalog/page/save', {
      method: 'POST',
      body: new URLSearchParams(formData)
    });

    if (response.ok) {
      const code = formData.get('code');
      const name = formData.get('name');
      const url = formData.get('url');

      // Update UI row elements in-place without reload
      const row = document.getElementById(`page-row-${pageId}`);
      row.querySelector('.page-code-tag').textContent = code;
      row.querySelector('.page-name-text').textContent = name;
      row.querySelector('.page-url-text').textContent = url;
      row.setAttribute('data-code', code);
      row.setAttribute('data-name', name);
      row.setAttribute('data-url', url);

      showAlert('Page updated!');
    } else {
      showAlert('Failed to update page details.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}

// 3. DELETE PAGE
async function deletePage(pageId) {
  if (!confirm('Delete this page and all associated buttons?')) return;

  const formData = new URLSearchParams();
  formData.append('id', pageId);

  try {
    const response = await fetch('/admin/page-catalog/page/delete', {
      method: 'POST',
      body: formData
    });

    if (response.ok) {
      document.getElementById(`page-row-${pageId}`)?.remove();
      document.getElementById(`drawer-${pageId}`)?.remove();
      showAlert('Page deleted.');
      filterPages();
    } else {
      showAlert('Could not delete page.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}

// 4. CREATE BUTTON
async function createButton(e, pageId) {
  e.preventDefault();
  const form = e.target;
  const formData = new FormData(form);
  formData.append('pageId', pageId);

  try {
    const response = await fetch('/admin/page-catalog/button/save', {
      method: 'POST',
      body: new URLSearchParams(formData)
    });

    if (response.ok) {
      showAlert('Button added!');
      setTimeout(() => window.location.reload(), 400);
    } else {
      showAlert('Failed to add button.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}

// 5. UPDATE BUTTON
async function updateButton(e, buttonId, pageId) {
  e.preventDefault();
  const formData = new FormData(e.target);
  formData.append('id', buttonId);
  formData.append('pageId', pageId);

  try {
    const response = await fetch('/admin/page-catalog/button/save', {
      method: 'POST',
      body: new URLSearchParams(formData)
    });

    if (response.ok) {
      showAlert('Button definition updated!');
    } else {
      showAlert('Failed to update button.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}

// 6. DELETE BUTTON
async function deleteButton(buttonId, pageId) {
  if (!confirm('Delete this button?')) return;

  const formData = new URLSearchParams();
  formData.append('id', buttonId);

  try {
    const response = await fetch('/admin/page-catalog/button/delete', {
      method: 'POST',
      body: formData
    });

    if (response.ok) {
      document.getElementById(`btn-chip-${buttonId}`)?.remove();
      
      // Decrement badge count
      const badge = document.getElementById(`badge-count-${pageId}`);
      if (badge) {
        badge.textContent = Math.max(0, parseInt(badge.textContent) - 1);
      }
      showAlert('Button removed.');
    } else {
      showAlert('Could not delete button.', false);
    }
  } catch (err) {
    showAlert('Server communication error.', false);
  }
}