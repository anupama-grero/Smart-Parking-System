/**
 * security-gate.js
 * Advanced Security Guard Dashboard & Gate Barrier Controller
 * Features: Dark/Light Mode, Live Barrier Visualizer, Ripple Animations, Real-Time Polling
 */

// Theme Management
function initTheme() {
    const savedTheme = localStorage.getItem('sps-theme') || 'dark';
    setTheme(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.body.classList.contains('dark-theme') ? 'dark' : 'light';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
}

function setTheme(theme) {
    if (theme === 'dark') {
        document.body.classList.add('dark-theme');
        const themeBtn = document.getElementById('themeToggleBtn');
        if (themeBtn) themeBtn.innerHTML = '☀️ <span class="theme-label">Light Mode</span>';
    } else {
        document.body.classList.remove('dark-theme');
        const themeBtn = document.getElementById('themeToggleBtn');
        if (themeBtn) themeBtn.innerHTML = '🌙 <span class="theme-label">Dark Mode</span>';
    }
    localStorage.setItem('sps-theme', theme);
}

// Barrier Status Fetch & Visualizer Update
async function fetchGateStatus() {
    try {
        const response = await fetch('/api/gates/status');
        if (!response.ok) {
            throw new Error(`Server status ${response.status}`);
        }
        
        const data = await response.json();
        updateUIWithGateData(data);

    } catch (error) {
        console.error('Failed to fetch gate status:', error);
        updateUIForOffline();
    }
}

function updateUIWithGateData(data) {
    // ESP32 Status
    const espBadge = document.getElementById('espStatus');
    const espPulse = document.getElementById('espPulse');
    const espText = data.esp32Status || 'Online';
    
    if (espBadge) {
        espBadge.textContent = espText;
        if (espText.includes('Offline')) {
            espBadge.className = 'status-pill status-offline';
            if (espPulse) espPulse.className = 'pulse-dot pulse-red';
        } else {
            espBadge.className = 'status-pill status-online';
            if (espPulse) espPulse.className = 'pulse-dot pulse-green';
        }
    }

    // Entrance Gate
    const entryState = (data.entryGate || 'CLOSED').toUpperCase();
    const entryBadge = document.getElementById('entryStatus');
    const entryArm = document.getElementById('entryArmVisual');
    const entryLight = document.getElementById('entryLightVisual');

    if (entryBadge) {
        entryBadge.textContent = entryState;
        entryBadge.className = entryState === 'OPEN' ? 'status-pill status-open' : 'status-pill status-closed';
    }

    if (entryArm) {
        if (entryState === 'OPEN') {
            entryArm.classList.add('barrier-arm-open');
        } else {
            entryArm.classList.remove('barrier-arm-open');
        }
    }

    if (entryLight) {
        entryLight.className = entryState === 'OPEN' ? 'signal-light signal-green' : 'signal-light signal-red';
    }

    // Exit Gate
    const exitState = (data.exitGate || 'CLOSED').toUpperCase();
    const exitBadge = document.getElementById('exitStatus');
    const exitArm = document.getElementById('exitArmVisual');
    const exitLight = document.getElementById('exitLightVisual');

    if (exitBadge) {
        exitBadge.textContent = exitState;
        exitBadge.className = exitState === 'OPEN' ? 'status-pill status-open' : 'status-pill status-closed';
    }

    if (exitArm) {
        if (exitState === 'OPEN') {
            exitArm.classList.add('barrier-arm-open');
        } else {
            exitArm.classList.remove('barrier-arm-open');
        }
    }

    if (exitLight) {
        exitLight.className = exitState === 'OPEN' ? 'signal-light signal-green' : 'signal-light signal-red';
    }
}

function updateUIForOffline() {
    const espBadge = document.getElementById('espStatus');
    const espPulse = document.getElementById('espPulse');
    if (espBadge) {
        espBadge.textContent = 'Offline';
        espBadge.className = 'status-pill status-offline';
    }
    if (espPulse) espPulse.className = 'pulse-dot pulse-red';
}

// Gate Barrier Action Handler with Ripple & Loading State
async function controlBarrier(gate, action, buttonId) {
    const btn = document.getElementById(buttonId);
    const msgBox = document.getElementById('messageBox');

    if (btn) {
        btn.disabled = true;
        btn.classList.add('btn-loading');
        btn.dataset.originalHtml = btn.innerHTML;
        btn.innerHTML = `<span class="spinner"></span> Processing...`;
    }

    if (msgBox) {
        msgBox.style.opacity = '0';
        msgBox.className = 'message-toast';
    }

    const endpoint = `/api/gates/${gate}/${action}`;

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const result = await response.json();

        if (!response.ok) {
            const errorText = result.message || `Failed to ${action} ${gate} barrier. Code: ${response.status}`;
            throw new Error(errorText);
        }

        showNotification(result.message || `${gate.toUpperCase()} barrier ${action}ed successfully!`, 'success');
        await fetchGateStatus();

    } catch (error) {
        console.error(`Error executing ${action} on ${gate} barrier:`, error);
        showNotification(error.message || `Failed to ${action} ${gate} barrier. Please retry.`, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.classList.remove('btn-loading');
            btn.innerHTML = btn.dataset.originalHtml || `${action.toUpperCase()} ${gate.toUpperCase()}`;
        }
    }
}

function showNotification(text, type) {
    const msgBox = document.getElementById('messageBox');
    if (!msgBox) return;

    msgBox.textContent = text;
    msgBox.className = `message-toast toast-${type} toast-show`;
    msgBox.style.opacity = '1';

    setTimeout(() => {
        msgBox.style.opacity = '0';
    }, 4500);
}

function openEntryBarrier() {
    return controlBarrier('entry', 'open', 'openEntryBtn');
}

function closeEntryBarrier() {
    return controlBarrier('entry', 'close', 'closeEntryBtn');
}

function openExitBarrier() {
    return controlBarrier('exit', 'open', 'openExitBtn');
}

function closeExitBarrier() {
    return controlBarrier('exit', 'close', 'closeExitBtn');
}

// Button Ripple Effect
function createRipple(event) {
    const button = event.currentTarget;
    const circle = document.createElement('span');
    const diameter = Math.max(button.clientWidth, button.clientHeight);
    const radius = diameter / 2;

    const rect = button.getBoundingClientRect();
    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - rect.left - radius}px`;
    circle.style.top = `${event.clientY - rect.top - radius}px`;
    circle.classList.add('ripple');

    const ripple = button.getElementsByClassName('ripple')[0];
    if (ripple) {
        ripple.remove();
    }

    button.appendChild(circle);
}

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    fetchGateStatus();
    setInterval(fetchGateStatus, 3000);

    const themeToggle = document.getElementById('themeToggleBtn');
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }

    document.querySelectorAll('.btn-action').forEach(button => {
        button.addEventListener('click', createRipple);
    });
});
