// client/src/layouts/profile/index.js
import { useEffect, useState, useRef } from "react";
import { 
  Card, 
  Grid, 
  Avatar, 
  Typography, 
  Button, 
  Box, 
  Alert, 
  IconButton, 
  Tooltip,
  Divider,
  Paper,
  Badge,
  Fade,
  Zoom
} from "@mui/material";
import {
  PhotoCamera,
  Edit,
  Save,
  Cancel,
  Lock,
  Person,
  Email,
  Visibility,
  VisibilityOff,
  Delete,
  Settings,
  Security,
  AccountCircle
} from "@mui/icons-material";
import { useAuth } from "../../context/AuthContext";
import VuiBox from "../../components/VuiBox";
import VuiTypography from "../../components/VuiTypography";
import VuiInput from "../../components/VuiInput";
import VuiButton from "../../components/VuiButton";
import GradientBorder from "../../examples/GradientBorder";
import DashboardLayout from "../../examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "../../examples/Navbars/DashboardNavbar";
import Footer from "../../examples/Footer";
import radialGradient from "../../assets/theme/functions/radialGradient";
import palette from "../../assets/theme/base/colors";
import borders from "../../assets/theme/base/borders";
import ApiService from "../../services/ApiService";
import { useNavigate } from "react-router-dom";

const MAX_FILE_SIZE_MB = 20;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

