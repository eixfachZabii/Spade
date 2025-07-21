import React, { useState } from "react";

/**
 * Component containing login and registration forms
 *
 * @param {Object} props Component props
 * @param {function} props.onLogin Function to handle login submission
 * @param {function} props.onRegister Function to handle registration submission
 * @param {boolean} props.isLoading Loading state for form submission
 * @returns {JSX.Element} AuthForms component
 */
const AuthForms = ({ onLogin, onRegister, isLoading }) => {
  const [showLoginForm, setShowLoginForm] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");

  // Handle login form submission
  const handleLogin = (e) => {
    e.preventDefault();
    onLogin({ username, password });
  };

  // Handle registration form submission
  const handleRegister = (e) => {
    e.preventDefault();
    onRegister({ username, password, email });
  };

  return (
    <>
      <div className="auth-tabs">
        <button
          className={`auth-tab ${showLoginForm ? 'active' : ''}`}
          onClick={() => setShowLoginForm(true)}
        >
          Login
        </button>
        <button
          className={`auth-tab ${!showLoginForm ? 'active' : ''}`}
          onClick={() => setShowLoginForm(false)}
        >
          Register
        </button>
      </div>

      {showLoginForm ? (
        // Login Form
        <form onSubmit={handleLogin} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            className="primary-button"
            disabled={isLoading}
          >
            {isLoading ? "Logging in..." : "Login"}
          </button>
        </form>
      ) : (
        // Registration Form
        <form onSubmit={handleRegister} className="register-form">
          <div className="form-group">
            <label htmlFor="reg-username">Username</label>
            <input
              type="text"
              id="reg-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Choose a username"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-email">Email</label>
            <input
              type="email"
              id="reg-email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-password">Password</label>
            <input
              type="password"
              id="reg-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Create a password"
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            className="primary-button"
            disabled={isLoading}
          >
            {isLoading ? "Registering..." : "Create Account"}
          </button>
        </form>
      )}

      <div className="login-footer">
        <p>
          {showLoginForm
            ? "Don't have an account? Click Register to create one."
            : "Already have an account? Click Login to sign in."}
        </p>
      </div>
    </>
  );
};

export default AuthForms;