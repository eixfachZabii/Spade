import React, { useState, useRef } from "react";
import { FaCamera, FaExclamationTriangle } from "react-icons/fa";
import ApiService from "../services/ApiService";
import AuthForms from "../components/auth/AuthForms";

/**
 * Profile page component for user authentication and profile management
 *
 * @param {Object} props Component props
 * @param {Object} props.user Current user data
 * @param {function} props.onLogin Function to handle login
 * @param {function} props.onLogout Function to handle logout
 * @param {function} props.navigateToHome Function to navigate to home page
 * @returns {JSX.Element} ProfilePage component
 */
function ProfilePage({ user, onLogin, onLogout, navigateToHome }) {
  // State for form fields
  const [username, setUsername] = useState(user?.username || "");
  const [email, setEmail] = useState(user?.email || "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // State for avatar
  const [avatar, setAvatar] = useState(user?.avatar || "default");
  const [customAvatar, setCustomAvatar] = useState(user?.customAvatar || null);

  // State for UI feedback
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  // State for views
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const fileInputRef = useRef(null);

  // Available avatar options
  const availableAvatars = [
    "default",
    "player1",
    "player2",
    "player3",
    "player4",
  ];

  // Function to handle login
  const handleLogin = async (credentials) => {
    setError("");
    setIsLoading(true);

    try {
      // Call the login API
      const loginResponse = await ApiService.login(credentials);

      // Get user details after login
      const userData = loginResponse.user;

      // Process avatar data if it exists
      let processedAvatarData = null;
      if (userData.avatarBase64) {
        processedAvatarData = `data:image/jpeg;base64,${userData.avatarBase64}`;
      }

      // Create proper user object with processed avatar
      const userWithProcessedAvatar = {
        ...userData,
        avatar: processedAvatarData ? null : "default",
        customAvatar: processedAvatarData,
      };

      onLogin(userWithProcessedAvatar);

      setSuccessMessage("Login successful!");
      setTimeout(() => {
        setSuccessMessage("");
        navigateToHome();
      }, 1500);
    } catch (error) {
      setError(error.message || "Login failed. Please check your credentials.");
    } finally {
      setIsLoading(false);
    }
  };

  // Function to handle registration
  const handleRegister = async (userData) => {
    setError("");
    setIsLoading(true);

    try {
      // Register the user
      await ApiService.register(userData);

      // Login with the new credentials
      const loginResponse = await ApiService.login({
        username: userData.username,
        password: userData.password,
      });

      // Process avatar data for new user (should have default)
      const newUserData = loginResponse.user;
      onLogin({
        ...newUserData,
        avatar: "default",
        customAvatar: null,
      });

      setSuccessMessage("Registration successful!");
      setTimeout(() => {
        setSuccessMessage("");
        navigateToHome();
      }, 1500);
    } catch (error) {
      setError(error.message || "Registration failed. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  // Function to update user profile
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    if (!username || !email) {
      setError("Username and email are required");
      setIsLoading(false);
      return;
    }

    try {
      // Update user profile
      const updatedUser = await ApiService.updateUser({
        username,
        email,
      });

      // If there was a custom avatar before, we need to keep it
      // since the updatedUser object might not include avatar data
      const userWithAvatar = {
        ...updatedUser,
        avatar: avatar,
        customAvatar: customAvatar,
      };

      onLogin(userWithAvatar);

      setIsEditing(false);
      setSuccessMessage("Profile updated successfully!");
      setTimeout(() => setSuccessMessage(""), 2000);
    } catch (error) {
      setError(error.message || "Failed to update profile");
    } finally {
      setIsLoading(false);
    }
  };

  // Function to handle password change
  const handlePasswordChange = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setError("All password fields are required");
      setIsLoading(false);
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("New passwords do not match");
      setIsLoading(false);
      return;
    }

    try {
      // Call API to update password
      await ApiService.updatePassword({
        currentPassword,
        newPassword,
      });

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setIsChangingPassword(false);
      setSuccessMessage("Password updated successfully!");
      setTimeout(() => setSuccessMessage(""), 2000);
    } catch (error) {
      setError(error.message || "Failed to update password");
    } finally {
      setIsLoading(false);
    }
  };

  // Handle image upload
  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Check if the file is an image
    if (!file.type.match("image.*")) {
      setError("Please select an image file");
      return;
    }

    // Check file size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
      setError("Image size should be less than 2MB");
      return;
    }

    setIsLoading(true);

    try {
      // Show local preview immediately for better UX
      const localPreviewUrl = URL.createObjectURL(file);
      setCustomAvatar(localPreviewUrl);

      // Upload to server if logged in
      if (user) {
        const formData = new FormData();
        formData.append("avatar", file);

        const updatedUser = await ApiService.uploadAvatar(formData);

        // Process avatar data from response
        let processedAvatarData = null;

        // Check for the base64 encoded avatar
        if (updatedUser.avatarBase64) {
          processedAvatarData = `data:image/jpeg;base64,${updatedUser.avatarBase64}`;
        }

        onLogin({
          ...updatedUser,
          avatar: null, // Using custom avatar
          customAvatar: processedAvatarData || localPreviewUrl, // Use processed data or local preview
        });

        setSuccessMessage("Profile picture updated!");
        setTimeout(() => setSuccessMessage(""), 2000);

        // If we successfully got processed data, clean up the local object URL
        if (processedAvatarData) {
          URL.revokeObjectURL(localPreviewUrl);
        }
      }
    } catch (error) {
      setError("Failed to upload avatar: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
    }
  };

  // Function to trigger file input
  const triggerFileInput = () => {
    fileInputRef.current.click();
  };

  // Function to remove custom avatar
  const removeCustomAvatar = async () => {
    setCustomAvatar(null);

    if (user) {
      try {
        setIsLoading(true);
        // This should be implemented on your backend - for now we'll just update the local state
        onLogin({
          ...user,
          customAvatar: null,
          avatar: "default",
        });

        setSuccessMessage("Profile picture removed!");
        setTimeout(() => setSuccessMessage(""), 2000);
      } catch (error) {
        setError("Failed to remove avatar");
      } finally {
        setIsLoading(false);
      }
    }
  };

  // If user is not logged in, show login/register form
  if (!user) {
    return (
      <div className="profile-container">
        <div className="profile-card">
          {error && <div className="error-message">{error}</div>}
          {successMessage && (
            <div className="success-message">{successMessage}</div>
          )}

          <AuthForms
            onLogin={handleLogin}
            onRegister={handleRegister}
            isLoading={isLoading}
          />
        </div>
      </div>
    );
  }

  // User Profile View
  return (
    <div className="profile-container">
      <div className="profile-card">
        <h2>Player Profile</h2>

        {error && <div className="error-message">{error}</div>}
        {successMessage && (
          <div className="success-message">{successMessage}</div>
        )}

        <div className="profile-header">
          <div className="avatar-container">
            <div
              className={`avatar ${
                customAvatar ? "custom" : `avatar-${avatar}`
              }`}
              style={
                customAvatar ? { backgroundImage: `url(${customAvatar})` } : {}
              }
            >
              {!customAvatar && user.username.charAt(0).toUpperCase()}

              <div className="avatar-overlay" onClick={triggerFileInput}>
                <FaCamera className="camera-icon" />
                <span>Change</span>
              </div>
            </div>

            <input
              type="file"
              ref={fileInputRef}
              onChange={handleImageUpload}
              accept="image/*"
              className="file-input"
              disabled={isLoading}
            />
          </div>

          {!isEditing && !isChangingPassword ? (
            <div className="profile-info">
              <h3>{user.username}</h3>
              <p>{user.email}</p>
            </div>
          ) : null}
        </div>

        {isEditing ? (
          <form onSubmit={handleSaveProfile} className="edit-profile-form">
            <div className="form-group">
              <label htmlFor="edit-username">Username</label>
              <input
                type="text"
                id="edit-username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="edit-email">Email</label>
              <input
                type="email"
                id="edit-email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={isLoading}
              />
            </div>

            {!customAvatar && (
              <div className="form-group">
                <label>Select Avatar</label>
                <div className="avatar-selector">
                  {availableAvatars.map((avatarOption) => (
                    <div
                      key={avatarOption}
                      className={`avatar avatar-${avatarOption} ${
                        avatar === avatarOption ? "selected" : ""
                      }`}
                      onClick={() => setAvatar(avatarOption)}
                    >
                      {username.charAt(0).toUpperCase()}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {customAvatar && (
              <div className="form-group">
                <label>Profile Picture</label>
                <div className="custom-avatar-preview">
                  <img src={customAvatar} alt="Custom avatar" />
                  <button
                    type="button"
                    className="remove-avatar-btn"
                    onClick={removeCustomAvatar}
                    disabled={isLoading}
                  >
                    Remove and use default
                  </button>
                </div>
              </div>
            )}

            <div className="form-buttons">
              <button
                type="submit"
                className="primary-button"
                disabled={isLoading}
              >
                {isLoading ? "Saving..." : "Save Changes"}
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setIsEditing(false)}
                disabled={isLoading}
              >
                Cancel
              </button>
            </div>
          </form>
        ) : isChangingPassword ? (
          <form onSubmit={handlePasswordChange} className="edit-profile-form">
            <div className="form-group">
              <label htmlFor="current-password">Current Password</label>
              <input
                type="password"
                id="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="new-password">New Password</label>
              <input
                type="password"
                id="new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirm-password">Confirm New Password</label>
              <input
                type="password"
                id="confirm-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div className="form-buttons">
              <button
                type="submit"
                className="primary-button"
                disabled={isLoading}
              >
                {isLoading ? "Updating..." : "Update Password"}
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setIsChangingPassword(false)}
                disabled={isLoading}
              >
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <>
            <div className="stats-container">
              <div className="stat-item">
                <span className="stat-value">
                  {user.balance?.toLocaleString() || 1000}
                </span>
                <span className="stat-label">Chips</span>
              </div>
              <div className="stat-item">
                <span className="stat-value">{user.gamesPlayed || 0}</span>
                <span className="stat-label">Games</span>
              </div>
              <div className="stat-item">
                <span className="stat-value">{user.wins || 0}</span>
                <span className="stat-label">Wins</span>
              </div>
            </div>

            <div className="profile-actions">
              <button
                className="primary-button"
                onClick={() => setIsEditing(true)}
                disabled={isLoading}
              >
                Edit Profile
              </button>
              <button
                className="secondary-button"
                onClick={() => setIsChangingPassword(true)}
                disabled={isLoading}
              >
                Change Password
              </button>
              <button
                className="secondary-button danger"
                onClick={onLogout}
                disabled={isLoading}
              >
                Logout
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default ProfilePage;