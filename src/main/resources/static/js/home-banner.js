(() => {
    'use strict';
    const banner = document.getElementById('home-banner');
    if (!banner) return;
    const slides = [...banner.querySelectorAll('.banner-slide')];
    const controls = banner.querySelector('.banner-controls');
    const selectors = [...banner.querySelectorAll('[data-banner-select]')];
    const play = banner.querySelector('[data-banner-play]');
    const position = banner.querySelector('[data-banner-position]');
    const announcement = banner.querySelector('[data-banner-announcement]');
    if (slides.length < 2 || !controls || !play) return;

    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let current = 0;
    let paused = motion.matches;
    let hovered = false;
    let timer;
    const schedule = () => {
        window.clearTimeout(timer);
        if (!paused && !hovered && !document.hidden) {
            timer = window.setTimeout(() => show(current + 1, false), 8000);
        }
    };
    const updatePlay = () => {
        play.textContent = paused ? 'Retomar rotação' : 'Pausar rotação';
    };
    const show = (index, manual) => {
        current = (index + slides.length) % slides.length;
        slides.forEach((slide, i) => {
            slide.hidden = i !== current;
            slide.inert = i !== current;
            slide.setAttribute('aria-hidden', String(i !== current));
        });
        selectors.forEach((selector, i) => {
            if (i === current) selector.setAttribute('aria-current', 'true');
            else selector.removeAttribute('aria-current');
        });
        const label = `${current + 1} de ${slides.length} · ${slides[current].dataset.bannerTitle}`;
        position.textContent = label;
        if (manual) announcement.textContent = label;
        schedule();
    };
    const pause = () => { paused = true; updatePlay(); schedule(); };
    const navigate = (index) => { pause(); show(index, true); };
    banner.querySelector('[data-banner-prev]').addEventListener('click', () => navigate(current - 1));
    banner.querySelector('[data-banner-next]').addEventListener('click', () => navigate(current + 1));
    selectors.forEach((selector, i) => selector.addEventListener('click', () => navigate(i)));
    play.addEventListener('click', () => { paused = !paused; updatePlay(); schedule(); });
    // Foco interrompe até uma retomada explícita, sem perder o foco ao trocar painel.
    banner.addEventListener('focusin', (event) => { if (event.target !== play) pause(); });
    banner.addEventListener('mouseenter', () => { hovered = true; schedule(); });
    banner.addEventListener('mouseleave', () => { hovered = false; schedule(); });
    document.addEventListener('visibilitychange', schedule);
    motion.addEventListener('change', pause);
    banner.classList.add('banner-ready');
    controls.hidden = false;
    updatePlay();
    show(0, false);
})();
