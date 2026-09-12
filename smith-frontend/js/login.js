import { login } from './api.js';

const form = document.getElementById('login-form');
const usernameField = document.getElementById('username');
const passwordField = document.getElementById('password');
const errorEl = document.getElementById('login-error');
const submit = document.getElementById('login-submit');

function applyTheme() {
    const stored = localStorage.getItem('smith.theme');
    const theme = stored
        || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    document.documentElement.dataset.theme = theme;
}

function showError(message) {
    errorEl.textContent = message;
    errorEl.hidden = !message;
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = usernameField.value.trim();
    const password = passwordField.value;

    if (!username || !password) {
        showError('Введите логин и пароль');
        return;
    }

    showError('');
    submit.disabled = true;
    try {
        await login(username, password);
        window.location.replace('index.html');
    } catch (error) {
        showError(error.message || 'Не удалось войти');
        submit.disabled = false;
        passwordField.select();
    }
});

applyTheme();
usernameField.focus();
