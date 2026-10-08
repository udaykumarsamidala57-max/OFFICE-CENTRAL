(() => {
    const initAppShell = () => {
        const toggle = document.getElementById('sidebarToggle');
        const overlay = document.getElementById('appOverlay');
        const sidebar = document.getElementById('appSidebar');

        if (!toggle || !overlay || !sidebar) return;

        const desktopSidebar = window.matchMedia('(min-width: 992px)');
        const hiddenPreference = 'appSidebarAutoHidden';

        // Keep a desktop auto-hide choice across the full-page navigation used by this app.
        if (desktopSidebar.matches && localStorage.getItem(hiddenPreference) !== 'true') {
            document.body.classList.add('sidebar-open');
        } else {
            document.body.classList.remove('sidebar-open');
        }

        // Auto-close open module details when opening another (Accordion effect)
        sidebar.addEventListener('toggle', (event) => {
            const targetModule = event.target;
            if (targetModule.tagName === 'DETAILS' && targetModule.open) {
                sidebar.querySelectorAll('.nav-module').forEach((group) => {
                    if (group !== targetModule && group.open) {
                        group.open = false;
                    }
                });
            }
        }, true);

        const updateExpanded = () => {
            const isOpen = document.body.classList.contains('sidebar-open');
            toggle.setAttribute('aria-expanded', String(isOpen));
        };

        const closeSidebar = () => {
            document.body.classList.remove('sidebar-open');
            if (desktopSidebar.matches) localStorage.setItem(hiddenPreference, 'true');
            updateExpanded();
        };

        const openSidebar = () => {
            document.body.classList.add('sidebar-open');
            if (desktopSidebar.matches) localStorage.removeItem(hiddenPreference);
            updateExpanded();
        };

        // Toggle button click handler
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            if (document.body.classList.contains('sidebar-open')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });

        // Close when clicking overlay (outside drawer)
        overlay.addEventListener('click', closeSidebar);

        // Close on Escape key press
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') closeSidebar();
        });

        desktopSidebar.addEventListener('change', () => {
            if (desktopSidebar.matches && localStorage.getItem(hiddenPreference) !== 'true') {
                document.body.classList.add('sidebar-open');
            } else if (!desktopSidebar.matches) {
                document.body.classList.remove('sidebar-open');
            }
            updateExpanded();
        });

        // Auto-close when clicking any link inside sidebar
        sidebar.addEventListener('click', (event) => {
            if (event.target.closest('a')) {
                closeSidebar();
            }
        });

        updateExpanded();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAppShell);
    } else {
        initAppShell();
    }
})();
