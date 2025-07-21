// client/src/examples/Sidenav/SidenavRoot.js

// @mui material components
import Drawer from "@mui/material/Drawer";
import { styled } from "@mui/material/styles";
import linearGradient from "../../assets/theme/functions/linearGradient";

export default styled(Drawer)(({ theme, ownerState }) => {
  const { palette, boxShadows, transitions, breakpoints, functions } = theme;
  const { transparentSidenav, miniSidenav } = ownerState;

  const sidebarWidth = 250;
  const { transparent, gradients } = palette;
  const { xxl } = boxShadows;
  const { pxToRem } = functions;

  // Enhanced styles for the sidenav when miniSidenav={false}
  const drawerOpenStyles = () => ({
    transform: "translateX(0)",
    transition: transitions.create(["transform", "width"], {
      easing: transitions.easing.easeOut,
      duration: transitions.duration.enteringScreen,
    }),

    [breakpoints.up("xl")]: {
      boxShadow: transparentSidenav ? "none" : xxl,
      marginBottom: transparentSidenav ? 0 : "inherit",
      left: "0",
      width: sidebarWidth,
      transform: "translateX(0)",
      transition: transitions.create(["width", "background-color", "box-shadow"], {
        easing: transitions.easing.easeOut,
        duration: transitions.duration.enteringScreen,
      }),
    },

    // Enhanced mobile behavior
    [breakpoints.down("xl")]: {
      transform: "translateX(0)",
      width: sidebarWidth,
      transition: transitions.create("transform", {
        easing: transitions.easing.easeOut,
        duration: transitions.duration.enteringScreen,
      }),
    },
  });

  // Enhanced styles for the sidenav when miniSidenav={true}
  const drawerCloseStyles = () => ({
    transform: `translateX(${pxToRem(-320)})`,
    transition: transitions.create("transform", {
      easing: transitions.easing.easeIn,
      duration: transitions.duration.leavingScreen,
    }),

    [breakpoints.up("xl")]: {
      boxShadow: transparentSidenav ? "none" : xxl,
      marginBottom: transparentSidenav ? 0 : "inherit",
      left: "0",
      width: pxToRem(96),
      overflowX: "hidden",
      transform: "translateX(0)",
      transition: transitions.create(["width", "background-color", "box-shadow"], {
        easing: transitions.easing.easeIn,
        duration: transitions.duration.leavingScreen,
      }),
    },

    // Enhanced mobile behavior
    [breakpoints.down("xl")]: {
      transform: `translateX(${pxToRem(-320)})`,
      transition: transitions.create("transform", {
        easing: transitions.easing.easeIn,
        duration: transitions.duration.leavingScreen,
      }),
    },
  });

  return {
    "& .MuiDrawer-paper": {
      boxShadow: xxl,
      border: "none",
      background: transparentSidenav
          ? transparent.main
          : linearGradient(
              gradients.sidenav.main,
              gradients.sidenav.state,
              gradients.sidenav.deg
          ),
      backdropFilter: transparentSidenav ? "unset" : "blur(120px)",

      // Apply the appropriate styles based on miniSidenav state
      ...(miniSidenav ? drawerCloseStyles() : drawerOpenStyles()),

      // Enhanced mobile-specific styles
      [breakpoints.down("xl")]: {
        position: "fixed",
        top: 0,
        left: 0,
        height: "100vh",
        zIndex: 1200,
        ...(miniSidenav ? drawerCloseStyles() : drawerOpenStyles()),
      },

      // Improved scrollbar styling
      "&::-webkit-scrollbar": {
        width: "6px",
      },
      "&::-webkit-scrollbar-track": {
        background: "transparent",
      },
      "&::-webkit-scrollbar-thumb": {
        background: "rgba(255, 255, 255, 0.2)",
        borderRadius: "3px",
        "&:hover": {
          background: "rgba(255, 255, 255, 0.3)",
        },
      },

      // Smooth content transitions
      "& .MuiList-root": {
        transition: transitions.create("opacity", {
          easing: transitions.easing.easeInOut,
          duration: transitions.duration.shorter,
        }),
      },

      // Enhanced hover effects for logo area
      "& .logo-container": {
        transition: transitions.create(["background-color", "transform"], {
          easing: transitions.easing.easeOut,
          duration: transitions.duration.shorter,
        }),
      },
    },
  };
});