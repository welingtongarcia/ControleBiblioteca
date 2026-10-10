(() => {
    'use strict';
    document.querySelectorAll('.association-picker').forEach(picker => {
        const field = picker.dataset.field;
        const results = picker.querySelector('.picker-results');
        const selected = new Map();
        results.querySelectorAll('label[data-id]').forEach(label => {
            if (label.querySelector('input[type=checkbox]').checked) selected.set(label.dataset.id, {
                id: label.dataset.id, label: label.dataset.label, description: label.dataset.description
            });
        });
        const search = picker.querySelector('input[type=search]');
        const filter = picker.querySelector('.picker-function');
        const chips = picker.querySelector('.picker-chips');
        const status = picker.querySelector('.picker-status');
        const pages = picker.querySelector('.picker-pages');
        const previous = picker.querySelector('.picker-previous');
        const next = picker.querySelector('.picker-next');
        let currentItems = [], page = 0, controller, generation = 0, timer;
        function element(tag, text, className) {
            const node = document.createElement(tag);
            if (text != null) node.textContent = text;
            if (className) node.className = className;
            return node;
        }
        function renderSelected() {
            chips.replaceChildren();
            selected.forEach(item => {
                const chip = element('div', null, 'picker-chip');
                const input = element('input'); input.type = 'hidden'; input.name = field; input.value = item.id;
                const name = element('span', item.label);
                if (item.description) name.append(element('small', item.description, 'help-text'));
                const remove = element('button', 'Remover'); remove.type = 'button';
                remove.setAttribute('aria-label', 'Remover ' + item.label);
                remove.addEventListener('click', () => {
                    selected.delete(item.id); renderSelected(); renderResults(); search.focus();
                    status.textContent = item.label + ' removido. ' + selected.size + ' selecionados.';
                });
                chip.append(input, name, remove); chips.append(chip);
            });
            picker.querySelector('.picker-count').textContent = String(selected.size);
            if (!selected.size) chips.append(element('p', 'Nenhum registro selecionado.', 'help-text'));
        }
        function renderResults() {
            results.replaceChildren();
            currentItems.forEach(item => {
                const row = element('div', null, 'picker-result');
                const name = element('span', item.label);
                if (item.description) name.append(element('small', item.description, 'help-text'));
                const add = element('button', selected.has(item.id) ? 'Selecionado' : 'Adicionar');
                add.type = 'button'; add.disabled = selected.has(item.id);
                add.setAttribute('aria-label', (selected.has(item.id) ? 'Selecionado: ' : 'Adicionar ') + item.label);
                add.addEventListener('click', () => {
                    selected.set(item.id, item); renderSelected(); renderResults(); search.focus();
                    status.textContent = item.label + ' adicionado. ' + selected.size + ' selecionados.';
                });
                row.append(name, add); results.append(row);
            });
        }
        async function load(targetPage = 0, focusResults = false) {
            clearTimeout(timer); const request = ++generation;
            if (controller) controller.abort(); controller = new AbortController();
            status.textContent = 'Buscando registros…'; results.setAttribute('aria-busy', 'true');
            // Não deixar resultados antigos disponíveis durante uma nova pesquisa.
            currentItems = []; renderResults(); pages.hidden = true;
            try {
                const url = new URL(picker.dataset.url, location.origin);
                url.searchParams.set('q', search.value); url.searchParams.set('page', String(targetPage));
                if (filter && filter.value) url.searchParams.set('function', filter.value);
                const response = await fetch(url, { signal: controller.signal, headers: { Accept: 'application/json' }, cache: 'no-store' });
                if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('search');
                const data = await response.json(); if (request !== generation) return;
                currentItems = data.items; page = data.page; renderResults();
                previous.disabled = page === 0; next.disabled = !data.hasNext || page >= 500;
                picker.querySelector('.picker-page').textContent = 'Página ' + (page + 1);
                pages.hidden = !page && !data.hasNext;
                status.textContent = data.items.length ? data.items.length + ' resultados nesta página.' : 'Nenhum resultado. Tente outro nome ou filtro.';
                if (focusResults) status.focus();
            } catch (error) {
                if (request === generation && error.name !== 'AbortError') {
                    status.textContent = 'Não foi possível pesquisar. Suas seleções foram mantidas. Tente Buscar novamente; se a sessão expirou, entre novamente.';
                    if (focusResults) status.focus();
                }
            } finally { if (request === generation) results.removeAttribute('aria-busy'); }
        }
        picker.querySelector('.picker-controls').hidden = false;
        picker.querySelector('.picker-selected').hidden = false;
        search.addEventListener('input', () => {
            clearTimeout(timer); ++generation; if (controller) controller.abort();
            pages.hidden = true; currentItems = []; renderResults();
            timer = setTimeout(() => load(), 300);
        });
        search.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); load(); } });
        if (filter) filter.addEventListener('change', () => load());
        picker.querySelector('.picker-search').addEventListener('click', () => load());
        previous.addEventListener('click', () => load(page - 1, true));
        next.addEventListener('click', () => load(page + 1, true));
        renderSelected(); load();
    });
})();
