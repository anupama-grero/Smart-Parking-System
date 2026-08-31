/**
 * security-gate.js
 * Advanced Security Guard Dashboard & Gate Barrier Controller
 * Features: Dark/Light Mode, Single Active Gate Visualizer & Switcher, Real-Time Polling, Activity Feed
 */

// Global State
let currentGate = 'entry'; // 'entry' or 'exit'
let gateStates = {
    entry: 'CLOSED',
    exit: 'CLOSED'
};

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

// Gate Switcher Function
function switchGate(gateName) {
    if (gateName !== 'entry' && gateName !== 'exit') return;
    currentGate = gateName;

    // Update tab button highlights
    const tabEntry = document.getElementById('tabEntry');
    const tabExit = document.getElementById('tabExit');

    if (tabEntry && tabExit) {
        if (currentGate === 'entry') {
            tabEntry.classList.add('active');
            tabExit.classList.remove('active');
        } else {
            tabExit.classList.add('active');
            tabEntry.classList.remove('active');
        }
    }

    renderSingleGateView();
}

// Single Visualizer Renderer
function renderSingleGateView() {
    const activeState = (gateStates.entry || 'CLOSED').toUpperCase();
    
    // Icon and Title
    const iconElem = document.getElementById('activeGateIcon');
    const titleElem = document.getElementById('activeGateTitle');
    const badgeElem = document.getElementById('activeGateStatus');
    const armElem = document.getElementById('activeArmVisual');
    const lightElem = document.getElementById('activeLightVisual');

    if (iconElem) {
        iconElem.textContent = '🚗';
    }

    if (titleElem) {
        titleElem.textContent = 'Main Gate & Barrier';
    }

    if (badgeElem) {
        badgeElem.textContent = activeState;
        badgeElem.className = activeState === 'OPEN' ? 'status-pill status-open' : 'status-pill status-closed';
    }

    if (armElem) {
        if (activeState === 'OPEN') {
            armElem.classList.add('barrier-arm-open');
        } else {
            armElem.classList.remove('barrier-arm-open');
        }
    }

    if (lightElem) {
        lightElem.className = activeState === 'OPEN' ? 'signal-light-large signal-green' : 'signal-light-large signal-red';
    }
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

    // Cache Entrance & Exit States
    const entryState = (data.entryGate || 'CLOSED').toUpperCase();
    const exitState = (data.exitGate || 'CLOSED').toUpperCase();
    
    gateStates.entry = entryState;
    gateStates.exit = exitState;

    // Overview Summary Badges
    const entryBadge = document.getElementById('entryStatus');
    const exitBadge = document.getElementById('exitStatus');

    if (entryBadge) {
        entryBadge.textContent = entryState;
        entryBadge.className = entryState === 'OPEN' ? 'status-pill status-open' : 'status-pill status-closed';
    }

    if (exitBadge) {
        exitBadge.textContent = exitState;
        exitBadge.className = exitState === 'OPEN' ? 'status-pill status-open' : 'status-pill status-closed';
    }

    // Render single visualizer stage
    renderSingleGateView();
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

// Barrier Control Action Handler
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
            const errorText = result.message || `Failed to ${action} barrier. Code: ${response.status}`;
            throw new Error(errorText);
        }

        const successMsg = action === 'open' ? 'Barrier opened successfully!' : 'Barrier closed successfully!';
        showNotification(successMsg, 'success');
        addActivityLog(successMsg);
        
        await fetchGateStatus();
        tickVehicleSimulation(); // Trigger vehicle check instantly

    } catch (error) {
        console.error(`Error executing ${action} on barrier:`, error);
        const errorMsg = error.message || `Failed to ${action} barrier. Please retry.`;
        showNotification(errorMsg, 'error');
        addActivityLog(`Failed to ${action} barrier`, true);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.classList.remove('btn-loading');
            btn.innerHTML = btn.dataset.originalHtml || `${action.toUpperCase()} BARRIER`;
        }
    }
}

// Single Gate Actions
function openActiveGate() {
    return controlBarrier('entry', 'open', 'openActiveBtn');
}

function closeActiveGate() {
    return controlBarrier('entry', 'close', 'closeActiveBtn');
}

