// client/src/routes.js
// Import layouts
import Dashboard from "./layouts/dashboard";
import Poker from "./layouts/poker";
import Spotify from "./layouts/spotify"
import Analytics from "./layouts/analytics"
import Cheatsheet from "./layouts/cheatsheet"
import Profile from "./layouts/profile"

// Import authentication components
import SignIn from "./layouts/authentication/sign-in";
import SignUp from "./layouts/authentication/sign-up";

// Import icons
import { BsSpotify, BsSuitSpadeFill } from "react-icons/bs";
import { IoStatsChart } from "react-icons/io5";
import { IoHome } from "react-icons/io5";
import { LuFileSpreadsheet } from "react-icons/lu";
import { Person } from "@mui/icons-material";

const routes = [
  // Protected routes - require authentication
  {
    type: "collapse",
    name: "Dashboard",
    key: "dashboard",
    route: "/dashboard",
    icon: <IoHome size="15px" color="inherit" />,
    component: Dashboard,
    noCollapse: true,
    requiresAuth: true,
  },
  {
    type: "collapse",
    name: "Poker",
    key: "poker",
    route: "/poker",
    icon: <BsSuitSpadeFill size="15px" color="inherit" />,
    component: Poker,
    noCollapse: true,
    requiresAuth: true,
  },
  {
    type: "collapse",
    name: "Spotify",
    key: "spotify",
    route: "/spotify",
    icon: <BsSpotify size="15px" color="inherit" />,
    component: Spotify,
    noCollapse: true,
    requiresAuth: true,
  },
  {
    type: "collapse",
    name: "Cheatsheet",
    key: "cheatsheet",
    route: "/cheatsheet",
    icon: <LuFileSpreadsheet size="15px" color="inherit" />,
    component: Cheatsheet,
    noCollapse: true,
    requiresAuth: true,
  },
  {
    type: "collapse",
    name: "Analytics",
    key: "analytics",
    route: "/analytics",
    icon: <IoStatsChart size="15px" color="inherit" />,
    component: Analytics,
    noCollapse: true,
    requiresAuth: true,
  },
  {
    type: "divider",
    key: "divider-profile",
  },
  {
    type: "collapse",
    name: "Profile",
    key: "profile",
    route: "/profile",
    icon: <Person size="15px" color="inherit" />,
    component: Profile,
    noCollapse: true,
    requiresAuth: true,
  },

  // Public routes (no authentication required)
  {
    route: "/authentication/sign-in",
    component: SignIn,
    key: "sign-in",
    requiresAuth: false,
  },
  {
    route: "/authentication/sign-up",
    component: SignUp,
    key: "sign-up",
    requiresAuth: false,
  },
];

export default routes;