// Import layouts
import Dashboard from "./layouts/dashboard";
import Poker from "./layouts/poker";
import Billing from "./layouts/billing";
import Spotify from "./layouts/spotify"
import Analytics from "./layouts/analytics"
import Cheatsheet from "./layouts/cheatsheet"
import Profile from "./layouts/profile"

// Import authentication components
import SignIn from "./layouts/authentication/sign-in";  // Use the original component
import SignUp from "./layouts/authentication/sign-up";

// Import icons
import { BsSpotify, BsSuitSpadeFill } from "react-icons/bs";
import { BsCreditCardFill } from "react-icons/bs";
import { IoStatsChart } from "react-icons/io5";
import { IoHome } from "react-icons/io5";
import { FaRocket } from "react-icons/fa6";
import { LuFileSpreadsheet } from "react-icons/lu";
import { Person } from "@mui/icons-material";

const routes = [
  // Dashboard routes - accessible to all users
  {
    type: "collapse",
    name: "Dashboard",
    key: "dashboard",
    route: "/dashboard",
    icon: <IoHome size="15px" color="inherit" />,
    component: Dashboard,
    noCollapse: true,
  },
  {
    type: "collapse",
    name: "Poker",
    key: "poker",
    route: "/poker",
    icon: <BsSuitSpadeFill size="15px" color="inherit" />,
    component: Poker,
    noCollapse: true,
  },
  {
    type: "collapse",
    name: "Spotify",
    key: "spotify",
    route: "/spotify",
    icon: <BsSpotify size="15px" color="inherit" />,
    component: Spotify,
    noCollapse: true,
  },
  {
    type: "collapse",
    name: "Cheatsheet",
    key: "cheatsheet",
    route: "/cheatsheet",
    icon: <LuFileSpreadsheet size="15px" color="inherit" />,
    component: Cheatsheet,
    noCollapse: true,
  },
  {
    type: "collapse",
    name: "Analytics",
    key: "analytics",
    route: "/analytics",
    icon: <IoStatsChart size="15px" color="inherit" />,
    component: Analytics,
    noCollapse: true,
  },
  {
    type: "collapse",
    name: "Billing",
    key: "billing",
    route: "/billing",
    icon: <BsCreditCardFill size="15px" color="inherit" />,
    component: Billing,
    noCollapse: true,
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

  // Authentication routes (public)
  {
    route: "/authentication/sign-in",
    component: SignIn,
    key: "sign-in",
  },
  {
    route: "/authentication/sign-up",
    component: SignUp,
    key: "sign-up",
  },
];

export default routes;