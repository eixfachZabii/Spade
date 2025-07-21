import React from "react";
import { Card } from "@mui/material";

// Vision UI Dashboard components
import VuiBox from "../../components/VuiBox";
import VuiTypography from "../../components/VuiTypography";

import DashboardLayout from "../../examples/LayoutContainers/DashboardLayout";
import DashboardNavbar from "../../examples/Navbars/DashboardNavbar";
import Footer from "../../examples/Footer";

// Import Poker Game
import Poker from "../poker/PokerGameBox";

// Data for the Line Chart
import { lineChartDataDashboard } from "./data/lineChartData";
import { lineChartOptionsDashboard } from "./data/lineChartOptions";
import LineChart from "../../examples/Charts/LineCharts/LineChart";

function Dashboard() {
  return (
    <div
      style={{
        width: "100vw",
        minHeight: "100vh",
        overflow: "auto",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* Dashboard Layout */}
      <DashboardLayout>
        <DashboardNavbar />
        <VuiBox py={3} sx={{ flexGrow: 1 }}>
          {/* Full-width Poker Game */}
          <VuiBox mb={3}>
            <VuiBox
              position="relative"
              sx={{
                width: "100%",
                minHeight: "400px", // Increased height
                height: "auto",
                transition: "all 0.3s ease",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
              }}
            >
              {/* Poker Game Integration */}
              <Poker />
            </VuiBox>
          </VuiBox>

          {/* Line Chart Section */}
          <VuiBox mb={3}>
            <Card>
              <VuiBox p={3}>
                <VuiTypography variant="lg" color="white" fontWeight="bold" mb="5px">
                  Win/Loss Watch
                </VuiTypography>
                <VuiBox sx={{ height: "310px" }}>
                  <LineChart
                    lineChartData={lineChartDataDashboard}
                    lineChartOptions={lineChartOptionsDashboard}
                  />
                </VuiBox>
              </VuiBox>
            </Card>
          </VuiBox>
        </VuiBox>

        {/* Footer */}
        <VuiBox mt="auto">
          <Footer />
        </VuiBox>
      </DashboardLayout>
    </div>
  );
}

export default Dashboard;