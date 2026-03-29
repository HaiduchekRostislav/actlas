/* ================================================
   ACTLAS — app.js
   ================================================ */

/* ── Toast ── */
const Toast = {
    container: null,

    init() {
        this.container = document.createElement('div');
        this.container.className = 'toast-container';
        document.body.appendChild(this.container);
    },

    show(message, type = 'info', duration = 3000) {
        if (!this.container) this.init();
        const toast = document.createElement('div');
        toast.className = 'toast toast--' + type;
        toast.textContent = message;
        this.container.appendChild(toast);
        setTimeout(() => toast.remove(), duration);
    },

    success(msg) { this.show(msg, 'success'); },
    error(msg)   { this.show(msg, 'error'); },
    info(msg)    { this.show(msg, 'info'); }
};

/* ── Search ── */
const Search = {
    input: null,
    dropdown: null,
    debounceTimer: null,
    debounceDelay: 400,

    init(inputId, dropdownId) {
        this.input    = document.getElementById(inputId);
        this.dropdown = document.getElementById(dropdownId);
        if (!this.input || !this.dropdown) return;

        this.input.addEventListener('input', () => this.onInput());
        this.input.addEventListener('focus', () => {
            if (this.input.value.trim().length >= 2) {
                this.dropdown.classList.add('active');
            }
        });

        document.addEventListener('click', (e) => {
            if (!this.input.contains(e.target) && !this.dropdown.contains(e.target)) {
                this.dropdown.classList.remove('active');
            }
        });
    },

    onInput() {
        clearTimeout(this.debounceTimer);
        const query = this.input.value.trim();
        if (query.length < 2) {
            this.dropdown.classList.remove('active');
            return;
        }
        this.showLoading();
        this.debounceTimer = setTimeout(() => this.fetchResults(query), this.debounceDelay);
    },

    async fetchResults(query) {
        try {
            const res  = await fetch('/api/search?query=' + encodeURIComponent(query));
            const data = await res.json();
            this.renderResults(data);
        } catch (e) {
            this.renderError();
        }
    },

    renderResults(results) {
        this.dropdown.innerHTML = '';
        this.dropdown.classList.add('active');

        if (!results || results.length === 0) {
            this.dropdown.innerHTML = '<div class="search-state">Нічого не знайдено</div>';
            return;
        }

        results.forEach(movie => {
            const item = document.createElement('div');
            item.className = 'search-result-item';

            item.innerHTML =
                (movie.imageUrl
                    ? '<img class="search-result-item__poster" src="' + movie.imageUrl + '" loading="lazy" alt="">'
                    : '<div class="search-result-item__poster-placeholder"></div>') +
                '<div class="search-result-item__info">' +
                '<div class="search-result-item__title">' + movie.title + '</div>' +
                '<div class="search-result-item__meta">' + (movie.year || '—') + '</div>' +
                '</div>' +
                (movie.type ? '<span class="search-result-item__type">' + movie.type + '</span>' : '');

            item.addEventListener('click', () => this.onSelect(movie));
            this.dropdown.appendChild(item);
        });
    },

    showLoading() {
        this.dropdown.classList.add('active');
        this.dropdown.innerHTML =
            '<div class="search-state"><div class="search-spinner"></div>Пошук...</div>';
    },

    renderError() {
        this.dropdown.innerHTML =
            '<div class="search-state">Помилка пошуку. Спробуй ще раз.</div>';
    },

    onSelect(movie) {
        console.log('Selected:', movie);
    }
};

/* ── Movie API ── */
const MovieAPI = {
    async fetch(imdbId) {
        const res = await fetch('/api/movies/' + imdbId, { method: 'POST' });
        if (!res.ok) throw new Error('Не вдалось отримати фільм');
        return res.json();
    },
    async get(imdbId) {
        const res = await fetch('/api/movies/' + imdbId);
        if (!res.ok) throw new Error('Фільм не знайдено');
        return res.json();
    },
    async delete(imdbId) {
        const res = await fetch('/api/movies/' + imdbId, { method: 'DELETE' });
        if (!res.ok) throw new Error('Помилка видалення');
    },
    async getHistory() {
        const res = await fetch('/api/history');
        if (!res.ok) throw new Error('Помилка завантаження історії');
        return res.json();
    }
};

/* ── Actor API ── */
const ActorAPI = {
    async get(imdbId) {
        const res = await fetch('/api/actors/' + imdbId);
        if (!res.ok) throw new Error('Актора не знайдено');
        return res.json();
    },
    async getMovies(imdbId) {
        const res = await fetch('/api/actors/' + imdbId + '/movies');
        if (!res.ok) throw new Error('Помилка завантаження фільмів');
        return res.json();
    },
    async getCommonMovies(actorIds) {
        const params = actorIds.map(id => 'actorIds=' + id).join('&');
        const res = await fetch('/api/actors/common-movies?' + params);
        if (!res.ok) throw new Error('Помилка пошуку спільних фільмів');
        return res.json();
    }
};

