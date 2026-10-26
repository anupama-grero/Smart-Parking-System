const endpoints = {
    parking: "/api/parking/status",
    active: "/api/parking-sessions/active",
    history: "/api/parking-sessions/history",
    gates: "/api/gates/status"
};

const byId = (id) => document.getElementById(id);
const safeArray = (value) => Array.isArray(value) ? value : [];
const gateStates = { ENTRANCE: "unknown", EXIT: "unknown" };
let controllerOnline = false;
let gateCommandInProgress = false;

function setText(id, value) {
    byId(id).textContent = value;
}

function formatTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit" }).format(date);
}

function formatDateTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(date);
}

function makeCell(content, className) {
    const cell = document.createElement("td");
    if (className) cell.className = className;
    if (content instanceof Node) cell.append(content);
    else cell.textContent = content ?? "—";
    return cell;
}

function makePersonCell(driver) {
    const wrapper = document.createElement("div");
    wrapper.className = "person-cell";
    const name = document.createElement("strong");
    name.textContent = driver?.name || "Unknown driver";
    const identifier = document.createElement("span");
    identifier.textContent = driver?.driverId || "No driver ID";
    wrapper.append(name, identifier);
    return wrapper;
}

function renderSlots(slots) {
    const list = byId("slot-list");
    list.replaceChildren();
    if (!slots.length) {
        const empty = document.createElement("div");
        empty.className = "loading-row";
        empty.textContent = "No parking spaces have been registered.";
        list.append(empty);
        return;
    }

    slots.forEach((slot) => {
        const occupied = slot.isOccupied === true;
        const item = document.createElement("div");
        item.className = "slot-item";
        const number = document.createElement("span");
        number.className = "slot-number";
        number.textContent = String(slot.slotId ?? "—").padStart(2, "0");
        const copy = document.createElement("span");
        copy.className = "slot-copy";
        const category = document.createElement("strong");
        category.textContent = slot.category || "General parking";
        const sensor = document.createElement("small");
        sensor.textContent = slot.lastSensorUpdate
            ? `${slot.lastDistanceCm != null ? `${slot.lastDistanceCm} cm - ` : ""}Updated ${formatTime(slot.lastSensorUpdate)}`
            : "No sensor reading yet";
        copy.append(category, sensor);
        const status = document.createElement("span");
        status.className = `slot-status ${occupied ? "is-occupied" : ""}`;
        status.textContent = occupied ? "Occupied" : "Available";
        item.append(number, copy, status);
        list.append(item);
    });
}

function renderSessions(sessions) {
    const body = byId("active-sessions-body");
    body.replaceChildren();
    setText("active-count", sessions.length);
    setText("active-session-pill", `${sessions.length} active`);
    if (!sessions.length) {
        const row = document.createElement("tr");
        row.append(makeCell("No active parking sessions.", "empty-cell"));
        row.firstChild.colSpan = 4;
        body.append(row);
        return;
    }

    sessions.slice(0, 8).forEach((session) => {
        const row = document.createElement("tr");
        const vehicle = document.createElement("strong");
        vehicle.className = "vehicle-cell";
        vehicle.textContent = session.driver?.vehicleNumber || "—";
        const space = document.createElement("span");
        space.className = "space-tag";
        space.textContent = session.assignedSlot != null ? `Space ${String(session.assignedSlot).padStart(2, "0")}` : "Unassigned";
        row.append(
            makeCell(makePersonCell(session.driver)),
            makeCell(vehicle),
            makeCell(space),
            makeCell(formatTime(session.entryTime))
        );
        body.append(row);
    });
}

function renderActivity(sessions) {
    const body = byId("activity-body");
    body.replaceChildren();
    if (!sessions.length) {
        const row = document.createElement("tr");
        row.append(makeCell("No session history available.", "empty-cell"));
        row.firstChild.colSpan = 4;
        body.append(row);
        return;
    }

    sessions.slice(0, 6).forEach((session) => {
        const row = document.createElement("tr");
        const status = document.createElement("span");
        const statusText = String(session.status || "unknown").toLowerCase();
        status.className = `activity-status ${statusText === "completed" ? "is-completed" : ""}`;
        status.textContent = statusText;
        row.append(
            makeCell(makePersonCell(session.driver)),
            makeCell(session.assignedSlot != null ? `Space ${String(session.assignedSlot).padStart(2, "0")}` : "—"),
            makeCell(status),
            makeCell(formatDateTime(session.exitTime || session.entryTime))
        );
        body.append(row);
    });
}

function setGateState(elementId, value) {
    const element = byId(elementId);
    const state = String(value || "Unknown").trim();
    element.textContent = state.toLowerCase();
    element.className = "gate-state";
    if (state.toLowerCase() === "closed") element.classList.add("is-closed");
    else if (["offline", "error", "unknown"].includes(state.toLowerCase())) element.classList.add("is-offline");
    return state.toUpperCase();
}

function updateGateControls() {
    document.querySelectorAll(".gate-control-button").forEach((button) => {
        const gate = button.dataset.gate;
        const action = button.dataset.action;
        button.disabled = !controllerOnline
            || gateCommandInProgress
            || gateStates[gate] === action;
    });
}