// Compatibility wrappers for direct calls
function openEntryBarrier() {
    return controlBarrier('entry', 'open', 'openActiveBtn');
}

function closeEntryBarrier() {
    return controlBarrier('entry', 'close', 'openActiveBtn');
}

function openExitBarrier() {
    return controlBarrier('exit', 'open', 'openActiveBtn');
}

function closeExitBarrier() {
    return controlBarrier('exit', 'close', 'openActiveBtn');
}

// Activity Log Helper
function addActivityLog(text, isError = false) {
    const list = document.getElementById('activityList');
    if (!list) return;

    const li = document.createElement('li');
    li.className = 'activity-item';
    
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    li.innerHTML = `
        <span style="${isError ? 'color: var(--accent-red); font-weight: 600;' : ''}">${text}</span>
        <span class="activity-time">${timeStr}</span>
    `;

    list.insertBefore(li, list.firstChild);

    // Keep log to maximum 10 items
    while (list.children.length > 10) {
        list.removeChild(list.lastChild);
    }
}

function showNotification(text, type) {
    const msgBox = document.getElementById('messageBox');
    if (!msgBox) return;

    msgBox.textContent = text;
    msgBox.className = `message-toast toast-${type} toast-show`;

    setTimeout(() => {
        msgBox.className = 'message-toast';
    }, 4500);
}

// Vehicle Simulation State Engine
let vehicleState = 'APPROACHING_ENTRY';
let isVehicleAnimating = false;

function startVehicleSimulation() {
    setInterval(tickVehicleSimulation, 1000);
}

function tickVehicleSimulation() {
    const car = document.getElementById('simVehicle');
    if (!car || isVehicleAnimating) return;

    const currentBarrierState = (gateStates.entry || 'CLOSED').toUpperCase();

    switch (vehicleState) {
        case 'APPROACHING_ENTRY':
            isVehicleAnimating = true;
            car.classList.add('vehicle-facing-right'); // Bumper facing right towards gate
            car.style.left = '40px'; // Stops at entrance approach line in front of gate
            setTimeout(() => {
                vehicleState = 'WAITING_ENTRY';
                isVehicleAnimating = false;
            }, 2300);
            break;

        case 'WAITING_ENTRY':
            if (currentBarrierState === 'OPEN') {
                isVehicleAnimating = true;
                car.classList.add('vehicle-facing-right');
                car.style.left = '340px'; // Drives into parking campus corner
                setTimeout(() => {
                    vehicleState = 'PARKED_PAUSE';
                    isVehicleAnimating = false;
                    // ~2 seconds waiting time in parking area
                    setTimeout(() => {
                        if (vehicleState === 'PARKED_PAUSE') {
                            car.classList.remove('vehicle-facing-right'); // Turns facing left towards exit
                            vehicleState = 'APPROACHING_EXIT';
                        }
                    }, 2000);
                }, 2300);
            }
            break;

        case 'APPROACHING_EXIT':
            isVehicleAnimating = true;
            car.classList.remove('vehicle-facing-right'); // Bumper facing left towards exit
            car.style.left = '160px'; // Stops at exit approach line inside gate
            setTimeout(() => {
                vehicleState = 'WAITING_EXIT';
                isVehicleAnimating = false;
            }, 2300);
            break;

        case 'WAITING_EXIT':
            if (currentBarrierState === 'OPEN') {
                isVehicleAnimating = true;
                car.classList.remove('vehicle-facing-right');
                car.style.left = '-80px'; // Drives out off-screen to left
                setTimeout(() => {
                    vehicleState = 'RESET_PAUSE';
                    isVehicleAnimating = false;
                    // ~2 seconds waiting time outside before next loop
                    setTimeout(() => {
                        car.classList.add('vehicle-facing-right'); // Turns facing right for next entry
                        car.style.left = '-60px';
                        vehicleState = 'APPROACHING_ENTRY';
                    }, 2000);
                }, 2300);
            }
            break;
    }
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
    startVehicleSimulation();

    const themeToggle = document.getElementById('themeToggleBtn');
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }

    document.querySelectorAll('.btn-action').forEach(button => {
        button.addEventListener('click', createRipple);
    });
});
