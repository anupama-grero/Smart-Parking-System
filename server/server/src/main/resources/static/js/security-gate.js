/**
 * security-gate.js
 * Advanced Security Guard Dashboard & Gate Barrier Controller
 * Features: Dark/Light Mode, Single Active Gate Visualizer & Switcher, Real-Time Polling, Activity Feed
 */

// Global State
let currentGate = 'entry';
let gateStates = {
    entry: 'UNKNOWN',
    exit: 'UNSUPPORTED'
};
let parkingIsFull = false;
let gateControlEnabled = false;
let gateCommandState = 'IDLE';
let entryOpenAllowed = false;
let gateStateConfirmed = false;

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
    const themeBtn = document.getElementById('themeToggleBtn');
    if (theme === 'dark') {
        document.body.classList.add('dark-theme');
        if (themeBtn) {
            themeBtn.innerHTML = '☀️';
            themeBtn.title = 'Switch to Light Mode';
            themeBtn.setAttribute('aria-label', 'Switch to Light Mode');
        }
    } else {
        document.body.classList.remove('dark-theme');
        if (themeBtn) {
            themeBtn.innerHTML = '🌙';
            themeBtn.title = 'Switch to Dark Mode';
            themeBtn.setAttribute('aria-label', 'Switch to Dark Mode');
        }
    }
    localStorage.setItem('sps-theme', theme);
}

// Gate Switcher Function
function switchGate(gateName) {
    if (gateName !== 'entry' && gateName !== 'exit') return;
    currentGate = gateName;

    renderSingleGateView();
}

// Single Visualizer Renderer
function renderSingleGateView() {
    const activeState = (gateStates[currentGate] || 'CLOSED').toUpperCase();

    const iconElem = document.getElementById('activeGateIcon');
    const titleElem = document.getElementById('activeGateTitle');
    const badgeElem = document.getElementById('activeGateStatus');
    const armElem = document.getElementById('activeArmVisual');
    const lightElem = document.getElementById('activeLightVisual');

    if (iconElem) iconElem.textContent = '🚗';
    if (titleElem) {
        titleElem.textContent = currentGate === 'entry'
            ? 'Entry Gate & Barrier'
            : 'Exit Gate & Barrier';
    }

    if (badgeElem) {
        badgeElem.textContent = gateStateConfirmed || activeState === 'UNKNOWN'
            ? activeState
            : `${activeState} · LAST KNOWN`;
        badgeElem.className = !gateStateConfirmed
            ? 'status-pill status-offline'
            : activeState === 'OPEN'
                ? 'status-pill status-open'
                : activeState === 'CLOSED'
                    ? 'status-pill status-closed'
                    : 'status-pill status-offline';
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
        const [gateResponse, parkingResponse] = await Promise.all([
            fetch('/api/gates/status'),
            fetch('/api/parking/status')
        ]);
        if (!gateResponse.ok || !parkingResponse.ok) {
            throw new Error(`Server status ${gateResponse.status}/${parkingResponse.status}`);
        }

        const data = await gateResponse.json();
        const parkingData = await parkingResponse.json();
        updateUIWithGateData(data);
        updateParkingUI(parkingData);

    } catch (error) {
        console.error('Failed to fetch gate status:', error);
        updateUIForOffline();
    }
}

function updateReverseWarningUI(data) {
    const badge = document.getElementById('rwStatus');
    const pulse = document.getElementById('rwPulse');

    if (!badge || !pulse) return;

    const active = Boolean(data.active || data.reverseWarning);

    if (active) {
        badge.textContent = 'ACTIVE';
        badge.className = 'status-pill status-offline';
        pulse.className = 'pulse-dot pulse-red';
    } else {
        badge.textContent = 'INACTIVE';
        badge.className = 'status-pill status-online';
        pulse.className = 'pulse-dot pulse-green';
    }
}

