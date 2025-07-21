// client/src/layouts/authentication/sign-in/index.js
import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";

// @mui material components

// Icons

// Vision UI Dashboard React components
import VuiBox from "../../../components/VuiBox";
import VuiTypography from "../../../components/VuiTypography";
import VuiInput from "../../../components/VuiInput";
import VuiButton from "../../../components/VuiButton";
import VuiSwitch from "../../../components/VuiSwitch";
import GradientBorder from "../../../examples/GradientBorder";

// Vision UI Dashboard assets
import radialGradient from "../../../assets/theme/functions/radialGradient";
import palette from "../../../assets/theme/base/colors";
import borders from "../../../assets/theme/base/borders";

// Authentication layout components
import IllustrationLayout from "../components/IllustrationLayout";

// Authentication context
import { useAuth } from "../../../context/AuthContext";

// Images
import bgSignIn from "../../../assets/images/signInImage.png";
import CoverLayout from "../components/CoverLayout";
import {Alert, AlertTitle, IconButton} from "@mui/material";
import Icon from "@mui/material/Icon";

function SignIn() {
  const [rememberMe, setRememberMe] = useState(true);
  const [credentials, setCredentials] = useState({
    username: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [localError, setLocalError] = useState("");

  const { login, error: authError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Get the path to redirect to after login
  const from = location.state?.from || '/dashboard';

  const handleSetRememberMe = () => setRememberMe(!rememberMe);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setCredentials(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear errors when user starts typing
    if (localError) setLocalError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Basic validation
    if (!credentials.username.trim()) {
      setLocalError("Username is required");
      return;
    }

    if (!credentials.password) {
      setLocalError("Password is required");
      return;
    }

    setIsLoading(true);
    setLocalError("");

    try {
      await login(credentials);
      // Redirect to the intended page or dashboard
      navigate(from, { replace: true });
    } catch (err) {
      setLocalError(err.message || "Login failed. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  // Display error - prioritize local error over auth context error
  const displayError = localError || authError;

  return (
      <CoverLayout
          title="Welcome back"
          color="white"
          description="Enter your username and password to sign in"
          premotto="SPADE BOOT"
          motto="YOUR POKER DASHBOARD"
          image={bgSignIn}
      >
        <VuiBox component="form" role="form" onSubmit={handleSubmit}>
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
                  type="text"
                  name="username"
                  placeholder="Your username..."
                  value={credentials.username}
                  onChange={handleInputChange}
                  sx={({ typography: { size } }) => ({
                    fontSize: size.sm,
                  })}
              />
            </GradientBorder>
          </VuiBox>
          <VuiBox mb={2}>
            <VuiBox mb={1} ml={0.5}>
              <VuiTypography component="label" variant="button" color="white" fontWeight="medium">
                Password
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
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="Your password..."
                  value={credentials.password}
                  onChange={handleInputChange}
                  sx={({ typography: { size } }) => ({
                    fontSize: size.sm,
                  })}
                  endAdornment={
                    <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                        sx={{ color: "white" }}
                    >
                      <Icon style={{ color: "white" }}>
                        {showPassword ? "visibility_off" : "visibility"}
                      </Icon>
                    </IconButton>
                  }
              />
            </GradientBorder>
          </VuiBox>
          <VuiBox display="flex" alignItems="center">
            <VuiSwitch color="info" checked={rememberMe} onChange={handleSetRememberMe} />
            <VuiTypography
                variant="caption"
                color="white"
                fontWeight="medium"
                onClick={handleSetRememberMe}
                sx={{ cursor: "pointer", userSelect: "none" }}
            >
              &nbsp;&nbsp;&nbsp;&nbsp;Remember me
            </VuiTypography>
          </VuiBox>

          {/* Error Display */}
          {displayError && (
              <VuiBox mt={2}>
                <VuiTypography variant="caption" color="error" textAlign="center">
                  {displayError}
                </VuiTypography>
              </VuiBox>
          )}

          <VuiBox mt={4} mb={1}>
            <VuiButton
                color="info"
                size="large"
                fullWidth
                type="submit"
                disabled={isLoading}
            >
              {isLoading ? "SIGNING IN..." : "SIGN IN"}
            </VuiButton>
          </VuiBox>
          <VuiBox mt={3} textAlign="center">
            <VuiTypography variant="button" color="text" fontWeight="regular">
              Don&apos;t have an account?{" "}
              <VuiTypography
                  component={Link}
                  to="/authentication/sign-up"
                  variant="button"
                  color="white"
                  fontWeight="medium"
              >
                Sign up
              </VuiTypography>
            </VuiTypography>
          </VuiBox>
        </VuiBox>
      </CoverLayout>
  );
}

export default SignIn;