/* ── Common Movies Modal ── */
// Створюємо модалку програмно — вона потрібна на будь-якій сторінці де є pinned bar
function createCommonMoviesModal() {
    if (document.getElementById('common-movies-modal')) return;

    const modal = document.createElement('div');
    modal.id = 'common-movies-modal';
    modal.className = 'modal-overlay';
    modal.style.display = 'none';
    modal.innerHTML =
        '<div class="modal">' +
        '<div class="modal__header">' +
        '<h3 class="modal__title" id="common-modal-title">Спільні фільми</h3>' +
        '<button class="modal__close" id="common-modal-close">&#x2715;</button>' +
        '</div>' +
        '<div class="modal__body" id="common-modal-body"></div>' +
        '</div>';

    document.body.appendChild(modal);

    document.getElementById('common-modal-close').addEventListener('click', () => {
        modal.style.display = 'none';
    });
    modal.addEventListener('click', function(e) {
        if (e.target === this) this.style.display = 'none';
    });
}

function renderMovieCards(movies) {
    if (!movies || movies.length === 0) {
        return '<div class="empty-state">' +
            '<div class="empty-state__line"></div>' +
            '<div class="empty-state__text">Спільних фільмів у базі не знайдено.<br>' +
            'Переглянь більше фільмів з цими акторами.</div></div>';
    }
    return '<div class="grid grid--movies">' +
        movies.map(m =>
            '<div class="movie-card" style="cursor:pointer" ' +
            'onclick="window.location.href=\'/movies/' + m.imdbId + '\'">' +
            (m.imageUrl
                ? '<img class="movie-card__poster" src="' + m.imageUrl + '" loading="lazy" alt="">'
                : '<div class="movie-card__poster-placeholder"></div>') +
            '<div class="movie-card__body">' +
            '<div class="movie-card__title">' + m.title + '</div>' +
            '<div class="movie-card__meta"><span>' + (m.year || '—') + '</span>' +
            (m.rating
                ? '<span class="movie-card__rating">&#9733; ' + m.rating.toFixed(1) + '</span>'
                : '') +
            '</div></div></div>'
        ).join('') +
        '</div>';
}

/* ── Pinned Actors ── */
const PinnedActors = {
    pinned: [],
    bar: null,
    actorsContainer: null,
    maxPinned: 3,

    init() {
        this.bar             = document.getElementById('pinned-bar');
        this.actorsContainer = document.getElementById('pinned-actors');
        if (!this.bar) return;

        createCommonMoviesModal();

        const findBtn  = document.getElementById('find-common-btn');
        const clearBtn = document.getElementById('clear-pinned-btn');
        if (findBtn)  findBtn.addEventListener('click',  () => this.findCommon());
        if (clearBtn) clearBtn.addEventListener('click', () => this.clear());
    },

    toggle(actorId, actorName, actorImage) {
        const idx = this.pinned.findIndex(a => a.id === actorId);

        if (idx !== -1) {
            this.pinned.splice(idx, 1);
            Toast.info(actorName + ' відкріплено');
        } else {
            if (this.pinned.length >= this.maxPinned) {
                Toast.error('Можна закріпити максимум ' + this.maxPinned + ' актори');
                return false;
            }
            this.pinned.push({ id: actorId, name: actorName, image: actorImage });
            Toast.success(actorName + ' закріплено');
        }

        this.render();
        this.updateButtons();
        return true;
    },

    isPinned(actorId) {
        return this.pinned.some(a => a.id === actorId);
    },

    render() {
        if (!this.bar) return;
        if (this.pinned.length === 0) {
            this.bar.classList.remove('active');
            return;
        }
        this.bar.classList.add('active');
        if (this.actorsContainer) {
            this.actorsContainer.innerHTML = this.pinned.map(a =>
                '<div class="pinned-bar__actor">' +
                (a.image
                    ? '<img src="' + a.image + '" alt="' + a.name + '">'
                    : '<div style="width:26px;height:26px;border-radius:50%;' +
                    'background:var(--bg-hover);border:1px solid var(--border-dark)"></div>') +
                '<span>' + a.name + '</span>' +
                '</div>'
            ).join('');
        }
    },

    updateButtons() {
        document.querySelectorAll('.actor-card__pin').forEach(btn => {
            const pinned = this.isPinned(btn.dataset.actorId);
            btn.classList.toggle('pinned', pinned);
            btn.textContent = pinned ? 'Закріплено' : 'Закріпити';
        });
    },

    async findCommon() {
        if (this.pinned.length < 2) {
            Toast.error('Закріпи мінімум 2 актори');
            return;
        }

        const modal = document.getElementById('common-movies-modal');
        const body  = document.getElementById('common-modal-body');
        const title = document.getElementById('common-modal-title');

        // Назви акторів в заголовку
        title.textContent = this.pinned.map(a => a.name).join(' + ');

        body.innerHTML =
            '<div class="loading-state"><div class="search-spinner"></div>Пошук спільних фільмів...</div>';
        modal.style.display = 'flex';

        try {
            const ids    = this.pinned.map(a => a.id);
            const movies = await ActorAPI.getCommonMovies(ids);
            body.innerHTML = renderMovieCards(movies);
        } catch (e) {
            body.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state__text">Помилка пошуку</div></div>';
            Toast.error('Помилка пошуку спільних фільмів');
        }
    },

    clear() {
        this.pinned = [];
        this.render();
        this.updateButtons();
    }
};

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    Toast.init();
    PinnedActors.init();

    document.addEventListener('click', (e) => {
        const pinBtn = e.target.closest('.actor-card__pin');
        if (pinBtn) {
            const { actorId, actorName, actorImage } = pinBtn.dataset;
            PinnedActors.toggle(actorId, actorName, actorImage || '');
        }
    });
});