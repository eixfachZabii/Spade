// client/src/examples/Navbars/DashboardNavbar/styles.js

export function navbar(theme, ownerState) {
  const { palette, boxShadows, functions, transitions, breakpoints, borders } = theme;
  const { transparentNavbar, absolute, light } = ownerState;

  const { dark, white, gradients, transparent, background } = palette;
  const { navbarBoxShadow } = boxShadows;
  const { rgba, pxToRem, linearGradient } = functions;
  const { borderRadius, borderWidth } = borders;

  return {
    boxShadow: transparentNavbar || absolute ? "none" : navbarBoxShadow,
    backdropFilter: transparentNavbar || absolute ? "none" : `blur(${pxToRem(42)})`,
    backgroundColor: transparentNavbar || absolute
      ? `${transparent.main} !important`
      : rgba(white.main, 0.8),

    // Add a subtle border
    border: transparentNavbar || absolute
      ? `${borderWidth[1]} solid rgba(255, 255, 255, 0.15) !important`
      : `${borderWidth[1]} solid rgba(255, 255, 255, 0.2) !important`,

    // Add a subtle gradient overlay
    backgroundImage: transparentNavbar || absolute
      ? 'none'
      : `${linearGradient(
          rgba(gradients.dark.main, 0.05),
          rgba(gradients.dark.state, 0.05)
        )}`,

    color: () => {
      let color;

      if (light) {
        color = white.main;
      } else if (transparentNavbar) {
        color = dark.main;
      } else {
        color = dark.main;
      }

      return color;
    },
    top: absolute ? 0 : pxToRem(12),
    minHeight: pxToRem(75),
    display: "grid",
    alignItems: "center",
    borderRadius: borderRadius.xl,
    paddingTop: pxToRem(8),
    paddingBottom: pxToRem(8),
    paddingRight: absolute ? pxToRem(8) : 0,
    paddingLeft: absolute ? pxToRem(16) : 0,
    transition: transitions.create("all", {
      easing: transitions.easing.easeInOut,
      duration: transitions.duration.standard,
    }),

    "& > *": {
      transition: transitions.create("all", {
        easing: transitions.easing.easeInOut,
        duration: transitions.duration.standard,
      }),
    },

    "& .MuiToolbar-root": {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",

      [breakpoints.up("sm")]: {
        minHeight: "auto",
        padding: `${pxToRem(4)} ${pxToRem(16)}`,
      },
    },
  };
}

export const navbarContainer = ({ breakpoints }) => ({
  flexDirection: "column",
  alignItems: "flex-start",
  justifyContent: "space-between",
  pt: 0.5,
  pb: 0.5,

  [breakpoints.up("md")]: {
    flexDirection: "row",
    alignItems: "center",
    paddingTop: "0",
    paddingBottom: "0",
  },
});

export const navbarRow = ({ breakpoints }, { isMini }) => ({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  width: "100%",

  [breakpoints.up("md")]: {
    justifyContent: isMini ? "space-between" : "stretch",
    width: isMini ? "100%" : "max-content",
  },

  [breakpoints.up("xl")]: {
    justifyContent: "stretch !important",
    width: "max-content !important",
  },
});

export const navbarIconButton = ({ typography: { size }, transitions, palette }) => ({
  px: 0.75,
  mx: 0.5,
  transition: transitions.create("all", {
    easing: transitions.easing.easeInOut,
    duration: transitions.duration.standard,
  }),

  // Add hover effects
  "&:hover": {
    backgroundColor: `rgba(255, 255, 255, 0.1)`,
    transform: "translateY(-2px)",
  },

  "& .material-icons, .material-icons-round": {
    fontSize: `${size.md} !important`,
  },
});

export const navbarMobileMenu = ({ breakpoints, transitions }) => ({
  display: "inline-block",
  lineHeight: 0,
  mx: 0.5,
  transition: transitions.create("all", {
    easing: transitions.easing.easeInOut,
    duration: transitions.duration.standard,
  }),

  // Add hover effects for mobile menu button
  "&:hover": {
    backgroundColor: `rgba(255, 255, 255, 0.1)`,
    transform: "translateY(-2px)",
  },

  [breakpoints.up("xl")]: {
    display: "none",
  },
});