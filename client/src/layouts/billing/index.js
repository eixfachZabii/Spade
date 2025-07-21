import React from "react";
import Grid from "@mui/material/Grid";

// Vision UI Dashboard React components
import VuiBox from "components/VuiBox";
import VuiTypography from "components/VuiTypography";

// Vision UI Dashboard React example components
import DashboardLayout from "examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "examples/Navbars/DashboardNavbar";
import Footer from "examples/Footer";

// Modern PayPal component
import ModernPayPalCard from "./components/PayPalCard";

function Billing() {
  return (
    <div
      style={{
        width: "100%",
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <DashboardLayout>
        <DashboardNavbar />
        <VuiBox mt={4}>
          <VuiBox mb={3} textAlign="center">
            <VuiTypography variant="h4" color="white" fontWeight="bold" mb={1}>
              Payment Methods
            </VuiTypography>
            <VuiTypography variant="body2" color="text">
              Scan the QR code to make payments or share with others
            </VuiTypography>
          </VuiBox>

          <VuiBox mb={3}>
            <Grid container spacing={3} justifyContent="center">
              <Grid item xs={12} md={8} lg={5}>
                <ModernPayPalCard userName="Sebastian Rogg" amount="2,457.80" />
              </Grid>
            </Grid>
          </VuiBox>
        </VuiBox>
        <Footer />
      </DashboardLayout>
    </div>
  );
}

export default Billing;