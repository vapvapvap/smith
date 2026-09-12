const DEFAULT_BASE_URL = 'http://localhost:8080';

function resolveBaseUrl() {
    const override = window.SMITH_API_BASE_URL
        || localStorage.getItem('smith.baseUrl')
        || DEFAULT_BASE_URL;
    return String(override).replace(/\/+$/, '');
}

export const API = {
    baseUrl: resolveBaseUrl(),
    prefix: '/api/v1',
};

export function apiUrl(path) {
    return `${API.baseUrl}${API.prefix}${path}`;
}

export const CONTEXT_CHAR_LIMIT = 12000;
