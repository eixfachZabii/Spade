// client/src/examples/Sidenav/index.js

import React, { useEffect, useState } from "react";
import { useLocation, NavLink } from "react-router-dom";
import PropTypes from "prop-types";

// @mui material components
import List from "@mui/material/List";
import Divider from "@mui/material/Divider";
import Icon from "@mui/material/Icon";

// Vision UI Dashboard React components
import VuiBox from "../../components/VuiBox";
import VuiTypography from "../../components/VuiTypography";

// Vision UI Dashboard React example components
import SidenavCollapse from "./SidenavCollapse";
import SidenavRoot from "./SidenavRoot";

// Vision UI Dashboard React context
import { useVisionUIController, setMiniSidenav, setTransparentSidenav } from "../../context";

// Vision UI Dashboard React icons
import SpadeLogo from "../Icons/SpadeLogo";

function Sidenav({ color, brandName, routes, enableHover = false, ...rest }) {
    const [controller, dispatch] = useVisionUIController();
    const { miniSidenav, transparentSidenav } = controller;
    const location = useLocation();
    const { pathname } = location;
    const collapseName = pathname.split("/").slice(1)[0];
    const [isFullscreen, setIsFullscreen] = useState(false);

    const closeSidenav = () => setMiniSidenav(dispatch, true);

    // Manual toggle function for the logo
    const toggleSidenav = (e) => {
        if (e) {
            e.preventDefault();
            e.stopPropagation();
        }
        console.log("Manual toggle, current state:", miniSidenav);
        setMiniSidenav(dispatch, !miniSidenav);
    };

    // Conditional hover handlers - only work if enableHover is true
    const handleMouseEnter = (e) => {
        if (enableHover && miniSidenav) {
            setMiniSidenav(dispatch, false);
        }
    };

    const handleMouseLeave = (e) => {
        if (enableHover && !miniSidenav) {
            // Add a small delay to prevent immediate closing
            setTimeout(() => {
                setMiniSidenav(dispatch, true);
            }, 300);
        }
    };

    // Listen for fullscreen change
    useEffect(() => {
        function checkFullscreen() {
            setIsFullscreen(
                document.fullscreenElement !== null ||
                document.webkitFullscreenElement !== null ||
                document.mozFullScreenElement !== null ||
                document.msFullscreenElement !== null
            );
        }

        document.addEventListener("fullscreenchange", checkFullscreen);
        document.addEventListener("webkitfullscreenchange", checkFullscreen);
        document.addEventListener("mozfullscreenchange", checkFullscreen);
        document.addEventListener("MSFullscreenChange", checkFullscreen);

        return () => {
            document.removeEventListener("fullscreenchange", checkFullscreen);
            document.removeEventListener("webkitfullscreenchange", checkFullscreen);
            document.removeEventListener("mozfullscreenchange", checkFullscreen);
            document.removeEventListener("MSFullscreenChange", checkFullscreen);
        };
    }, []);

    // Handle sidenav resizing dynamically
    useEffect(() => {
        function handleMiniSidenav() {
            setMiniSidenav(dispatch, window.innerWidth < 1200);
        }

        window.addEventListener("resize", handleMiniSidenav);
        handleMiniSidenav();

        return () => window.removeEventListener("resize", handleMiniSidenav);
    }, [dispatch]);

    useEffect(() => {
        if (window.innerWidth < 1440) {
            setTransparentSidenav(dispatch, false);
        }
    }, []);

    const renderRoutes = routes.map(({ type, name, icon, title, noCollapse, key, route, href }) => {
        let returnValue;

        if (type === "collapse") {
            returnValue = href ? (
                <a href={href} key={key} target="_blank" rel="noreferrer" style={{ textDecoration: "none" }}>
                    <SidenavCollapse
                        color={color}
                        name={name}
                        icon={icon}
                        active={key === collapseName}
                        noCollapse={noCollapse}
                    />
                </a>
            ) : (
                <NavLink to={route} key={key}>
                    <SidenavCollapse
                        color={color}
                        key={key}
                        name={name}
                        icon={icon}
                        active={key === collapseName}
                        noCollapse={noCollapse}
                    />
                </NavLink>
            );
        } else if (type === "title") {
            returnValue = (
                <VuiTypography
                    key={key}
                    color="white"
                    display="block"
                    variant="caption"
                    fontWeight="bold"
                    textTransform="uppercase"
                    pl={3}
                    mt={2}
                    mb={1}
                    ml={1}
                >
                    {title}
                </VuiTypography>
            );
        } else if (type === "divider") {
            returnValue = <Divider light key={key} />;
        }

        return returnValue;
    });

    return (
        <>
            {/* Sidebar */}
            <SidenavRoot
                {...rest}
                variant="permanent"
                ownerState={{ transparentSidenav, miniSidenav }}
                onMouseEnter={enableHover ? handleMouseEnter : undefined}
                onMouseLeave={enableHover ? handleMouseLeave : undefined}
            >
                <VuiBox pt={2} px={4} textAlign="center" sx={{ overflow: "unset !important" }}>
                    {/* Close button for mobile */}
                    <VuiBox
                        display={{ xs: "block", xl: "none" }}
                        position="absolute"
                        top={0}
                        right={0}
                        p={1.5}
                        onClick={closeSidenav}
                        sx={{ cursor: "pointer" }}
                    >
                        <VuiTypography variant="h6" color="text">
                            <Icon sx={{ fontWeight: "bold" }}>close</Icon>
                        </VuiTypography>
                    </VuiBox>

                    {/* Enhanced Logo - Manual Toggle Button */}
                    <VuiBox
                        display="flex"
                        alignItems="center"
                        justifyContent="center"
                        onClick={toggleSidenav}
                        sx={{
                            cursor: "pointer",
                            transition: "all 0.3s ease",
                            borderRadius: "12px",
                            padding: "12px",
                            minWidth: "60px", // Ensure minimum clickable area
                            minHeight: "48px", // Ensure minimum clickable area
                            "&:hover": {
                                background: "rgba(255, 255, 255, 0.1)",
                                transform: "scale(1.02)",
                            },
                            "&:active": {
                                transform: "scale(0.98)",
                            },
                            // Add visual feedback for the toggle state
                            position: "relative",
                            "&::after": {
                                content: '""',
                                position: "absolute",
                                bottom: "4px",
                                left: "50%",
                                transform: "translateX(-50%)",
                                width: miniSidenav ? "6px" : "20px",
                                height: "2px",
                                background: "rgba(255, 255, 255, 0.4)",
                                borderRadius: "1px",
                                transition: "width 0.3s ease",
                            },
                        }}
                    >
                        <VuiBox
                            display="flex"
                            alignItems="center"
                            justifyContent={miniSidenav ? "center" : "center"}
                            sx={{
                                transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
                                width: "100%",
                            }}
                        >
                            <SpadeLogo
                                size={miniSidenav ? "28px" : "24px"}
                                style={{
                                    marginRight: miniSidenav ? "0px" : "8px",
                                    transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
                                }}
                            />
                            <VuiTypography
                                variant="button"
                                textGradient={true}
                                color="logo"
                                fontWeight="medium"
                                sx={{
                                    opacity: miniSidenav ? 0 : 1,
                                    maxWidth: miniSidenav ? "0px" : "120px",
                                    overflow: "hidden",
                                    whiteSpace: "nowrap",
                                    transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
                                    transform: miniSidenav ? "translateX(-20px)" : "translateX(0px)",
                                }}
                            >
                                {brandName}
                            </VuiTypography>
                        </VuiBox>
                    </VuiBox>
                </VuiBox>

                <Divider light />
                <List>{renderRoutes}</List>
            </SidenavRoot>
        </>
    );
}

// Setting default values for the props of Sidenav
Sidenav.defaultProps = {
    color: "info",
    brandName: "SPADE",
    enableHover: false, // Disabled by default - manual toggle only
};

// Typechecking props for the Sidenav
Sidenav.propTypes = {
    color: PropTypes.oneOf(["primary", "secondary", "info", "success", "warning", "error", "dark"]),
    brandName: PropTypes.string,
    routes: PropTypes.arrayOf(PropTypes.object).isRequired,
    enableHover: PropTypes.bool, // New prop to control hover behavior
};

export default Sidenav;