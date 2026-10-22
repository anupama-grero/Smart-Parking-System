const dashboardElements = {
    totalSlots: document.getElementById('total-slots'),
    availableSlots: document.getElementById('available-slots'),
    occupiedSlots: document.getElementById('occupied-slots'),
    activeSessionsCount: document.getElementById('active-sessions-count'),
    occupancyFoot: document.getElementById('occupancy-foot'),
    occupancyRing: document.getElementById('occupancy-ring'),
    occupancyPercent: document.getElementById('occupancy-percent'),
    legendOccupied: document.getElementById('legend-occupied'),
    legendAvailable: document.getElementById('legend-available'),
    slotCountLabel: document.getElementById('slot-count-label'),
    slotList: document.getElementById('slot-list'),
    deviceStatus: document.getElementById('device-status'),
    entryGateStatus: document.getElementById('entry-gate-status'),
    exitGateStatus: document.getElementById('exit-gate-status'),
    activeSessionBadge: document.getElementById('active-session-badge'),
    activeSessionsTable: document.getElementById('active-sessions-table'),
    recentActivityTable: document.getElementById('recent-activity-table'),
    lastUpdated: document.getElementById('last-updated'),
    footerUpdated: document.getElementById('footer-updated'),
    error: document.getElementById('dashboard-error')
};

function formatDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat(undefined, {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit'
    }).format(date);
}

function makeCell(text, className) {
    const cell = document.createElement('td');
    cell.textContent = text;
    if (className) cell.className = className;
    return cell;
}

function makeEmptyRow(message) {
    const row = document.createElement('tr');
    const cell = makeCell(message, 'admin-empty');
    cell.colSpan = 4;
    row.append(cell);
    return row;
}

function renderSlots(slots) {
    const occupied = slots.filter(slot => slot.occupied).length;
    const available = slots.length - occupied;
    const occupancy = slots.length ? Math.round((occupied / slots.length) * 100) : 0;

    dashboardElements.totalSlots.textContent = slots.length;
    dashboardElements.availableSlots.textContent = available;
    dashboardElements.occupiedSlots.textContent = occupied;
    dashboardElements.occupancyFoot.textContent = `${occupancy}% of campus capacity in use`;
    dashboardElements.occupancyRing.style.setProperty('--occupied-share', `${occupancy}%`);
    dashboardElements.occupancyPercent.textContent = `${occupancy}%`;
    dashboardElements.occupancyRing.setAttribute('aria-label', `${occupancy}% of parking slots occupied`);
    dashboardElements.legendOccupied.textContent = occupied;
    dashboardElements.legendAvailable.textContent = available;
    dashboardElements.slotCountLabel.textContent = `${slots.length} total`;
    dashboardElements.slotList.replaceChildren();

    if (!slots.length) {
        dashboardElements.slotList.innerHTML = '<p class="admin-empty">No parking slots are configured.</p>';
        return;
    }

    slots.forEach(slot => {
        const row = document.createElement('div');
        row.className = 'admin-slot-row';
        const number = document.createElement('span');
        number.className = 'admin-slot-number';
        number.textContent = `P${String(slot.slotNumber ?? '—').padStart(2, '0')}`;
        const category = document.createElement('span');
        category.className = 'admin-slot-category';
        category.textContent = slot.category || 'Uncategorized';
        const status = document.createElement('span');
        status.className = `admin-status-pill ${slot.occupied ? 'status-occupied' : 'status-available'}`;
        status.textContent = slot.occupied ? 'Occupied' : 'Available';
        row.append(number, category, status);
        dashboardElements.slotList.append(row);
    });
}

function renderGateStatus(gates) {
    setStatusPill(dashboardElements.deviceStatus, gates.esp32Status || 'Unknown');
    setStatusPill(dashboardElements.entryGateStatus, gates.entryGate || 'Unknown');
    setStatusPill(dashboardElements.exitGateStatus, gates.exitGate || 'Unknown');
}

function setStatusPill(element, value) {
    const normalized = value.toUpperCase();
    element.textContent = value;
    element.className = 'admin-status-pill';
    if (normalized.includes('OFFLINE') || normalized.includes('ERROR')) {
        element.classList.add('status-offline');
    } else if (normalized === 'OPEN' || normalized.includes('ONLINE')) {
        element.classList.add('status-online');
    } else if (normalized === 'CLOSED') {
        element.classList.add('status-closed');
    } else {
        element.classList.add('status-neutral');
    }
}

