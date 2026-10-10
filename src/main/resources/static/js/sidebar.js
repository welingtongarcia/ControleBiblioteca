(() => {
    'use strict';
    const sidebar = document.getElementById('main-sidebar');
    const toggle = document.getElementById('sidebar-toggle');
    const controls = document.getElementById('sidebar-controls');
    const layout = sidebar?.closest('.dashboard-layout');
    if (!sidebar || !toggle || !controls || !layout) return;

    const preferenceKey = 'minha-gibiteca.sidebar-hidden';
    const setHidden = (hidden) => {
        sidebar.hidden = hidden;
        layout.classList.toggle('sidebar-is-hidden', hidden);
        toggle.setAttribute('aria-expanded', String(!hidden));
        toggle.textContent = hidden ? 'Mostrar menu' : 'Ocultar menu';
    };
    let savedHidden = false;
    try { savedHidden = localStorage.getItem(preferenceKey) === 'true'; }
    catch { /* Armazenamento indisponível: navegação permanece utilizável. */ }
    setHidden(savedHidden);
    toggle.addEventListener('click', () => {
        const hidden = !sidebar.hidden;
        setHidden(hidden);
        try { localStorage.setItem(preferenceKey, String(hidden)); }
        catch { /* A preferência vale para esta página mesmo sem persistência. */ }
    });
    layout.classList.add('sidebar-ready');
    controls.hidden = false;
})();