function Profile() {
  const { user, logout, isAuthenticated, refreshUser } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  
  const [profileData, setProfileData] = useState({
    username: user?.username || "",
    email: user?.email || "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  
  const [profileImage, setProfileImage] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
  const [message, setMessage] = useState({ type: "", text: "" });
  const [passwordMessage, setPasswordMessage] = useState({ type: "", text: "" });
  const [loading, setLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [showPassword, setShowPassword] = useState({
    current: false,
    new: false,
    confirm: false
  });
  const [editMode, setEditMode] = useState(false);

  // Redirect to login page if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/authentication/sign-in");
    }
  }, [isAuthenticated, navigate]);

  // Update profile data when user changes
  useEffect(() => {
    if (user) {
      setProfileData(prev => ({
        ...prev,
        username: user.username || "",
        email: user.email || ""
      }));
    }
  }, [user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleImageUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.size > MAX_FILE_SIZE_BYTES) {
        setMessage({
          type: "error",
          text: `File size must be less than ${MAX_FILE_SIZE_MB}MB`
        });
        return;
      }

      if (!file.type.startsWith('image/')) {
        setMessage({
          type: "error",
          text: "Please select a valid image file"
        });
        return;
      }

      setProfileImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImage(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRemoveImage = () => {
    setProfileImage(null);
    setPreviewImage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const togglePasswordVisibility = (field) => {
    setShowPassword(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: "", text: "" });

    // Client-side validation to match backend DTO rules
    if (!profileData.username || profileData.username.trim().length < 3) {
      setMessage({
        type: "error",
        text: "Username must be at least 3 characters long"
      });
      setLoading(false);
      return;
    }

    if (profileData.username.trim().length > 30) {
      setMessage({
        type: "error",
        text: "Username must be no more than 30 characters long"
      });
      setLoading(false);
      return;
    }

    if (!profileData.email || !profileData.email.trim()) {
      setMessage({
        type: "error",
        text: "Email is required"
      });
      setLoading(false);
      return;
    }

    // Basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(profileData.email.trim())) {
      setMessage({
        type: "error",
        text: "Please enter a valid email address"
      });
      setLoading(false);
      return;
    }

    try {
      const updateData = {
        username: profileData.username.trim(),
        email: profileData.email.trim()
      };

      if (profileImage) {
        // Handle profile image upload separately using FormData
        const reader = new FileReader();
        reader.onload = async () => {
          try {
            // First upload the avatar using FormData
            const formData = new FormData();
            formData.append("avatar", profileImage);
            
            await ApiService.uploadAvatar(formData);
            
            // Then update other profile fields (username/email) if they changed
            if (updateData.username.trim() !== user?.username || updateData.email.trim() !== user?.email) {
              await ApiService.updateUser(updateData);
            }

            // Check if username was changed - if so, force re-authentication
            const usernameChanged = user?.username !== updateData.username.trim();
            
            if (usernameChanged) {
              // Username changed - need fresh JWT token
              setMessage({
                type: "success",
                text: "Profile updated successfully! Please log in again with your new username."
              });
              
              // Clear token and redirect to login after a short delay
              setTimeout(() => {
                ApiService.clearToken();
                window.location.href = "/authentication/sign-in";
              }, 2000);
              
              setEditMode(false);
              setProfileImage(null);
            } else {
              // Only refresh user context if username didn't change
              await refreshUser();

              setMessage({
                type: "success",
                text: "Profile updated successfully!"
              });
              setEditMode(false);
              setProfileImage(null);
            }
          } catch (error) {
            setMessage({
              type: "error",
              text: error.message || "Failed to update profile"
            });
          } finally {
            setLoading(false);
          }
        };
        reader.readAsDataURL(profileImage);
      } else {
        await ApiService.updateUser(updateData);

        // Check if username was changed - if so, force re-authentication
        const usernameChanged = user?.username !== updateData.username.trim();
        
        if (usernameChanged) {
          // Username changed - need fresh JWT token
          setMessage({
            type: "success",
            text: "Username updated successfully! Please log in again with your new username."
          });
          
          // Clear token and redirect to login after a short delay
          setTimeout(() => {
            ApiService.clearToken();
            window.location.href = "/authentication/sign-in";
          }, 2000);
          
          setEditMode(false);
          setProfileImage(null);
        } else {
          // Only refresh user context if username didn't change
          await refreshUser();

          setMessage({
            type: "success",
            text: "Profile updated successfully!"
          });
          setEditMode(false);
          setProfileImage(null);
        }
      }
    } catch (error) {
      setMessage({
        type: "error",
        text: error.message || "Failed to update profile"
      });
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();

    // Validate passwords
    if (profileData.newPassword !== profileData.confirmPassword) {
      setPasswordMessage({
        type: "error",
        text: "New passwords do not match"
      });
      return;
    }

    if (profileData.newPassword.length < 6) {
      setPasswordMessage({
        type: "error",
        text: "Password must be at least 6 characters long"
      });
      return;
    }

    setPasswordLoading(true);
    setPasswordMessage({ type: "", text: "" });

    try {
      await ApiService.updatePassword({
        currentPassword: profileData.currentPassword,
        newPassword: profileData.newPassword
      });

      // Clear password fields
      setProfileData(prev => ({
        ...prev,
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
      }));

      setPasswordMessage({
        type: "success",
        text: "Password updated successfully!"
      });
    } catch (error) {
      setPasswordMessage({
        type: "error",
        text: error.message || "Failed to update password"
      });
    } finally {
      setPasswordLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = "/authentication/sign-in";
  };

  const cancelEdit = () => {
    setEditMode(false);
    setProfileData(prev => ({
      ...prev,
      username: user?.username || "",
      email: user?.email || ""
    }));
    handleRemoveImage();
    setMessage({ type: "", text: "" });
  };

  // Get the current profile image to display
  const getCurrentProfileImage = () => {
    // If in edit mode and there's a preview image, show that
    if (editMode && previewImage) {
      return previewImage;
    }
    
    // Otherwise, show the user's actual profile picture from backend
    if (user?.avatarBase64) {
      return `data:image/jpeg;base64,${user.avatarBase64}`;
    }
    
    // No profile picture available
    return null;
  };

  return (
    <DashboardLayout>
      <DashboardNavbar />
      <VuiBox py={3}>
        <VuiBox mb={3}>
          {/* Profile Header Card */}
          <Card sx={{ mb: 3 }}>
            <VuiBox p={4}>
              <VuiBox display="flex" alignItems="center" mb={3}>
                <AccountCircle sx={{ color: palette.info.main, fontSize: 32, mr: 2 }} />
                <VuiTypography variant="h4" color="white" fontWeight="bold">
                  Account Settings
                </VuiTypography>
                <Box sx={{ flexGrow: 1 }} />
                <Badge 
                  badgeContent="Pro" 
                  color="primary"
                  sx={{
                    '& .MuiBadge-badge': {
                      backgroundColor: palette.info.main,
                      color: 'white'
                    }
                  }}
                >
                  <VuiTypography variant="body2" color="text">
                    Premium Account
                  </VuiTypography>
                </Badge>
              </VuiBox>

              {message.text && (
                <Fade in={Boolean(message.text)}>
                  <Alert
                    severity={message.type}
                    onClose={() => setMessage({ type: "", text: "" })}
                    sx={{
                      mb: 3,
                      backgroundColor: message.type === "success"
                        ? "rgba(46, 204, 113, 0.2)"
                        : "rgba(231, 76, 60, 0.2)",
                      color: "white",
                      "& .MuiAlert-icon": {
                        color: "white"
                      }
                    }}
                  >
                    {message.text}
                  </Alert>
                </Fade>
              )}

              <Grid container spacing={4}>
                {/* Profile Image and Basic Info */}
                <Grid item xs={12} md={4}>
                  <Paper 
                    elevation={3}
                    sx={{ 
                      p: 3, 
                      backgroundColor: 'rgba(255, 255, 255, 0.05)',
                      backdropFilter: 'blur(10px)',
                      border: '1px solid rgba(255, 255, 255, 0.1)'
                    }}
                  >
                    <VuiBox display="flex" flexDirection="column" alignItems="center">
                      <VuiBox position="relative" mb={2}>
                        <Zoom in={true}>
                          <Avatar
                            src={getCurrentProfileImage()}
                            sx={{
                              width: 120,
                              height: 120,
                              bgcolor: palette.info.main,
                              fontSize: '3rem',
                              border: `3px solid ${palette.info.main}`,
                              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)'
                            }}
                          >
                            {user?.username?.charAt(0)?.toUpperCase() || 'U'}
                          </Avatar>
                        </Zoom>
                        
                        {editMode && (
                          <VuiBox position="absolute" bottom={0} right={0}>
                            <input
                              type="file"
                              ref={fileInputRef}
                              onChange={handleImageUpload}
                              accept="image/*"
                              style={{ display: 'none' }}
                            />
                            <Tooltip title="Upload Photo">
                              <IconButton
                                onClick={() => fileInputRef.current?.click()}
                                sx={{
                                  backgroundColor: palette.info.main,
                                  color: 'white',
                                  '&:hover': {
                                    backgroundColor: palette.info.focus,
                                  }
                                }}
                                size="small"
                              >
                                <PhotoCamera fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </VuiBox>
                        )}
                      </VuiBox>

                      {(previewImage || user?.avatarBase64) && editMode && (
                        <VuiButton
                          color="error"
                          size="small"
                          onClick={handleRemoveImage}
                          sx={{ mb: 2 }}
                        >
                          Remove Image
                        </VuiButton>
                      )}

                      <VuiTypography variant="h5" color="white" textAlign="center">
                        {user?.username}
                      </VuiTypography>
                      <VuiTypography variant="body2" color="text" textAlign="center" mb={3}>
                        {user?.email}
                      </VuiTypography>

                      <VuiBox width="100%" mt={2}>
                        <VuiButton
                          color="error"
                          onClick={handleLogout}
                          fullWidth
                          startIcon={<Settings />}
                        >
                          Logout
                        </VuiButton>
                      </VuiBox>
                    </VuiBox>
                  </Paper>
                </Grid>

                {/* Profile Information Form */}
                <Grid item xs={12} md={8}>
                  <Paper 
                    elevation={3}
                    sx={{ 
                      p: 3, 
                      backgroundColor: 'rgba(255, 255, 255, 0.05)',
                      backdropFilter: 'blur(10px)',
                      border: '1px solid rgba(255, 255, 255, 0.1)'
                    }}
                  >
                    <VuiBox display="flex" alignItems="center" justifyContent="space-between" mb={3}>
                      <VuiBox display="flex" alignItems="center">
                        <Person sx={{ color: palette.info.main, mr: 1 }} />
                        <VuiTypography variant="h6" color="white" fontWeight="bold">
                          Personal Information
                        </VuiTypography>
                      </VuiBox>
                      
                      {!editMode ? (
                        <VuiButton
                          color="info"
                          onClick={() => setEditMode(true)}
                          startIcon={<Edit />}
                          size="small"
                        >
                          Edit Profile
                        </VuiButton>
                      ) : (
                        <VuiBox display="flex" gap={1}>
                          <VuiButton
                            color="success"
                            onClick={handleUpdateProfile}
                            disabled={loading}
                            startIcon={<Save />}
                            size="small"
                          >
                            {loading ? "Saving..." : "Save"}
                          </VuiButton>
                          <VuiButton
                            color="secondary"
                            onClick={cancelEdit}
                            startIcon={<Cancel />}
                            size="small"
                          >
                            Cancel
                          </VuiButton>
                        </VuiBox>
                      )}
                    </VuiBox>

                    <VuiBox component="form" onSubmit={handleUpdateProfile}>
                      <Grid container spacing={3}>
                        <Grid item xs={12} sm={6}>
                          <VuiBox mb={1} ml={0.5}>
                            <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                              Username
                            </VuiTypography>
                          </VuiBox>
                          <GradientBorder
                            minWidth="100%"
                            padding="1px"
                            borderRadius={borders.borderRadius.lg}
                            backgroundImage={radialGradient(
                              palette.gradients.borderLight.main,
                              palette.gradients.borderLight.state,
                              palette.gradients.borderLight.angle
                            )}
                          >
                            <VuiInput
                              name="username"
                              value={profileData.username}
                              onChange={handleChange}
                              placeholder="Your username"
                              disabled={!editMode}
                              startAdornment={<Person sx={{ color: palette.text.main, mr: 1 }} />}
                            />
                          </GradientBorder>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                          <VuiBox mb={1} ml={0.5}>
                            <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                              Email
                            </VuiTypography>
                          </VuiBox>
                          <GradientBorder
                            minWidth="100%"
                            padding="1px"
                            borderRadius={borders.borderRadius.lg}
                            backgroundImage={radialGradient(
                              palette.gradients.borderLight.main,
                              palette.gradients.borderLight.state,
                              palette.gradients.borderLight.angle
                            )}
                          >
                            <VuiInput
                              type="email"
                              name="email"
                              value={profileData.email}
                              onChange={handleChange}
                              disabled
                              startAdornment={<Email sx={{ color: palette.text.main, mr: 1 }} />}
                            />
                          </GradientBorder>
                        </Grid>
                      </Grid>
                    </VuiBox>
                  </Paper>
                </Grid>
              </Grid>
            </VuiBox>
          </Card>

          {/* Security Settings Card */}
          <Card>
            <VuiBox p={4}>
              <VuiBox display="flex" alignItems="center" mb={3}>
                <Security sx={{ color: palette.warning.main, fontSize: 28, mr: 2 }} />
                <VuiTypography variant="h5" color="white" fontWeight="bold">
                  Security Settings
                </VuiTypography>
              </VuiBox>

              {passwordMessage.text && (
                <Fade in={Boolean(passwordMessage.text)}>
                  <Alert
                    severity={passwordMessage.type}
                    onClose={() => setPasswordMessage({ type: "", text: "" })}
                    sx={{
                      mb: 3,
                      backgroundColor: passwordMessage.type === "success"
                        ? "rgba(46, 204, 113, 0.2)"
                        : "rgba(231, 76, 60, 0.2)",
                      color: "white",
                      "& .MuiAlert-icon": {
                        color: "white"
                      }
                    }}
                  >
                    {passwordMessage.text}
                  </Alert>
                </Fade>
              )}

              <Paper 
                elevation={3}
                sx={{ 
                  p: 3, 
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  backdropFilter: 'blur(10px)',
                  border: '1px solid rgba(255, 255, 255, 0.1)'
                }}
              >
                <VuiBox display="flex" alignItems="center" mb={3}>
                  <Lock sx={{ color: palette.warning.main, mr: 1 }} />
                  <VuiTypography variant="h6" color="white" fontWeight="bold">
                    Change Password
                  </VuiTypography>
                </VuiBox>

                <VuiBox component="form" onSubmit={handleUpdatePassword}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <VuiBox mb={1} ml={0.5}>
                        <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                          Current Password
                        </VuiTypography>
                      </VuiBox>
                      <GradientBorder
                        minWidth="100%"
                        padding="1px"
                        borderRadius={borders.borderRadius.lg}
                        backgroundImage={radialGradient(
                          palette.gradients.borderLight.main,
                          palette.gradients.borderLight.state,
                          palette.gradients.borderLight.angle
                        )}
                      >
                        <VuiInput
                          type={showPassword.current ? "text" : "password"}
                          name="currentPassword"
                          value={profileData.currentPassword}
                          onChange={handleChange}
                          placeholder="Enter current password"
                          endAdornment={
                            <IconButton
                              onClick={() => togglePasswordVisibility('current')}
                              sx={{ color: palette.text.main }}
                            >
                              {showPassword.current ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          }
                        />
                      </GradientBorder>
                    </Grid>

                    <Grid item xs={12} sm={6}>
                      <VuiBox mb={1} ml={0.5}>
                        <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                          New Password
                        </VuiTypography>
                      </VuiBox>
                      <GradientBorder
                        minWidth="100%"
                        padding="1px"
                        borderRadius={borders.borderRadius.lg}
                        backgroundImage={radialGradient(
                          palette.gradients.borderLight.main,
                          palette.gradients.borderLight.state,
                          palette.gradients.borderLight.angle
                        )}
                      >
                        <VuiInput
                          type={showPassword.new ? "text" : "password"}
                          name="newPassword"
                          value={profileData.newPassword}
                          onChange={handleChange}
                          placeholder="Enter new password"
                          endAdornment={
                            <IconButton
                              onClick={() => togglePasswordVisibility('new')}
                              sx={{ color: palette.text.main }}
                            >
                              {showPassword.new ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          }
                        />
                      </GradientBorder>
                    </Grid>

                    <Grid item xs={12} sm={6}>
                      <VuiBox mb={1} ml={0.5}>
                        <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                          Confirm New Password
                        </VuiTypography>
                      </VuiBox>
                      <GradientBorder
                        minWidth="100%"
                        padding="1px"
                        borderRadius={borders.borderRadius.lg}
                        backgroundImage={radialGradient(
                          palette.gradients.borderLight.main,
                          palette.gradients.borderLight.state,
                          palette.gradients.borderLight.angle
                        )}
                      >
                        <VuiInput
                          type={showPassword.confirm ? "text" : "password"}
                          name="confirmPassword"
                          value={profileData.confirmPassword}
                          onChange={handleChange}
                          placeholder="Confirm new password"
                          endAdornment={
                            <IconButton
                              onClick={() => togglePasswordVisibility('confirm')}
                              sx={{ color: palette.text.main }}
                            >
                              {showPassword.confirm ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          }
                        />
                      </GradientBorder>
                    </Grid>
                  </Grid>

                  <VuiBox display="flex" justifyContent="flex-end" mt={3}>
                    <VuiButton
                      type="submit"
                      color="warning"
                      disabled={passwordLoading}
                      startIcon={<Lock />}
                    >
                      {passwordLoading ? "Updating..." : "Update Password"}
                    </VuiButton>
                  </VuiBox>
                </VuiBox>
              </Paper>
            </VuiBox>
          </Card>
        </VuiBox>
        <Footer />
      </VuiBox>
    </DashboardLayout>
  );
}

export default Profile;