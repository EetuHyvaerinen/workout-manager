const API_BASE = 'http://localhost:8444/api/dashboard';

const loadingEl    = document.getElementById('loading');
const dashboardEl  = document.getElementById('dashboard');
const loggerSelect = document.getElementById('loggerSelect');
const levelSelect  = document.getElementById('levelSelect');
const logContent   = document.getElementById('logContent');

const totalUsersEl    = document.getElementById('totalUsers');
const totalWorkoutsEl = document.getElementById('totalWorkouts');
const activeUsers24hEl = document.getElementById('activeUsers24h');
const workoutsTodayEl = document.getElementById('workoutsToday');

const uptimeEl     = document.getElementById('uptime');
const memoryEl     = document.getElementById('memory');
const databaseEl   = document.getElementById('database');
const logQueueEl   = document.getElementById('logQueue');
const logDroppedEl = document.getElementById('logDropped');

function formatUptime(seconds) {
  const hrs  = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  return `${hrs}h ${mins}m ${secs}s`;
}

async function fetchStats() {
  const res = await fetch(`${API_BASE}/stats`, { credentials: 'include' });
  const json = await res.json();
  return json.data;
}

async function loadDashboard() {
  try {
    const stats = await fetchStats();

    totalUsersEl.textContent    = stats.totalUsers;
    totalWorkoutsEl.textContent = stats.totalWorkouts;
    activeUsers24hEl.textContent = stats.activeUsers24h;
    workoutsTodayEl.textContent = stats.workoutsToday;

    const s = stats.serverStats;

    uptimeEl.textContent   = `Uptime: ${formatUptime(s.uptimeSeconds)}`;
    memoryEl.textContent   = `Memory: ${s.usedMemoryMB} / ${s.totalMemoryMB} MB`;
    databaseEl.textContent = `Database: ${stats.databaseStats.healthy ? 'Healthy' : 'Unhealthy'}`;

    logQueueEl.textContent = `Log Queue: ${s.loggerQueueSize} / ${s.loggerCapacity}`;
    const dropped = s.droppedLogEvents;
    logDroppedEl.textContent = `Dropped Log Events: ${dropped}`;
    logDroppedEl.style.color = dropped > 0 ? 'crimson' : 'inherit';

    loadingEl.hidden   = true;
    dashboardEl.hidden = false;

    await loadLogs();
  } catch (err) {
    loadingEl.textContent = 'Failed to load dashboard';
    console.error(err);
  }
}

async function loadLogs() {
  const logger = loggerSelect.value || 'Server';
  const level  = levelSelect.value  || 'ALL';
  const url    = `${API_BASE}/logs?logger=${encodeURIComponent(logger)}&level=${level}&lines=100`;

  try {
    const res  = await fetch(url, { credentials: 'include' });
    const json = await res.json();

    if (!json.data) throw new Error('No data in response');
    const data = json.data;

    if (loggerSelect.options.length <= 1) {
      loggerSelect.innerHTML = '';
      data.availableLogFiles.forEach(name => {
        const option = document.createElement('option');
        const parts  = name.split('.');
        option.value       = name;
        option.textContent = parts[parts.length - 1];
        if (name === data.logFile) option.selected = true;
        loggerSelect.appendChild(option);
      });
    }

    logContent.textContent = data.logs.join('\n');
    logContent.scrollTop   = 0;

  } catch (err) {
    logContent.textContent = 'Failed to load logs: ' + err.message;
    console.error('Log load error:', err);
  }
}

levelSelect.addEventListener('change', loadLogs);
loggerSelect.addEventListener('change', loadLogs);

loadDashboard();