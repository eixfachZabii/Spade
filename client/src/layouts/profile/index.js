// client/src/layouts/profile/index.js
import {useEffect, useState} from "react";
import { Card, Grid, Avatar, Typography, Button, Box, Alert } from "@mui/material";
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
import AuthService from "../../services/AuthService";
import {useNavigate} from "react-router-dom";

function Profile() {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [profileData, setProfileData] = useState({
    username: user?.username || "",
    email: user?.email || "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [message, setMessage] = useState({ type: "", text: "" });
  const [loading, setLoading] = useState(false);

  // Redirect to login page if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/authentication/sign-in");
    }
  }, [isAuthenticated, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: "", text: "" });

    try {
      // Call API with proper field name (username)
      await AuthService.apiCall("/users/me", {
        method: "PUT",
        body: JSON.stringify({
          username: profileData.username
        })
      });

      setMessage({
        type: "success",
        text: "Profile updated successfully!"
      });
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
      setMessage({
        type: "error",
        text: "New passwords do not match"
      });
      return;
    }

    if (profileData.newPassword.length < 6) {
      setMessage({
        type: "error",
        text: "Password must be at least 6 characters long"
      });
      return;
    }

    setLoading(true);
    setMessage({ type: "", text: "" });

    try {
      // Using the format expected by UpdatePasswordDto
      await AuthService.apiCall("/users/me/password", {
        method: "PUT",
        body: JSON.stringify({
          currentPassword: profileData.currentPassword,
          newPassword: profileData.newPassword
        })
      });

      // Clear password fields
      setProfileData(prev => ({
        ...prev,
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
      }));

      setMessage({
        type: "success",
        text: "Password updated successfully!"
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: error.message || "Failed to update password"
      });
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = "/authentication/sign-in";
  };

  return (
    <DashboardLayout>
      <DashboardNavbar />
      <VuiBox py={3}>
        <VuiBox mb={3}>
          <Card>
            <VuiBox p={4}>
              <VuiTypography variant="h5" color="white" fontWeight="bold" mb={2}>
                Profile
              </VuiTypography>

              {message.text && (
                <Alert
                  severity={message.type}
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
              )}

              <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                  <VuiBox display="flex" flexDirection="column" alignItems="center">
                    <Avatar
                      sx={{
                        width: 100,
                        height: 100,
                        bgcolor: 'primary.main',
                        fontSize: '2.5rem',
                        mb: 2
                      }}
                    >
                      {user?.username?.charAt(0) || 'U'}
                    </Avatar>
                    <VuiTypography variant="h5" color="white">
                      {user?.username}
                    </VuiTypography>
                    <VuiTypography variant="body2" color="text">
                      {user?.email}
                    </VuiTypography>
                    <VuiBox mt={4} width="100%">
                      <VuiButton
                        color="error"
                        onClick={handleLogout}
                        fullWidth
                      >
                        Logout
                      </VuiButton>
                    </VuiBox>
                  </VuiBox>
                </Grid>
                <Grid item xs={12} md={8}>
                  <VuiBox>
                    <VuiBox component="form" onSubmit={handleUpdateProfile} mb={4}>
                      <VuiTypography variant="h6" color="white" mb={2}>
                        Personal Information
                      </VuiTypography>
                      <VuiBox mb={2}>
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
                          />
                        </GradientBorder>
                      </VuiBox>
                      <VuiBox mb={2}>
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
                          />
                        </GradientBorder>
                      </VuiBox>
                      <VuiBox display="flex" justifyContent="flex-end">
                        <VuiButton
                          type="submit"
                          color="info"
                          disabled={loading}
                        >
                          {loading ? "Updating..." : "Update Profile"}
                        </VuiButton>
                      </VuiBox>
                    </VuiBox>

                    <VuiBox component="form" onSubmit={handleUpdatePassword}>
                      <VuiTypography variant="h6" color="white" mb={2}>
                        Change Password
                      </VuiTypography>
                      <VuiBox mb={2}>
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
                            type="password"
                            name="currentPassword"
                            value={profileData.currentPassword}
                            onChange={handleChange}
                            placeholder="Current password"
                          />
                        </GradientBorder>
                      </VuiBox>
                      <VuiBox mb={2}>
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
                            type="password"
                            name="newPassword"
                            value={profileData.newPassword}
                            onChange={handleChange}
                            placeholder="New password"
                          />
                        </GradientBorder>
                      </VuiBox>
                      <VuiBox mb={2}>
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
                            type="password"
                            name="confirmPassword"
                            value={profileData.confirmPassword}
                            onChange={handleChange}
                            placeholder="Confirm new password"
                          />
                        </GradientBorder>
                      </VuiBox>
                      <VuiBox display="flex" justifyContent="flex-end">
                        <VuiButton
                          type="submit"
                          color="info"
                          disabled={loading}
                        >
                          {loading ? "Updating..." : "Change Password"}
                        </VuiButton>
                      </VuiBox>
                    </VuiBox>
                  </VuiBox>
                </Grid>
              </Grid>
            </VuiBox>
          </Card>
        </VuiBox>
        <Footer />
      </VuiBox>
    </DashboardLayout>
  );
}

export default Profile;