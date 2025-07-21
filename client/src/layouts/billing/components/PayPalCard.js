import React from "react";
import { Card, Box } from "@mui/material";
import PropTypes from "prop-types";
import QRCodeImage from "assets/images/PayPal-QR-Code.png";
import { FaCcPaypal } from "react-icons/fa6";

// Vision UI Dashboard React components
import VuiBox from "components/VuiBox";
import VuiTypography from "components/VuiTypography";

function ModernPayPalCard({ userName, amount }) {
  return (
    <Card
      sx={{
        position: "relative",
        overflow: "hidden",
        background: "linear-gradient(145deg, #0070ba 0%, #1546a0 35%, #003087 100%)",
        borderRadius: "24px",
        boxShadow: "0 20px 40px rgba(0, 53, 128, 0.18), 0 1px 3px rgba(0, 0, 0, 0.1)",
        padding: "30px",
      }}
    >
      {/* Decorative background elements */}
      <Box
        sx={{
          position: "absolute",
          top: "-10%",
          right: "-5%",
          width: "200px",
          height: "200px",
          background: "radial-gradient(circle, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 70%)",
          borderRadius: "50%",
          zIndex: 0,
        }}
      />
      <Box
        sx={{
          position: "absolute",
          bottom: "-15%",
          left: "-10%",
          width: "250px",
          height: "250px",
          background: "radial-gradient(circle, rgba(255,255,255,0.08) 0%, rgba(255,255,255,0) 70%)",
          borderRadius: "50%",
          zIndex: 0,
        }}
      />

      {/* Content container */}
      <VuiBox position="relative" zIndex="1">
        <VuiBox display="flex" alignItems="center" mb={3}>
          <VuiBox
            sx={{
              backgroundColor: "white",
              borderRadius: "12px",
              padding: "10px",
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              boxShadow: "0 8px 16px rgba(0, 0, 0, 0.1)",
              mr: 2,
            }}
          >
            <FaCcPaypal size={36} color="#003087" />
          </VuiBox>
          <VuiBox>
            <VuiTypography
              variant="button"
              color="white"
              fontWeight="regular"
              opacity={0.8}
              textTransform="uppercase"
              letterSpacing="1px"
              fontSize="0.75rem"
            >
              PayPal QR-Code
            </VuiTypography>
            <VuiTypography variant="h4" color="white" fontWeight="bold">

            </VuiTypography>
          </VuiBox>
        </VuiBox>

        {/* QR Code Section */}
        <VuiBox
          display="flex"
          flexDirection="column"
          alignItems="center"
          sx={{
            backgroundColor: "rgba(255, 255, 255, 0.95)",
            borderRadius: "20px",
            padding: "30px 20px",
            boxShadow: "0 15px 35px rgba(0, 53, 128, 0.15)",
          }}
        >
          <VuiTypography
            color="#003087"
            variant="h5"
            fontWeight="bold"
            textAlign="center"
            mb={2}
          >
            {userName}
          </VuiTypography>

          <VuiBox
            position="relative"
            sx={{
              padding: "10px",
              borderRadius: "16px",
              backgroundColor: "white",
              boxShadow: "0 8px 16px rgba(0, 0, 0, 0.05)",
              border: "1px solid rgba(0, 0, 0, 0.08)",
              width: "fit-content",
              marginBottom: "20px",
            }}
          >
            <Box
              component="img"
              src={QRCodeImage}
              alt="PayPal QR Code"
              sx={{
                width: "180px",
                height: "180px",
                display: "block",
              }}
            />
            <Box
              sx={{
                position: "absolute",
                right: "-10px",
                top: "-10px",
                backgroundColor: "#0070ba",
                borderRadius: "50%",
                width: "30px",
                height: "30px",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
              }}
            >
              <FaCcPaypal size={16} color="white" />
            </Box>
          </VuiBox>

          <VuiTypography
            variant="button"
            color="text"
            fontWeight="medium"
          >
            Bank gewinnt immer ♠️💸
          </VuiTypography>
        </VuiBox>
      </VuiBox>
    </Card>
  );
}

ModernPayPalCard.defaultProps = {
  userName: "Sebastian Rogg",
  amount: "1,000.80",
};

ModernPayPalCard.propTypes = {
  userName: PropTypes.string,
  amount: PropTypes.string,
};

export default ModernPayPalCard;
