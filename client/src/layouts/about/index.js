import { useState, useEffect } from "react";

// @mui material components
import Grid from "@mui/material/Grid";
import Card from "@mui/material/Card";
import Icon from "@mui/material/Icon";

// Vision UI Dashboard React components
import VuiBox from "../../components/VuiBox";
import VuiTypography from "../../components/VuiTypography";
import VuiButton from "../../components/VuiButton";
import VuiAvatar from "../../components/VuiAvatar";

// Vision UI Dashboard React example components
import PageLayout from "../../examples/LayoutContainers/PageLayout";

// Authentication layout components
import AuthNavbar from "../authentication/components/AuthNavbar";
import Footer from "../authentication/components/Footer";

// Vision UI Dashboard React base styles
import colors from "../../assets/theme/base/colors";
import typography from "../../assets/theme/base/typography";
import borders from "../../assets/theme/base/borders";

// Vision UI Dashboard React theme functions
import tripleLinearGradient from "../../assets/theme/functions/tripleLinearGradient";

function About() {
  const { gradients } = colors;
  const { size } = typography;
  const { borderRadius } = borders;
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    setVisible(true);
  }, []);

  const teamMembers = [
    {
      name: 'Markus Huber',
      role: 'Software Architect & Full-Stack Developer',
      bio: 'Information Systems student at TUM (B.Sc.) with expertise in software architecture, full-stack development, and machine learning.',
      avatar: 'https://d112y698adiu2z.cloudfront.net/photos/production/user_photos/003/508/125/datas/profile.jpg',
      linkedin: 'https://www.linkedin.com/in/markus-huber-0132282bb/',
      github: 'https://github.com/M4RKUS28'
    },
    {
      name: 'Luca Bozzetti',
      role: 'AI Researcher & Agent Developer',
      bio: 'Information Systems student at TUM (B.Sc.) with a strong passion for AI, machine learning, and agentic applications.',
      avatar: 'https://poker-spade.de/static/media/Luca.658c06336387cd26c193.jpeg',
      linkedin: 'https://www.linkedin.com/in/luca-bozzetti-371379282/',
      github: 'https://github.com/lucabzt'
    },
    {
      name: 'Sebastian Rogg',
      role: 'Frontend Developer & Database Engineer',
      bio: 'Information Systems student at TUM (B.Sc.) experienced in building a user friendly environment.',
      avatar: 'https://avatars.githubusercontent.com/u/144535689?v=4',
      linkedin: 'https://www.linkedin.com/in/sebastian-rogg/',
      github: 'https://github.com/eixfachZabii'
    },
    {
      name: 'Matthias Meierlohr',
      role: 'Frontend Designer & UX Specialist',
      bio: 'Information Systems student at TUM (B.Sc.) specialised in building clean and modern UX design.',
      avatar: 'https://via.placeholder.com/150/1a1a1a/ffffff?text=MM',
      linkedin: 'https://www.linkedin.com/in/matthias-meierlohr',
      github: 'https://github.com/Maths24'
    },
    {
      name: 'Jonas Hörter',
      role: 'Backend Developer',
      bio: 'Information Systems student at TUM (B.Sc.) with a strong passion for business applications.',
      avatar: 'https://poker-spade.de/static/media/Jonas.2327447cc8a67b962465.jpeg',
      linkedin: 'https://www.linkedin.com/in/jonas-hörter-4b22562bb/',
      github: 'https://github.com/Erliassystems'
    },
    {
      name: 'Paul Vorderbrügge',
      role: 'Beer Lover & Backend Specialist',
      bio: 'Computer Science student at TUM (B.Sc.) Focused on creating intuitive, user-friendly interfaces for AI applications.',
      avatar: 'https://via.placeholder.com/150/1a1a1a/ffffff?text=PV',
      linkedin: 'https://www.linkedin.com/in/paul-vorderbruegge/',
      github: 'https://github.com/paulvorderbruegge'
    },
  ];

  const features = [
    {
      icon: "casino",
      title: "Advanced Poker Engine",
      description: "State-of-the-art poker game mechanics with real-time multiplayer support."
    },
    {
      icon: "analytics",
      title: "Detailed Analytics",
      description: "Comprehensive statistics and performance tracking for serious players."
    },
    {
      icon: "security",
      title: "Secure Gaming",
      description: "Enterprise-level security ensuring fair play and data protection."
    },
    {
      icon: "groups",
      title: "Community Focused",
      description: "Built for poker enthusiasts by poker enthusiasts."
    }
  ];

  return (
    <PageLayout
      background={tripleLinearGradient(
        gradients.cover.main,
        gradients.cover.state,
        gradients.cover.stateSecondary,
        gradients.cover.angle
      )}
    >
      <AuthNavbar />
      <VuiBox py={3} px={3}>
        {/* Hero Section */}
        <VuiBox
          display="flex"
          alignItems="center"
          position="relative"
          minHeight="50vh"
          borderRadius="xl"
          sx={{
            backgroundImage: `linear-gradient(rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.6))`,
            backgroundSize: "cover",
            backgroundPosition: "center",
            transform: visible ? "translateY(0)" : "translateY(50px)",
            opacity: visible ? 1 : 0,
            transition: "all 0.8s ease-in-out",
            border: "1px solid rgba(255, 255, 255, 0.1)",
          }}
        >
          <VuiBox
            width="100%"
            display="flex"
            flexDirection="column"
            alignItems="center"
            textAlign="center"
            px={3}
          >
            <VuiTypography
              variant="h1"
              color="white"
              fontWeight="bold"
              sx={{
                fontSize: { xs: "2.5rem", md: "3.5rem" },
                background: `linear-gradient(45deg, ${gradients.logo.main}, ${gradients.logo.state})`,
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                backgroundClip: "text",
                mb: 2,
                letterSpacing: "2px"
              }}
            >
              About SPADE
            </VuiTypography>
            
            <VuiTypography
              variant="h4"
              color="white"
              fontWeight="regular"
              sx={{ maxWidth: "600px", mb: 4, opacity: 0.9 }}
            >
              The ultimate poker platform designed for serious players and casual enthusiasts alike
            </VuiTypography>

            <VuiButton
              component="a"
              href="/authentication/sign-up"
              variant="gradient"
              color="info"
              size="large"
              sx={{
                background: `linear-gradient(45deg, ${gradients.info.main}, ${gradients.info.state})`,
                px: 4,
                py: 1.5,
                fontSize: size.md,
                fontWeight: "medium"
              }}
            >
              <Icon sx={{ mr: 1 }}>casino</Icon>
              Start Playing
            </VuiButton>
          </VuiBox>
        </VuiBox>

        {/* Mission Section */}
        <VuiBox mt={6} mb={6}>
          <Card
            sx={{
              background: `linear-gradient(135deg, ${gradients.dark.main}, ${gradients.dark.state})`,
              backdropFilter: "blur(42px)",
              border: "1px solid rgba(255, 255, 255, 0.1)",
            }}
          >
            <VuiBox p={4} textAlign="center">
              <VuiBox display="flex" justifyContent="center" mb={3}>
                <VuiBox
                  display="flex"
                  alignItems="center"
                  justifyContent="center"
                  width="80px"
                  height="80px"
                  borderRadius="50%"
                  sx={{
                    background: `linear-gradient(45deg, ${gradients.logo.main}, ${gradients.logo.state})`,
                  }}
                >
                  <Icon sx={{ color: "white", fontSize: "2rem" }}>lightbulb</Icon>
                </VuiBox>
              </VuiBox>

              <VuiTypography variant="h3" color="white" fontWeight="bold" mb={2}>
                Our Mission
              </VuiTypography>

              <VuiTypography variant="body1" color="text" sx={{ maxWidth: "800px", mx: "auto", mb: 4 }}>
                SPADE is dedicated to revolutionizing the online poker experience by combining cutting-edge technology 
                with the timeless appeal of poker. We strive to create a platform that serves both newcomers learning 
                the game and seasoned professionals honing their skills. Our mission is to democratize high-quality poker 
                gaming by creating AI-powered experiences that are as unique as each player.
              </VuiTypography>

              <Grid container spacing={3} justifyContent="center">
                {features.map((feature, index) => (
                  <Grid item xs={12} sm={6} md={3} key={index}>
                    <Card
                      sx={{
                        height: "100%",
                        background: `linear-gradient(135deg, ${gradients.cardDark.main}, ${gradients.cardDark.state})`,
                        border: "1px solid rgba(255, 255, 255, 0.1)",
                        transition: "transform 0.3s ease, box-shadow 0.3s ease",
                        "&:hover": {
                          transform: "translateY(-5px)",
                          boxShadow: "0 20px 40px rgba(0, 0, 0, 0.3)",
                        }
                      }}
                    >
                      <VuiBox p={3} textAlign="center">
                        <VuiBox
                          display="flex"
                          alignItems="center"
                          justifyContent="center"
                          width="50px"
                          height="50px"
                          borderRadius="50%"
                          sx={{
                            background: `linear-gradient(45deg, ${gradients.info.main}, ${gradients.info.state})`,
                            mx: "auto",
                            mb: 2
                          }}
                        >
                          <Icon sx={{ color: "white", fontSize: "1.5rem" }}>{feature.icon}</Icon>
                        </VuiBox>
                        <VuiTypography variant="h6" color="white" fontWeight="bold" mb={1}>
                          {feature.title}
                        </VuiTypography>
                        <VuiTypography variant="body2" color="text">
                          {feature.description}
                        </VuiTypography>
                      </VuiBox>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            </VuiBox>
          </Card>
        </VuiBox>

        {/* Team Section */}
        <VuiBox mb={6}>
          <VuiTypography variant="h3" color="white" fontWeight="bold" textAlign="center" mb={4}>
            Meet Our Team
          </VuiTypography>
          
          <Grid container spacing={3}>
            {teamMembers.map((member, index) => (
              <Grid item xs={12} sm={6} md={4} key={index}>
                <Card
                  sx={{
                    height: "100%",
                    background: `linear-gradient(135deg, ${gradients.cardDark.main}, ${gradients.cardDark.state})`,
                    border: "1px solid rgba(255, 255, 255, 0.1)",
                    transition: "transform 0.3s ease, box-shadow 0.3s ease",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow: "0 20px 40px rgba(0, 0, 0, 0.3)",
                    }
                  }}
                >
                  <VuiBox p={3} textAlign="center">
                    <VuiAvatar
                      src={member.avatar}
                      alt={member.name}
                      size="xl"
                      sx={{ mx: "auto", mb: 2 }}
                    />
                    
                    <VuiTypography variant="h6" color="white" fontWeight="bold" mb={1}>
                      {member.name}
                    </VuiTypography>
                    
                    <VuiBox
                      display="inline-block"
                      px={2}
                      py={0.5}
                      borderRadius="lg"
                      sx={{
                        background: `linear-gradient(45deg, ${gradients.info.main}, ${gradients.info.state})`,
                        mb: 2
                      }}
                    >
                      <VuiTypography variant="caption" color="white" fontWeight="medium">
                        {member.role}
                      </VuiTypography>
                    </VuiBox>

                    <VuiTypography variant="body2" color="text" mb={2}>
                      {member.bio}
                    </VuiTypography>

                    <VuiButton
                      component="a"
                      href={member.github}
                      target="_blank"
                      rel="noopener noreferrer"
                      variant="outlined"
                      color="info"
                      size="small"
                      sx={{ mt: 1 }}
                    >
                      <Icon sx={{ mr: 1, fontSize: "1rem" }}>code</Icon>
                      GitHub
                    </VuiButton>
                    <VuiButton
                      component="a"
                      href={member.linkedin}
                      target="_blank"
                      rel="noopener noreferrer"
                      variant="outlined"
                      color="info"
                      size="small"
                      sx={{ mt: 1 }}
                    >
                      <Icon sx={{ mr: 1, fontSize: "1rem" }}>work</Icon>
                      LinkedIn
                    </VuiButton>
                  </VuiBox>
                </Card>
              </Grid>
            ))}
          </Grid>
        </VuiBox>

        {/* CTA Section */}
        <Card
          sx={{
            background: `linear-gradient(135deg, ${gradients.info.main}, ${gradients.info.state})`,
            border: "none",
          }}
        >
          <VuiBox p={4} textAlign="center">
            <VuiTypography variant="h3" color="white" fontWeight="bold" mb={2}>
              Ready to Join the Game?
            </VuiTypography>
            <VuiTypography variant="body1" color="white" sx={{ opacity: 0.9, mb: 3 }}>
              Experience the future of online poker with SPADE. Join thousands of players worldwide.
            </VuiTypography>
            <VuiButton
              component="a"
              href="/authentication/sign-up"
              variant="contained"
              color="white"
              size="large"
              sx={{
                color: gradients.info.main,
                fontWeight: "bold",
                px: 4,
                py: 1.5,
                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 10px 20px rgba(0, 0, 0, 0.2)",
                }
              }}
            >
              Get Started Now
            </VuiButton>
          </VuiBox>
        </Card>
      </VuiBox>
      <Footer />
    </PageLayout>
  );
}

export default About;
