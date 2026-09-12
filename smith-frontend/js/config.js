const DEFAULT_BACKEND_PORT = 3616;

function defaultBaseUrl() {
    const { protocol, hostname } = window.location;
    if (!hostname) {
        return `http://localhost:${DEFAULT_BACKEND_PORT}`;
    }
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1';
    if (protocol === 'https:' || !isLocal) {
        return `${protocol}//${hostname}`;
    }
    return `${protocol}//${hostname}:${DEFAULT_BACKEND_PORT}`;
}

function resolveBaseUrl() {
    const override = window.SMITH_API_BASE_URL
        || localStorage.getItem('smith.baseUrl')
        || defaultBaseUrl();
    return String(override).replace(/\/+$/, '');
}

export const API = {
    baseUrl: resolveBaseUrl(),
    prefix: '/api/v1',
};

export function apiUrl(path) {
    return `${API.baseUrl}${API.prefix}${path}`;
}

export function authUrl(path) {
    return `${API.baseUrl}/api/auth${path}`;
}

export const CONTEXT_CHAR_LIMIT = 12000;