function updateUIWithGateData(data) {
    const espBadge = document.getElementById('espStatus');
    const espPulse = document.getElementById('espPulse');
    const espText = data.esp32Status || 'Disconnected';

    if (espBadge) {
        espBadge.textContent = espText;
        if (!data.hardwareConnected) {
            espBadge.className = 'status-pill status-offline';
            if (espPulse) espPulse.className = 'pulse-dot pulse-red';
        } else {
            espBadge.className = 'status-pill status-online';
            if (espPulse) espPulse.className = 'pulse-dot pulse-green';
        }
    }

    const previousEntryState = gateStates.entry;
    gateStates.entry = (data.entryGate || 'UNKNOWN').toUpperCase();
    gateStates.exit = 'UNSUPPORTED';
    gateStateConfirmed = Boolean(data.gateStateConfirmed);
    gateControlEnabled = Boolean(data.gateControlEnabled);
    entryOpenAllowed = Boolean(data.entryOpenAllowed);
    gateCommandState = data.commandState || 'IDLE';
    const commandStatus = document.getElementById('gate-command-status');
    if (commandStatus) {
        commandStatus.textContent = data.commandMessage || 'No gate command reported.';
        commandStatus.dataset.state = gateCommandState.toLowerCase();
    }
    const updateLabel = document.getElementById('lastHardwareUpdate');
    if (updateLabel) {
        updateLabel.textContent = data.lastHardwareUpdate
            ? new Date(data.lastHardwareUpdate).toLocaleString()
            : 'Not received';
        updateLabel.className = data.hardwareConnected
            ? 'status-pill status-online'
            : 'status-pill status-offline';
    }

    renderSingleGateView();
    updateGateControlButtons(data.commandState);
    if (gateStates.entry !== previousEntryState) {
        tickVehicleSimulation();
        if (gateStates.entry === 'OPEN') {
            addActivityLog('🔓 Gate OPENED — confirmed by hardware');
        } else if (gateStates.entry === 'CLOSED') {
            addActivityLog('🔒 Gate CLOSED — confirmed by hardware');
        }
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
    gateControlEnabled = false;
    updateGateControlButtons('FAILED');
}

function updateGateControlButtons(commandState = gateCommandState) {
    const token = document.getElementById('gateControlToken');
    const authorized = Boolean(token && token.value.trim());
    const pending = String(commandState).toUpperCase() === 'PENDING';
    const canOpen = gateControlEnabled && entryOpenAllowed && authorized && !parkingIsFull && !pending;
    const canClose = gateControlEnabled && authorized && !pending;
    const openButton = document.getElementById('openActiveBtn');
    const closeButton = document.getElementById('closeActiveBtn');
    if (openButton) openButton.disabled = !canOpen;
    if (closeButton) closeButton.disabled = !canClose;
}

function updateParkingUI(data) {
    if (!data || !Array.isArray(data.slots) || data.slots.length !== 4) {
        throw new Error('Backend returned an invalid four-slot parking response');
    }
    parkingIsFull = Boolean(data.full);
    const fullWarning = document.getElementById('guard-parking-full');
    if (fullWarning) fullWarning.hidden = !parkingIsFull;

    const dataWarning = document.getElementById('guard-parking-data');
    if (dataWarning) {
        dataWarning.hidden = !data.stale && Number(data.unknownSlots || 0) === 0;
        dataWarning.textContent = data.stale
            ? 'ESP32 occupancy telemetry is stale or disconnected.'
            : `${Number(data.unknownSlots || 0)} slot sensor(s) currently have unknown readings.`;
    }

    data.slots.forEach(slot => {
        const badge = document.getElementById(`guard-slot-${slot.slotNumber}`);
        if (!badge) return;
        const status = !slot.known
            ? 'Unknown'
            : `${slot.occupied ? 'Occupied' : 'Available'}${slot.sensorFresh ? '' : ' · stale'}`;
        badge.textContent = `Slot ${slot.slotNumber}: ${status}`;
        badge.className = !slot.known || !slot.sensorFresh
            ? 'status-pill status-offline'
            : slot.occupied ? 'status-pill status-open' : 'status-pill status-online';
    });

    updateReverseWarningUI({ active: Boolean(data.slots[1].reverseWarning) });
    updateGateControlButtons();
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

    if (gate !== 'entry') {
        showNotification('The connected firmware does not support a separate exit-gate command.', 'error');
        return;
    }
    if (action === 'open' && parkingIsFull) {
        showNotification('PARKING FULL — entry opening is blocked.', 'error');
        updateGateControlButtons();
        return;
    }

    const endpoint = `/api/gates/${gate}/${action}`;
    const controlToken = document.getElementById('gateControlToken')?.value.trim();

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Gate-Control-Token': controlToken || ''
            }
        });

        const result = await response.json();

        if (!response.ok) {
            const errorText = result.message || `Failed to ${action} barrier. Code: ${response.status}`;
            throw new Error(errorText);
        }

        const successMsg = result.commandMessage || result.message || 'Command accepted; awaiting ESP32 confirmation.';
        showNotification(successMsg, 'success');
        addActivityLog(`${action.toUpperCase()} command pending confirmation`);
        
        await fetchGateStatus();

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
    return Promise.resolve(showNotification('Exit is operated by the firmware exit sensor; separate remote control is unavailable.', 'error'));
}

