const workoutsList = document.getElementById('workouts-list');
const selectedDayh2 = document.getElementById('selected-day-h2');
const exerciseForm = document.getElementById('exercise-form');
const exerciseInput = document.getElementById('exercise-name');
const exercisesContainer = document.getElementById('exercises-container');
const createWorkoutBtn = document.getElementById('create-workout');
const weekdaysContainer = document.querySelector('.weekdays');
const daySlots = document.querySelectorAll('.calendar-day');
const workoutNameInput = document.getElementById('workout-name-input');
const customModal = document.getElementById('custom-modal');
const modalConfirmBtn = document.getElementById('modal-confirm');
const modalCancelBtn = document.getElementById('modal-cancel');

let exercises = [];
let allWorkouts = [];
let allPlans = []; 
let selectedDate = new Date();
let activePlanId = null; 

function init() {
  fetchWorkouts();
  checkUserStatus();
  setupEventListeners();
}

function logout() {
  fetch('http://localhost:8444/api/logout', {
    method: 'POST',
    credentials: 'include' 
  })
  .then(() => {
    window.location.href = '/workoutmanager/login.html';
  })
  .catch(err => console.error("Logout failed", err));
}

function checkUserStatus() {
  fetch('http://localhost:8444/api/user/me', {
    method: 'GET',
    credentials: 'include'
  })
  .then(response => response.json())
  .then(data => {
    if (data.data && data.data.isAdmin) {
      const adminLink = document.getElementById('admin-link');
      if (adminLink) {
        adminLink.style.display = 'inline-block';
      }
    }
  })
  .catch(err => console.error("Could not verify admin status", err));
}

function fetchWorkouts() {
  const dateParam = selectedDate.toISOString();
  
  Promise.all([
    fetch(`http://localhost:8444/api/workout?date=${dateParam}`, { credentials: 'include' }),
    fetch(`http://localhost:8444/api/workout/plans`, { credentials: 'include' })
  ])
  .then(async ([resHistory, resPlans]) => {
    if (resHistory.status === 401) {
      window.location.href = '/workoutmanager/login.html';
      return;
    }
    const historyData = await resHistory.json();
    const plansData = await resPlans.json();

    allWorkouts = historyData.data || [];
    allPlans = plansData.data || [];

    displayWorkoutsForSelectedDay();
    createCalendar();
  })
  .catch(error => console.error('Error fetching data:', error));
}

function createCalendar() {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();

  const firstDay = new Date(year, month, 1).getDay();
  const startingDayIndex = (firstDay + 6) % 7;
  const totalDaysInMonth = new Date(year, month + 1, 0).getDate();

  const workoutDays = new Set(
    allWorkouts
      .map(w => new Date(w.createdAt))
      .filter(d => d.getFullYear() === year && d.getMonth() === month)
      .map(d => d.getDate())
  );

  const plannedDays = new Set(
    allPlans
      .filter(p => p.status !== 'MISSED')
      .map(p => new Date(p.activateTime))
      .filter(d => d.getFullYear() === year && d.getMonth() === month)
      .map(d => d.getDate())
  );

  const missedDays = new Set(
    allPlans
      .filter(p => p.status === 'MISSED')
      .map(p => new Date(p.activateTime))
      .filter(d => d.getFullYear() === year && d.getMonth() === month)
      .map(d => d.getDate())
  );

  daySlots.forEach((slot, index) => {
    slot.className = 'calendar-day';
    slot.textContent = '';
    slot.removeAttribute('data-day');

    const dayNumber = index - startingDayIndex + 1;

    if (dayNumber > 0 && dayNumber <= totalDaysInMonth) {
      slot.textContent = dayNumber;
      slot.setAttribute('data-day', dayNumber);

      if (dayNumber === today.getDate() && month === today.getMonth()) slot.classList.add('today');
      if (dayNumber === selectedDate.getDate() && month === selectedDate.getMonth()) slot.classList.add('selected');
      
      if (workoutDays.has(dayNumber)) {
        slot.classList.add('workout');
      } else if (missedDays.has(dayNumber)) {
        slot.classList.add('missed');
      } else if (plannedDays.has(dayNumber)) {
        slot.classList.add('planned');
      }
    } else {
      slot.classList.add('empty');
    }
  });
}

function setupEventListeners() {
  weekdaysContainer.addEventListener('click', (e) => {
    const slot = e.target.closest('.calendar-day');
    if (!slot || slot.classList.contains('empty')) return;

    const day = parseInt(slot.getAttribute('data-day'));
    selectedDate = new Date(new Date().getFullYear(), new Date().getMonth(), day);

    document.getElementById('create-workout-section').classList.add('active');
    createCalendar();
    displayWorkoutsForSelectedDay();
  });

  exerciseForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = exerciseInput.value.trim();
    if (!name) return;
    addExerciseUI(name);
    exerciseInput.value = '';
  });
}

