const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const errorMessage = document.getElementById('error-message');

if (loginForm) {
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        errorMessage.textContent = '';

        const email = loginForm.querySelector('#email').value;
        const password = loginForm.querySelector('#password').value;
        fetch('http://localhost:8444/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, password }),
        })
        .then(response => {
            console.log(response);
            if(response.ok) {
                window.location.href = '/workoutmanager';
            }
        })
        .catch(error => {
            console.error('Login error:', error);
            errorMessage.textContent = 'Invalid email or password. Please try again.';
        });
    });
}

if (registerForm) {
    console.log("yo");
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();
        errorMessage.textContent = '';

        const email = registerForm.querySelector('#email').value;
        const password = registerForm.querySelector('#password').value;

        if (password.length < 6) {
            errorMessage.textContent = 'Password must be at least 6 characters long.';
            return;
        }

        fetch('http://localhost:8444/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
        })
        .then(response => {
            if (response.status === 201) {
                alert('Registration successful! Please log in.');
                window.location.href = '/workoutmanager/login.html';
            } else if (response.status === 409) {
                errorMessage.textContent = 'An account with this email already exists.';
            } else {
                errorMessage.textContent = 'Registration failed. Please try again later.';
            }
        })
        .catch(error => {
            console.error('Registration error:', error);
            errorMessage.textContent = 'An error occurred. Please check your connection.';
        });
    });
}