function closeExitBarrier() {
    return Promise.resolve(showNotification('Exit is operated by the firmware exit sensor; separate remote control is unavailable.', 'error'));
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
        <span class="activity-text" style="${isError ? 'color: var(--accent-red); font-weight: 600;' : ''}">${text}</span>
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

// Vehicle Simulation & Hardware Sensor State Engine
let vehicleState = 'WAITING_ENTRY'; // Starts ready in front of gate
let isVehicleAnimating = false;

function startVehicleSimulation() {
    const car = document.getElementById('simVehicle');
    if (car) {
        car.classList.add('vehicle-facing-right');
        car.style.left = '40px'; // Initial approach stop line in front of barrier
    }
    setInterval(tickVehicleSimulation, 1000);
}

function tickVehicleSimulation() {
    const car = document.getElementById('simVehicle');
    if (!car || isVehicleAnimating) return;

    const currentBarrierState = (gateStates.entry || 'CLOSED').toUpperCase();

    switch (vehicleState) {
        case 'APPROACHING_ENTRY':
            isVehicleAnimating = true;
            car.classList.add('vehicle-facing-right');
            car.style.left = '40px'; // Smoothly stops at the gate
            setTimeout(() => {
                vehicleState = 'WAITING_ENTRY';
                isVehicleAnimating = false;
            }, 2100);
            break;

        case 'WAITING_ENTRY':
            // Only proceeds once the barrier is actually OPEN (via sensor or manual button)
            if (currentBarrierState === 'OPEN') {
                isVehicleAnimating = true;
                car.classList.add('vehicle-facing-right');
                car.style.left = '320px'; // Smoothly drives into the campus
                setTimeout(() => {
                    vehicleState = 'PARKED';
                    isVehicleAnimating = false;
                }, 2100);
            }
            break;

        case 'PARKED':
            break;
    }
}

/**
 * Hardware Sensor / Camera Integration Hooks:
 * When ESP32 / camera ultrasonic sensor detects a vehicle physically at the gate,
 * call window.onHardwareVehicleDetected() to seamlessly trigger live visual response.
 */
window.onHardwareVehicleDetected = function() {
    const car = document.getElementById('simVehicle');
    if (!car) return;
    vehicleState = 'APPROACHING_ENTRY';
    tickVehicleSimulation();
};

window.onHardwareVehicleCleared = function() {
    const car = document.getElementById('simVehicle');
    if (!car) return;
    car.classList.add('vehicle-facing-right');
    car.style.left = '-60px';
    vehicleState = 'APPROACHING_ENTRY';
    tickVehicleSimulation();
};

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
    const tokenInput = document.getElementById('gateControlToken');
    if (tokenInput) tokenInput.addEventListener('input', () => updateGateControlButtons());
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
