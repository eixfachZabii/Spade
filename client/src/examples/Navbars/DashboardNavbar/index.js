// client/src/examples/Navbars/DashboardNavbar/index.js

import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";

// @mui material components
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Icon from "@mui/material/Icon";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";

// Vision UI Dashboard React components
import VuiBox from "components/VuiBox";
import VuiInput from "components/VuiInput";

// Vision UI Dashboard React example components
import Breadcrumbs from "examples/Breadcrumbs";

// Custom styles for DashboardNavbar
import {
  navbar,
  navbarContainer,
  navbarRow,
  navbarIconButton,
  navbarMobileMenu,
} from "examples/Navbars/DashboardNavbar/styles";

// Vision UI Dashboard React context
import {
  useVisionUIController,
  setTransparentNavbar,
  setMiniSidenav,
  setOpenConfigurator,
} from "context";

// Import Auth menu component
import AuthNavMenu from "components/AuthNavMenu";

function DashboardNavbar({ absolute, light, isMini }) {
  const [navbarType, setNavbarType] = useState();
  const [controller, dispatch] = useVisionUIController();
  const { miniSidenav, transparentNavbar, fixedNavbar, openConfigurator } = controller;
  const route = useLocation().pathname.split("/").slice(1);

  useEffect(() => {
    // Setting the navbar type
    if (fixedNavbar) {
      setNavbarType("sticky");
    } else {
      setNavbarType("static");
    }

    // A function that sets the transparent state of the navbar.
     function handleTransparentNavbar() {
      // Instead of checking scroll position, keep navbar styling consistent
      setTransparentNavbar(dispatch, true);
    }

      // The rest of the effect remains the same:
      window.addEventListener("scroll", handleTransparentNavbar);
      handleTransparentNavbar();
      return () => window.removeEventListener("scroll", handleTransparentNavbar);
    }, [dispatch, fixedNavbar]);

  const handleMiniSidenav = () => setMiniSidenav(dispatch, !miniSidenav);
  const handleConfiguratorOpen = () => setOpenConfigurator(dispatch, !openConfigurator);

  // Get current page title from the route
  const getActiveRoute = (routes) => {
    let activeRoute = "Dashboard";
    for (let i = 0; i < routes.length; i++) {
      if (routes[i] && routes[i].trim() !== "") {
        activeRoute = routes[i].charAt(0).toUpperCase() + routes[i].slice(1);
      }
    }
    return activeRoute;
  };

  return (
    <AppBar
      position={absolute ? "absolute" : navbarType}
      color="inherit"
      sx={(theme) => navbar(theme, { transparentNavbar, absolute, light })}
    >
      <Toolbar sx={(theme) => navbarContainer(theme)}>
        <VuiBox color="inherit" mb={{ xs: 1, md: 0 }} sx={(theme) => navbarRow(theme, { isMini })}>
          <Breadcrumbs icon="home" title={getActiveRoute(route)} route={route} light={light} />
        </VuiBox>
        {isMini ? null : (
          <VuiBox sx={(theme) => navbarRow(theme, { isMini })}>
            <VuiBox pr={1}>
              <VuiInput
                placeholder="Type here..."
                icon={{ component: "search", direction: "left" }}
                sx={({ palette: { white, inputColors } }) => ({
                  "& .MuiInputBase-root": {
                    background: inputColors.backgroundColor,
                    borderRadius: "xl",
                    transition: "all 0.3s ease",
                    "&:hover": {
                      boxShadow: "0 0 10px rgba(82, 172, 250, 0.3)",
                    },
                  },
                  "& input": {
                    color: white.main,
                  },
                })}
              />
            </VuiBox>
            <VuiBox color={light ? "white" : "inherit"} display="flex" alignItems="center">
              <IconButton
                size="small"
                color="inherit"
                sx={navbarMobileMenu}
                onClick={handleMiniSidenav}
              >
                <Icon className={light ? "text-white" : "text-dark"}>
                  {miniSidenav ? "menu_open" : "menu"}
                </Icon>
              </IconButton>
              <IconButton
                size="small"
                color="inherit"
                sx={navbarIconButton}
                onClick={handleConfiguratorOpen}
              >
                <Icon>settings</Icon>
              </IconButton>

              {/* User profile menu - replaced the sign-in button */}
              <AuthNavMenu />
            </VuiBox>
          </VuiBox>
        )}
      </Toolbar>
    </AppBar>
  );
}

export default DashboardNavbar;