function updateActiveNavigation() {
    const sections = [...document.querySelectorAll(".side-nav .nav-link")]
        .map((link) => ({ link, section: document.querySelector(link.hash) }))
        .filter(({ section }) => section);
    const marker = window.scrollY + 110;
    let active = sections[0];
    sections.forEach((item) => {
        if (item.section.offsetTop <= marker) active = item;
    });

    sections.forEach(({ link }) => {
        const isActive = link === active?.link;
        link.classList.toggle("is-active", isActive);
        if (isActive) link.setAttribute("aria-current", "location");
        else link.removeAttribute("aria-current");
    });
}

function renderGateStatus(gates) {
    gateStates.ENTRANCE = setGateState("entry-gate-status", gates.entryGate);
    gateStates.EXIT = setGateState("exit-gate-status", gates.exitGate);
    controllerOnline = String(gates.esp32Status || "").toLowerCase().startsWith("online");
    const health = byId("gate-health");
    health.classList.toggle("is-offline", !controllerOnline);
    health.replaceChildren();
    const dot = document.createElement("i");
    health.append(dot, document.createTextNode(controllerOnline ? "Online" : gates.esp32Status || "Offline"));
    const controller = byId("controller-status");
    controller.textContent = gates.esp32Status || "Unknown";
    controller.classList.toggle("is-offline", !controllerOnline);
    setText("controller-message", gates.message || "ESP32 connection status");
    updateGateControls();
}

async function controlGate(button) {
    const gate = button.dataset.gate;
    const action = button.dataset.action;
    if (!gate || !action || !["ENTRANCE", "EXIT"].includes(gate) || !["OPEN", "CLOSE"].includes(action)) {
        return;
    }
    if (action === "OPEN" && !window.confirm(`Open the ${gate.toLowerCase()} gate?`)) {
        return;
    }

    gateCommandInProgress = true;
    updateGateControls();
    const message = byId("gate-action-message");
    message.className = "gate-action-message";
    message.textContent = `${action === "OPEN" ? "Opening" : "Closing"} ${gate.toLowerCase()} gate…`;

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { Accept: "application/json", "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        const response = await fetch("/api/gates/control", {
            method: "POST",
            headers,
            body: JSON.stringify({ gate, action })
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || `Gate command failed (${response.status})`);
        }

        renderGateStatus(result);
        message.textContent = result.message || `${gate.toLowerCase()} gate ${action.toLowerCase()} command completed.`;
        await refreshDashboard();
    } catch (error) {
        message.classList.add("is-error");
        message.textContent = `Gate command failed: ${error.message}`;
    } finally {
        gateCommandInProgress = false;
        updateGateControls();
    }
}

async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) throw new Error(`${url} returned ${response.status}`);
    return response.json();
}

async function refreshDashboard() {
    const button = byId("refresh-button");
    const connection = document.querySelector(".live-indicator");
    button.disabled = true;
    const results = await Promise.allSettled(Object.entries(endpoints).map(async ([key, url]) => [key, await fetchJson(url)]));
    const data = {};
    const failures = [];
    results.forEach((result, index) => {
        if (result.status === "fulfilled") data[result.value[0]] = result.value[1];
        else failures.push(Object.keys(endpoints)[index]);
    });

    if (data.parking) {
        const slots = safeArray(data.parking).slice().sort((a, b) => Number(a.slotId) - Number(b.slotId));
        const occupied = slots.filter((slot) => slot.isOccupied === true).length;
        const available = slots.filter((slot) => slot.isOccupied === false).length;
        const unknown = slots.length - occupied - available;
        const percentage = slots.length ? Math.round((occupied / slots.length) * 100) : 0;
        setText("occupied-count", occupied);
        setText("capacity-count", `/ ${slots.length} spaces`);
        setText("available-count", available);
        setText("occupancy-percent", `${percentage}%`);
        setText("ring-percent", `${percentage}%`);
        setText("legend-occupied", occupied);
        setText("legend-available", available);
        setText("legend-unknown", unknown);
        setText("slot-count-label", `${slots.length} spaces`);
        byId("occupancy-bar").style.width = `${percentage}%`;
        byId("occupancy-ring").style.background = `conic-gradient(#2563eb 0deg ${percentage * 3.6}deg, #e2e8f0 ${percentage * 3.6}deg 360deg)`;
        byId("occupancy-ring").setAttribute("aria-label", `${percentage}% of parking spaces occupied`);
        renderSlots(slots);
    }
    if (data.active) renderSessions(safeArray(data.active));
    if (data.history) {
        const history = safeArray(data.history).slice().sort((a, b) => new Date(b.exitTime || b.entryTime) - new Date(a.exitTime || a.entryTime));
        renderActivity(history);
    }
    if (data.gates) {
        renderGateStatus(data.gates);
    }

    connection.classList.toggle("is-error", failures.length > 0);
    setText("connection-label", failures.length ? `${4 - failures.length}/4 services connected` : "Live data");
    setText("last-updated", `Updated ${new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit", second: "2-digit" }).format(new Date())}`);
    button.disabled = false;
}

setText("today-date", new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" }).format(new Date()));
byId("refresh-button").addEventListener("click", refreshDashboard);
window.addEventListener("scroll", updateActiveNavigation, { passive: true });
window.addEventListener("resize", updateActiveNavigation);
updateActiveNavigation();
document.querySelectorAll(".gate-control-button").forEach((button) => {
    button.addEventListener("click", () => controlGate(button));
});
refreshDashboard();
window.setInterval(refreshDashboard, 10000);