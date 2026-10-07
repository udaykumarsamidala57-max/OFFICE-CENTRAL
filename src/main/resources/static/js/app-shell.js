(() => {
    const toggle = document.getElementById('sidebarToggle');
    const overlay = document.getElementById('appOverlay');
    const sidebar = document.getElementById('appSidebar');

    if (!toggle || !overlay || !sidebar) return;

    // Match PInventory: opening a module closes any other expanded module.
    const moduleGroups = sidebar.querySelectorAll('.nav-module');
    moduleGroups.forEach((group) => {
        group.addEventListener('toggle', () => {
            if (!group.open) return;
            moduleGroups.forEach((other) => {
                if (other !== group && other.open) other.open = false;
            });
        });
    });

    const updateExpanded = () => {
        const isOpen = document.body.classList.contains('sidebar-open');
        toggle.setAttribute('aria-expanded', String(isOpen));
    };

    const closeSidebar = () => {
        document.body.classList.remove('sidebar-open');
        updateExpanded();
    };

    const openSidebar = () => {
        document.body.classList.add('sidebar-open');
        updateExpanded();
    };

    // Toggle button click
    toggle.addEventListener('click', () => {
        if (document.body.classList.contains('sidebar-open')) {
            closeSidebar();
        } else {
            openSidebar();
        }
    });

    // Close when clicking overlay (outside the sidebar)
    overlay.addEventListener('click', closeSidebar);

    // Close when pressing Escape
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeSidebar();
    });

    // Auto-close when clicking any link inside sidebar
    sidebar.addEventListener('click', (event) => {
        if (event.target.closest('a')) {
            closeSidebar();
        }
    });

    updateExpanded();
})();