function renderActiveSessions(sessions) {
    dashboardElements.activeSessionsCount.textContent = sessions.length;
    dashboardElements.activeSessionBadge.textContent = `${sessions.length} active`;
    dashboardElements.activeSessionsTable.replaceChildren();

    if (!sessions.length) {
        dashboardElements.activeSessionsTable.append(makeEmptyRow('No active parking sessions.'));
        return;
    }

    sessions
        .slice()
        .sort((a, b) => new Date(b.entryTime) - new Date(a.entryTime))
        .forEach(session => {
            const row = document.createElement('tr');
            row.append(
                makeCell(session.driver?.name || session.driver?.driverId || 'Unknown driver', 'admin-driver-name'),
                makeCell(session.driver?.vehicleNumber || '—'),
                makeCell(`Slot ${session.parkingSlot?.slotNumber ?? session.assignedSlot ?? '—'}`),
                makeCell(formatDateTime(session.entryTime))
            );
            dashboardElements.activeSessionsTable.append(row);
        });
}

function renderRecentActivity(sessions) {
    dashboardElements.recentActivityTable.replaceChildren();
    const recentSessions = sessions
        .slice()
        .sort((a, b) => new Date(b.entryTime) - new Date(a.entryTime))
        .slice(0, 5);

    if (!recentSessions.length) {
        dashboardElements.recentActivityTable.append(makeEmptyRow('No parking activity yet.'));
        return;
    }

    recentSessions.forEach(session => {
        const row = document.createElement('tr');
        const status = document.createElement('span');
        const active = session.status === 'ACTIVE';
        status.className = `admin-status-pill ${active ? 'status-available' : 'status-neutral'}`;
        status.textContent = active ? 'Active' : 'Completed';
        const statusCell = document.createElement('td');
        statusCell.append(status);
        row.append(
            makeCell(session.driver?.name || session.driver?.driverId || 'Unknown driver', 'admin-driver-name'),
            makeCell(`Slot ${session.parkingSlot?.slotNumber ?? session.assignedSlot ?? '—'}`),
            makeCell(formatDateTime(session.entryTime)),
            statusCell
        );
        dashboardElements.recentActivityTable.append(row);
    });
}

async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
        throw new Error(`${url} returned ${response.status}`);
    }
    return response.json();
}

async function refreshDashboard() {
    const refreshButton = document.getElementById('refresh-dashboard');
    refreshButton.disabled = true;
    dashboardElements.error.hidden = true;

    try {
        const [slots, activeSessions, history, gates] = await Promise.all([
            fetchJson('/api/parking/slots'),
            fetchJson('/api/parking-sessions/active'),
            fetchJson('/api/parking-sessions/history'),
            fetchJson('/api/gates/status')
        ]);

        renderSlots(slots.map(slot => ({
            slotNumber: slot.slotNumber,
            category: slot.category,
            occupied: slot.occupied ?? slot.isOccupied ?? false
        })));
        renderActiveSessions(activeSessions);
        renderRecentActivity(history);
        renderGateStatus(gates);

        const updatedAt = new Date();
        const time = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit', second: '2-digit' }).format(updatedAt);
        dashboardElements.lastUpdated.textContent = `Updated ${time}`;
        dashboardElements.footerUpdated.textContent = `Last synced at ${time}`;
    } catch (error) {
        console.error('Unable to refresh the admin dashboard:', error);
        dashboardElements.error.textContent = 'Live dashboard data could not be loaded. Check that the Smart Parking server is available, then refresh.';
        dashboardElements.error.hidden = false;
        dashboardElements.footerUpdated.textContent = 'Data sync failed';
    } finally {
        refreshButton.disabled = false;
    }
}

document.getElementById('today-label').textContent = new Intl.DateTimeFormat(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric'
}).format(new Date());
document.getElementById('refresh-dashboard').addEventListener('click', refreshDashboard);
refreshDashboard();
window.setInterval(refreshDashboard, 5000);