function displayWorkoutsForSelectedDay() {
  selectedDayh2.innerHTML = `<div>${selectedDate.toDateString()}</div>`;
  workoutsList.innerHTML = '';

  const dateStr = selectedDate.toDateString();
  const filteredHistory = allWorkouts.filter(w => new Date(w.createdAt).toDateString() === dateStr);
  const filteredPlans = allPlans.filter(p => new Date(p.activateTime).toDateString() === dateStr);

  if (filteredHistory.length === 0 && filteredPlans.length === 0) {
    workoutsList.innerHTML = `<div class="no-workouts">No workouts or plans for this day</div>`;
    return;
  }

  filteredPlans.forEach(plan => renderWorkoutItem(plan, true));
  filteredHistory.forEach(workout => renderWorkoutItem(workout, false));
}

function renderWorkoutItem(data, isPlan) {
  const newDiv = document.createElement('div');
  
  let itemClasses = 'workout-item';
  if (isPlan) {
    itemClasses += ' planned-item';
    if (data.status === 'MISSED') {
      itemClasses += ' missed-plan';
    }
  }
  newDiv.className = itemClasses;

  const grouped = {};
  data.exercises.forEach(ex => {
    if (!grouped[ex.name]) grouped[ex.name] = [];
    grouped[ex.name].push(ex);
  });

  let actions = '';
  if (isPlan) {
    if (data.status === 'MISSED') {
      actions = `
        <input type="date" id="resched-${data.id}" style="display:none" onchange="reschedulePlan('${data.id}', this.value)">
        <button class="btn reschedule-btn" onclick="document.getElementById('resched-${data.id}').showPicker()">Reschedule</button>
        <button class="btn delete-x" onclick="deletePlan('${data.id}')">X</button>
      `;
    } else {
      actions = `
        <button class="btn start-plan" onclick="startPlannedWorkout('${data.id}')">Start</button>
        <button class="btn delete-x" onclick="deletePlan('${data.id}')">X</button>
      `;
    }
  } else {
    actions = `<button class="btn delete-x" onclick="deleteWorkout('${data.id}')">X</button>`;
  }

  newDiv.innerHTML = `
    <div class="workout-name-container">
      <div class="workout-name">
        ${isPlan && data.status === 'MISSED' ? '<span class="missed-label">Missed: </span>' : ''}
        ${data.name}
      </div>
      <div class="action-group">${actions}</div>
    </div>
    <div class="workout-info">
      ${Object.entries(grouped).map(([name, sets]) => `
        <div class="exercise-info">
          <strong>${name}</strong>
          ${sets.map(s => `
            <div class="set-container">
              <div>${s.repetitions} x ${s.weight}kg</div>
            </div>
          `).join('')}
        </div>
      `).join('')}
    </div>
  `;

  workoutsList.appendChild(newDiv);
}

window.reschedulePlan = function(id, dateValue) {
  if (!dateValue) return;
  const isoDate = new Date(dateValue).toISOString();
  fetch(`https://hyvaerinen.com:8444/api/workout/plans/reschedule?planId=${id}&date=${isoDate}`, {
    method: 'POST',
    credentials: 'include'
  })
  .then(response => {
    if (response.ok) {
      fetchWorkouts(); 
    } else {
      console.error("Failed to reschedule workout");
    }
  })
  .catch(err => console.error("Error during rescheduling:", err));
};

window.startPlannedWorkout = function(planId) {
  const plan = allPlans.find(p => p.id === planId);
  if (!plan) return;

  exercises = [];
  exercisesContainer.innerHTML = '';
  activePlanId = planId;

  const grouped = {};
  plan.exercises.forEach(ex => {
    if (!grouped[ex.name]) grouped[ex.name] = [];
    grouped[ex.name].push(ex);
  });

  Object.entries(grouped).forEach(([name, sets]) => {
    addExerciseUI(name);
    const currentEx = exercises[exercises.length - 1];
    sets.forEach(s => {
      currentEx.sets.push({ repetitions: s.repetitions, weight: s.weight });
    });
    renderSetsUI(currentEx.id);
  });

  workoutNameInput.value = plan.name;
  lockWorkoutName();
  
  document.getElementById('create-workout-section').scrollIntoView({ behavior: 'smooth' });
};

function addExerciseUI(name) {
  const id = Date.now() + Math.random(); 
  exercises.push({ id, name, sets: [] });
  const div = document.createElement('div');
  div.className = 'exercise-container';
  div.innerHTML = `
    <div class="exercise-info">
      <strong>${name}</strong>
      <div id="sets-${id}"></div>
      <div class="set-input-group">
        <input type="number" placeholder="Reps" class="s-reps">
        <input type="number" placeholder="Kg" class="s-weight">
        <button type="button" onclick="addSet(${id}, this)">+</button>
      </div>
    </div>
  `;
  exercisesContainer.appendChild(div);
  
  const repsInput = div.querySelector('.s-reps');
  const weightInput = div.querySelector('.s-weight');
  const addButton = div.querySelector('button');

  weightInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { addSet(id, addButton); repsInput.focus(); }
  });
  repsInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') weightInput.focus();
  });
  repsInput.focus();
}

