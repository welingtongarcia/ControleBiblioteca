// Verificação opcional do controlador, sem navegador nem dependência de build Java.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const source = readFileSync('src/main/resources/static/js/home-banner.js', 'utf8');

function fixture(reduced = false) {
    const element = (extra = {}) => ({
        hidden: false, textContent: '', attrs: {}, listeners: {},
        addEventListener(name, handler) { this.listeners[name] = handler; },
        setAttribute(name, value) { this.attrs[name] = value; },
        removeAttribute(name) { delete this.attrs[name]; },
        emit(name, event = {}) { this.listeners[name]?.(event); }, ...extra
    });
    const slides = Array.from({ length: 4 }, (_, i) => element({ dataset: { bannerTitle: `Painel ${i + 1}` } }));
    const selectors = slides.map(() => element());
    const controls = element({ hidden: true }), play = element(), position = element(), announcement = element();
    const prev = element(), next = element();
    const nodes = { '.banner-controls': controls, '[data-banner-play]': play, '[data-banner-position]': position,
        '[data-banner-announcement]': announcement, '[data-banner-prev]': prev, '[data-banner-next]': next };
    const banner = element({ classList: { add() {} }, querySelector: s => nodes[s],
        querySelectorAll: s => s === '.banner-slide' ? slides : selectors });
    const document = element({ hidden: false, getElementById: () => banner });
    const motion = element({ matches: reduced });
    let scheduled;
    const window = { matchMedia: () => motion, clearTimeout() { scheduled = undefined; },
        setTimeout(callback, delay) { assert.equal(delay, 8000); scheduled = callback; return 1; } };
    vm.runInNewContext(source, { document, window });
    return { slides, selectors, controls, play, position, announcement, prev, next, banner, document, motion,
        ticking: () => Boolean(scheduled), tick() { assert.ok(scheduled); const callback = scheduled; scheduled = undefined; callback(); } };
}

test('rotação a cada oito segundos e ciclo sem anúncios automáticos', () => {
    const f = fixture();
    assert.equal(f.controls.hidden, false);
    assert.deepEqual(f.slides.map(s => s.hidden), [false, true, true, true]);
    f.tick(); assert.equal(f.slides[1].hidden, false);
    f.tick(); f.tick(); f.tick(); assert.equal(f.slides[0].hidden, false);
    assert.equal(f.announcement.textContent, '');
});
test('navegação manual pausa, seleciona e anuncia sem mexer no foco', () => {
    const f = fixture();
    f.prev.emit('click'); assert.equal(f.slides[3].hidden, false);
    assert.equal(f.ticking(), false);
    assert.equal(f.play.textContent, 'Retomar rotação');
    f.next.emit('click'); assert.equal(f.slides[0].hidden, false);
    f.selectors[2].emit('click'); assert.equal(f.selectors[2].attrs['aria-current'], 'true');
    assert.equal(f.selectors[0].attrs['aria-current'], undefined);
    assert.equal(f.announcement.textContent, '3 de 4 · Painel 3');
});
test('foco pausa até retomada explícita e botão de pausa mantém ação', () => {
    const f = fixture();
    f.banner.emit('focusin', { target: f.next }); assert.equal(f.ticking(), false);
    f.play.emit('click'); assert.equal(f.ticking(), true);
    f.banner.emit('focusin', { target: f.play }); assert.equal(f.ticking(), true);
    f.play.emit('click'); assert.equal(f.ticking(), false);
});
test('hover e aba invisível suspendem e reiniciam intervalo completo', () => {
    const f = fixture();
    f.banner.emit('mouseenter'); assert.equal(f.ticking(), false);
    f.banner.emit('mouseleave'); assert.equal(f.ticking(), true);
    f.document.hidden = true; f.document.emit('visibilitychange'); assert.equal(f.ticking(), false);
    f.document.hidden = false; f.document.emit('visibilitychange'); assert.equal(f.ticking(), true);
});
test('movimento reduzido inicia pausado e mudança de preferência pausa', () => {
    const f = fixture(true);
    assert.equal(f.ticking(), false);
    assert.equal(f.play.textContent, 'Retomar rotação');
    f.play.emit('click'); assert.equal(f.ticking(), true);
    f.motion.emit('change'); assert.equal(f.ticking(), false);
});
