// client/src/examples/Sidenav/styles/sidenav.js

export default function sidenavLogoLabel(theme, ownerState) {
  const { functions, transitions, typography, breakpoints } = theme;
  const { miniSidenav } = ownerState;

  const { pxToRem } = functions;
  const { fontWeightMedium } = typography;

  return {
    ml: 0.5,
    fontWeight: fontWeightMedium,
    wordSpacing: pxToRem(-1),
    transition: transitions.create(["opacity", "transform", "margin"], {
      easing: transitions.easing.easeInOut,
      duration: transitions.duration.standard,
    }),

    [breakpoints.up("xl")]: {
      opacity: miniSidenav ? 0 : 1,
      transform: miniSidenav ? "translateX(-10px)" : "translateX(0px)",
    },

    // Enhanced mobile behavior
    [breakpoints.down("xl")]: {
      opacity: 1,
      transform: "translateX(0px)",
    },
  };
}