window.addSet = function (id, btn) {
  const container = btn.parentElement;
  const reps = container.querySelector('.s-reps').value;
  const weight = container.querySelector('.s-weight').value;
  if (!reps || !weight) return;

  const ex = exercises.find(e => e.id === id);
  ex.sets.push({ repetitions: reps, weight: weight });
  renderSetsUI(id);

  container.querySelector('.s-reps').value = '';
  container.querySelector('.s-weight').value = '';
};

function renderSetsUI(exerciseId) {
  const ex = exercises.find(e => e.id === exerciseId);
  const setList = document.getElementById(`sets-${exerciseId}`);
  setList.innerHTML = ex.sets.map((set, index) => `
    <div class="set-container create-workout">
      <div>${set.repetitions} x ${set.weight}kg</div>
      <button class="btn delete-x" onclick="deleteSet(${exerciseId}, ${index})">X</button>
    </div>
  `).join('');
}

window.deleteSet = function(exerciseId, setIndex) {
  const ex = exercises.find(e => e.id === exerciseId);
  if (ex) {
    ex.sets.splice(setIndex, 1);
    renderSetsUI(exerciseId);
  }
};

function lockWorkoutName() {
  const name = workoutNameInput.value.trim() || "Untitled Workout";
  const nameDisplay = document.createElement('div');
  nameDisplay.className = 'locked-workout-name';
  nameDisplay.textContent = name;
  nameDisplay.onclick = () => unlockWorkoutName(nameDisplay);

  if (workoutNameInput.parentNode) {
    workoutNameInput.parentNode.replaceChild(nameDisplay, workoutNameInput);
  }
}

function unlockWorkoutName(displayElement) {
  displayElement.parentNode.replaceChild(workoutNameInput, displayElement);
  workoutNameInput.focus();
}

workoutNameInput.addEventListener('blur', () => lockWorkoutName());
workoutNameInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') { e.preventDefault(); workoutNameInput.blur(); }
});

createWorkoutBtn.addEventListener('click', () => {
    if (exercises.length === 0) return alert('Add an exercise first');
    
    const lockedName = document.querySelector('.locked-workout-name');
    const workoutName = lockedName ? lockedName.textContent : workoutNameInput.value.trim();
    
    const exercisesPayload = exercises.flatMap(ex => ex.sets.map(s => ({
        name: ex.name,
        repetitions: parseInt(s.repetitions),
        weight: parseFloat(s.weight)
    })));

    const payload = {
        name: workoutName || "Untitled Workout",
        createdAt: selectedDate.toISOString(), 
        exercises: exercisesPayload
    };

    const endpoint = activePlanId 
        ? `http://localhost:8444/api/workout/plans/complete?planId=${activePlanId}`
        : 'http://localhost:8444/api/workout';

    fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload),
    }).then(res => {
        if (res.ok) {
            exercises = [];
            activePlanId = null;
            exercisesContainer.innerHTML = '';
            if (lockedName) lockedName.parentNode.replaceChild(workoutNameInput, lockedName);
            workoutNameInput.value = ''; 
            fetchWorkouts();
        } else {
            alert("Failed to save workout.");
        }
    });
});

window.deleteWorkout = function (id) {
  customModal.classList.add('active');
  const handleConfirm = () => {
    fetch(`http://localhost:8444/api/workout?workoutId=${id}`, {
      method: 'DELETE',
      credentials: 'include',
    }).then(() => {
      closeModal();
      fetchWorkouts();
    });
  };

  const closeModal = () => {
    customModal.classList.remove('active');
    modalConfirmBtn.removeEventListener('click', handleConfirm);
  };

  modalConfirmBtn.addEventListener('click', handleConfirm, { once: true });
  modalCancelBtn.addEventListener('click', closeModal, { once: true });
};

window.deletePlan = function (id) {
  customModal.classList.add('active');
  document.getElementById('modal-message').textContent = "Do you really want to delete this planned workout?";
  
  const handleConfirm = () => {
    fetch(`http://localhost:8444/api/workout/plans?planId=${id}`, {
      method: 'DELETE',
      credentials: 'include',
    }).then(() => {
      closeModal();
      fetchWorkouts();
    });
  };

  const closeModal = () => {
    customModal.classList.remove('active');
    document.getElementById('modal-message').textContent = "Do you really want to delete this workout?";
    modalConfirmBtn.removeEventListener('click', handleConfirm);
  };

  modalConfirmBtn.addEventListener('click', handleConfirm, { once: true });
  modalCancelBtn.addEventListener('click', closeModal, { once: true });
};

init();