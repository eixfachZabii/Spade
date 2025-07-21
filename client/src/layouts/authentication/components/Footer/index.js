/**/

// @mui material components
import Grid from "@mui/material/Grid";

// @mui icons

// Vision UI Dashboard React components
import VuiBox from "../../../../components/VuiBox";
import VuiTypography from "../../../../components/VuiTypography";

function Footer() {
  return (
    <VuiBox
      component="footer"
      py={6}
      sx={({ breakpoints }) => ({
        maxWidth: "450px",
        [breakpoints.down("xl")]: {
          maxWidth: "400px",
        },
      })}
    >
      <Grid container justifyContent="center">
        <Grid item xs={12} sx={{ textAlign: "center" }}>
          <VuiTypography
            variant="button"
            sx={{ textAlign: "center", fontWeight: "400 !important" }}
            color="text"
          >
            2025, Created for poker enthusiasts
          </VuiTypography>
        </Grid>
        <Grid item xs={10}>
          <VuiBox display="flex" justifyContent="center" flexWrap="wrap" mb={3}>
            <VuiBox mr={{ xs: "20px", lg: "46px" }}>
              <VuiTypography component="a" href="https://github.com/eixfachZabii/Spade" variant="body2" color="text" target="_blank" rel="noopener noreferrer">
                GitHub
              </VuiTypography>
            </VuiBox>
            <VuiBox>
              <VuiTypography component="a" href="/about" variant="body2" color="text">
                About
              </VuiTypography>
            </VuiBox>
          </VuiBox>
        </Grid>
      </Grid>
    </VuiBox>
  );
}

export default Footer;
