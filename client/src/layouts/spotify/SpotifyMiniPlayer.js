import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Card, IconButton, Box, Typography, Tooltip } from "@mui/material";
import {
  PlayArrow,
  Pause,
  SkipNext,
  SkipPrevious,
  MusicNote,
  OpenInFull,
  SyncProblem,
} from "@mui/icons-material";
import { useSpotify } from "../../context/SpotifyContext";

// Styles
const miniPlayerStyles = {
  container: {
    position: "fixed",
    bottom: "20px",
    right: "20px",
    zIndex: 1000,
    width: "300px",
    backgroundColor: "rgba(18, 18, 38, 0.8)",
    backdropFilter: "blur(10px)",
    borderRadius: "10px",
    boxShadow: "0 4px 30px rgba(0, 0, 0, 0.3)",
    padding: "10px",
    display: "flex",
    flexDirection: "row",
    alignItems: "center",
    gap: "10px",
    transition: "all 0.3s ease",
    border: "1px solid rgba(255, 255, 255, 0.1)",
    "&:hover": {
      transform: "translateY(-5px)",
      boxShadow: "0 6px 30px rgba(0, 0, 0, 0.4)",
    }
  },
  albumArt: {
    width: "60px",
    height: "60px",
    borderRadius: "6px",
    objectFit: "cover",
  },
  trackInfo: {
    display: "flex",
    flexDirection: "column",
    flexGrow: 1,
    overflow: "hidden",
  },
  trackName: {
    color: "#fff",
    fontSize: "0.9rem",
    fontWeight: "bold",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
  },
  artistName: {
    color: "rgba(255, 255, 255, 0.7)",
    fontSize: "0.8rem",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
  },
  controls: {
    display: "flex",
    alignItems: "center",
  },
  controlButton: {
    color: "#fff",
    padding: "5px",
  },
  disabledButton: {
    color: "rgba(255, 255, 255, 0.5)",
    padding: "5px",
    cursor: "not-allowed",
  },
  progressBar: {
    width: "100%",
    height: "3px",
    backgroundColor: "rgba(255, 255, 255, 0.2)",
    borderRadius: "3px",
    marginTop: "5px",
    position: "relative",
  },
  progress: {
    height: "100%",
    backgroundColor: "#1DB954", // Spotify green
    borderRadius: "3px",
    transition: "width 0.1s linear",
  },
  expandButton: {
    color: "rgba(255, 255, 255, 0.7)",
    padding: "4px",
  },
  noTrackContainer: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "10px",
  },
  noTrackText: {
    color: "rgba(255, 255, 255, 0.7)",
    fontSize: "0.9rem",
  }
};

const SpotifyMiniPlayer = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const {
    // Authentication & state
    token,
    isReady,

    // Player state
    currentTrack,
    isPlaying,
    trackProgress,
    trackDuration,
    isControlBusy,
    isPlayerHealthy,

    // Player controls
    togglePlay,
    skipToNext,
    skipToPrevious,
  } = useSpotify();

  const progressPercentage = (trackProgress / trackDuration) * 100 || 0;

  const handleExpandClick = () => {
    navigate("/spotify"); // Navigate to the full Spotify page
  };

  // Don't show the mini player on the Spotify page or if there's no token
  if (location.pathname === "/spotify" || !token) {
    return null;
  }

  // Calculate if controls should be disabled
  const areControlsDisabled = isControlBusy || !isPlayerHealthy;

  return (
    <Card sx={miniPlayerStyles.container}>
      {currentTrack ? (
        <>
          <img
            src={currentTrack.album.images[0].url}
            alt={currentTrack.name}
            style={{
              ...miniPlayerStyles.albumArt,
              opacity: isPlayerHealthy ? 1 : 0.6,
              filter: isPlayerHealthy ? "none" : "grayscale(50%)"
            }}
          />
          <Box sx={miniPlayerStyles.trackInfo}>
              <Typography sx={miniPlayerStyles.trackName}>
                {currentTrack.name}
              </Typography>
            <Typography sx={miniPlayerStyles.artistName}>
              {currentTrack.artists.map((a) => a.name).join(", ")}
            </Typography>
            <Box sx={miniPlayerStyles.progressBar}>
              <Box
                sx={{
                  ...miniPlayerStyles.progress,
                  width: `${progressPercentage}%`,
                }}
              />
            </Box>
          </Box>
          <Box sx={miniPlayerStyles.controls}>
            <Tooltip title={areControlsDisabled ? "Player reconnecting..." : "Previous"}>
              <span>
                <IconButton
                  onClick={skipToPrevious}
                  sx={areControlsDisabled ? miniPlayerStyles.disabledButton : miniPlayerStyles.controlButton}
                  size="small"
                  disabled={areControlsDisabled}
                >
                  <SkipPrevious fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>

            <Tooltip title={areControlsDisabled ? "Player reconnecting..." : (isPlaying ? "Pause" : "Play")}>
              <span>
                <IconButton
                  onClick={togglePlay}
                  sx={areControlsDisabled ? miniPlayerStyles.disabledButton : miniPlayerStyles.controlButton}
                  size="small"
                  disabled={areControlsDisabled}
                >
                  {isPlaying ? <Pause fontSize="small" /> : <PlayArrow fontSize="small" />}
                </IconButton>
              </span>
            </Tooltip>

            <Tooltip title={areControlsDisabled ? "Player reconnecting..." : "Next"}>
              <span>
                <IconButton
                  onClick={skipToNext}
                  sx={areControlsDisabled ? miniPlayerStyles.disabledButton : miniPlayerStyles.controlButton}
                  size="small"
                  disabled={areControlsDisabled}
                >
                  <SkipNext fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>

            <Tooltip title="Open full player">
              <IconButton onClick={handleExpandClick} sx={miniPlayerStyles.expandButton} size="small">
                <OpenInFull fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
        </>
      ) : (
        <Box sx={miniPlayerStyles.noTrackContainer}>
          {!isPlayerHealthy ? (
            <SyncProblem sx={{ color: "rgba(255, 255, 255, 0.5)", marginRight: "8px" }} />
          ) : (
            <MusicNote sx={{ color: "rgba(255, 255, 255, 0.5)", marginRight: "8px" }} />
          )}
          <Typography sx={miniPlayerStyles.noTrackText}>
            {!isPlayerHealthy ? "Reconnecting..." : "No track playing"}
          </Typography>
          <IconButton onClick={handleExpandClick} sx={miniPlayerStyles.expandButton} size="small">
            <OpenInFull fontSize="small" />
          </IconButton>
        </Box>
      )}
    </Card>
  );
};

export default SpotifyMiniPlayer;