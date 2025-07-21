import React, { useRef, useState, useEffect } from "react";
import "./SpotifyPlayer.css";
import {
  BsPauseFill,
  BsPlayFill,
  BsSkipBackwardFill,
  BsSkipForwardFill,
} from "react-icons/bs";
import { PiShuffleBold } from "react-icons/pi";
import { ImVolumeHigh, ImVolumeLow, ImVolumeMedium, ImVolumeMute2 } from "react-icons/im";
import { MdSyncProblem } from "react-icons/md";
import SpotifyLyrics from './SpotifyLyrics';

// Import our Spotify context hook
import { useSpotify } from "../../../context/SpotifyContext";

const SpotifyPlayer = ({ useLyrics = true }) => {
  // Use the context including control state
  const {
    // Player state
    currentTrack,
    isPlaying,
    trackProgress,
    trackDuration,
    volume,
    isMuted,
    isShuffle,
    isControlBusy,
    isPlayerHealthy,

    // Lyrics
    lyrics,
    loadingLyrics,

    // Player controls
    togglePlay,
    skipToNext,
    skipToPrevious,
    seek,
    setVolume,
    toggleMute,
    setShuffle,

    // Helper functions
    formatDuration,
  } = useSpotify();

  // Refs for the interactive elements
  const progressBarRef = useRef(null);
  const volumeBarRef = useRef(null);

  // State for dragging progress
  const [isDragging, setIsDragging] = useState(false);
  const [dragProgress, setDragProgress] = useState(null);
  const [dragText, setDragText] = useState("");

  // State for dragging volume
  const [isDraggingVolume, setIsDraggingVolume] = useState(false);
  const [dragVolume, setDragVolume] = useState(null);

  // Calculate progress percentage for the progress bar
  const progressPercentage = isDragging
    ? (dragProgress / trackDuration) * 100
    : (trackProgress / trackDuration) * 100 || 0;

  // Calculate volume percentage
  const volumePercentage = isDraggingVolume
    ? dragVolume
    : (isMuted ? 0 : volume);

  // PROGRESS BAR INTERACTIONS

  // Helper function to calculate position from mouse/touch event for progress bar
  const getPositionFromEvent = (e, ref, maxValue) => {
    if (!ref.current || !maxValue) return 0;

    const rect = ref.current.getBoundingClientRect();

    // Get horizontal position (handle both mouse and touch events)
    const clientX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 0);

    const clickPositionX = clientX - rect.left;
    const elementWidth = rect.width;

    // Calculate position as a percentage (0-1)
    const positionRatio = Math.max(0, Math.min(1, clickPositionX / elementWidth));

    // Convert to value based on maxValue
    return Math.floor(positionRatio * maxValue);
  };

  // Handle mouse/touch down on progress bar
  const handleDragStart = (e) => {
    if (isControlBusy || !isPlayerHealthy) return;

    e.preventDefault();
    const position = getPositionFromEvent(e, progressBarRef, trackDuration);
    setIsDragging(true);
    setDragProgress(position);
    setDragText(formatDuration(position));

    // Add global event listeners for drag and release
    document.addEventListener('mousemove', handleDragMove);
    document.addEventListener('touchmove', handleDragMove);
    document.addEventListener('mouseup', handleDragEnd);
    document.addEventListener('touchend', handleDragEnd);
  };

  // Handle mouse/touch move during progress drag
  const handleDragMove = (e) => {
    if (!isDragging) return;
    e.preventDefault();

    const position = getPositionFromEvent(e, progressBarRef, trackDuration);
    setDragProgress(position);
    setDragText(formatDuration(position));
  };

  // Handle progress drag end - seek to the position
  const handleDragEnd = (e) => {
    if (!isDragging) return;
    e.preventDefault();

    // Remove global event listeners
    document.removeEventListener('mousemove', handleDragMove);
    document.removeEventListener('touchmove', handleDragMove);
    document.removeEventListener('mouseup', handleDragEnd);
    document.removeEventListener('touchend', handleDragEnd);

    // Seek to the drag position
    seek(dragProgress);

    // Reset drag state
    setIsDragging(false);
    setDragProgress(null);
  };

  // Handle click on progress bar (for direct jumps)
  const handleProgressClick = (e) => {
    if (!progressBarRef.current || !trackDuration || isControlBusy || !isPlayerHealthy) return;

    // Only treat it as a click if we're not already dragging
    if (!isDragging) {
      const position = getPositionFromEvent(e, progressBarRef, trackDuration);
      seek(position);
    }
  };

  // VOLUME SLIDER INTERACTIONS

  // Handle mouse/touch down on volume slider
  const handleVolumeDragStart = (e) => {
    if (isControlBusy || !isPlayerHealthy) return;

    e.preventDefault();
    const newVolume = getPositionFromEvent(e, volumeBarRef, 100);
    setIsDraggingVolume(true);
    setDragVolume(newVolume);

    // Add global event listeners for drag and release
    document.addEventListener('mousemove', handleVolumeDragMove);
    document.addEventListener('touchmove', handleVolumeDragMove);
    document.addEventListener('mouseup', handleVolumeDragEnd);
    document.addEventListener('touchend', handleVolumeDragEnd);
  };

  // Handle mouse/touch move during volume drag
  const handleVolumeDragMove = (e) => {
    if (!isDraggingVolume) return;
    e.preventDefault();

    const newVolume = getPositionFromEvent(e, volumeBarRef, 100);
    setDragVolume(newVolume);
  };

  // Handle volume drag end - set the volume
  const handleVolumeDragEnd = (e) => {
    if (!isDraggingVolume) return;
    e.preventDefault();

    // Remove global event listeners
    document.removeEventListener('mousemove', handleVolumeDragMove);
    document.removeEventListener('touchmove', handleVolumeDragMove);
    document.removeEventListener('mouseup', handleVolumeDragEnd);
    document.removeEventListener('touchend', handleVolumeDragEnd);

    const newVolume = dragVolume;

    // Set the new volume
    setVolume(newVolume);

    // Toggle mute based on the new volume
    if (newVolume === 0) {
      if (!isMuted) {
        toggleMute(); // Mute if volume is 0 and not already muted
      }
    } else {
      if (isMuted) {
        toggleMute(); // Unmute if volume is not 0 and currently muted
      }
    }

    // Reset drag state
    setIsDraggingVolume(false);
    setDragVolume(null);
  };

  // Handle click on volume bar (for direct volume change)
  const handleVolumeClick = (e) => {
    if (!volumeBarRef.current || isControlBusy || !isPlayerHealthy) return;

    // Only treat it as a click if we're not already dragging
    if (!isDraggingVolume) {
      const newVolume = getPositionFromEvent(e, volumeBarRef, 100);
      setVolume(newVolume);
    }
  };

  // Clean up event listeners when component unmounts
  useEffect(() => {
    // Clean up function
    const cleanupListeners = () => {
      // Progress bar listeners
      document.removeEventListener('mousemove', handleDragMove);
      document.removeEventListener('touchmove', handleDragMove);
      document.removeEventListener('mouseup', handleDragEnd);
      document.removeEventListener('touchend', handleDragEnd);

      // Volume slider listeners
      document.removeEventListener('mousemove', handleVolumeDragMove);
      document.removeEventListener('touchmove', handleVolumeDragMove);
      document.removeEventListener('mouseup', handleVolumeDragEnd);
      document.removeEventListener('touchend', handleVolumeDragEnd);
    };

    return cleanupListeners;
  }, [isDragging, isDraggingVolume]);

  // Helper function to get appropriate volume icon based on volume level
  const getVolumeIcon = (volume, isMuted) => {
    if (isMuted) return <ImVolumeMute2 size="25px" />;
    if (volume <= 33) return <ImVolumeLow size="25px" />;
    if (volume <= 66) return <ImVolumeMedium size="25px" />;
    return <ImVolumeHigh size="25px" />;
  };

  return (
    <div className="spotify-player-container">
      {/* Left side: Song playback */}
      <div className="playback-container">
        <div className="track-info">
          {currentTrack ? (
            <img
              src={currentTrack.album.images[0].url}
              alt={currentTrack.name}
              className="track-image"
              style={isPlayerHealthy ? {} : { opacity: 0.5, filter: "grayscale(50%)" }}
            />
          ) : (
            <img
              src="https://image-cdn-ak.spotifycdn.com/image/ab67706c0000d72cdd7cb0d442bee004f48dee14"
              alt="Placeholder"
              className="track-image"
            />
          )}
          <div className="track-details">
            {currentTrack ? (
              <div>
                <p className="track-name">
                  <strong>{currentTrack.name}</strong>
                </p>
                <p className="track-artist">
                  {currentTrack.artists.map((a) => a.name).join(", ")}
                </p>
                {!isPlayerHealthy && (
                  <div className="player-status warning">
                    <MdSyncProblem size="14px" />
                    <span>Reconnecting to Spotify...</span>
                  </div>
                )}
              </div>
            ) : (
              <div></div>
            )}
            <p className="track-duration">
              {isDragging ? dragText : formatDuration(trackProgress)} / {formatDuration(trackDuration)}
            </p>
            <div
              className={`progress-bar-container ${isDragging ? 'seeking' : ''}`}
              ref={progressBarRef}
              onClick={handleProgressClick}
              onMouseDown={handleDragStart}
              onTouchStart={handleDragStart}
              style={{
                cursor: isControlBusy || !isPlayerHealthy ? 'not-allowed' : 'pointer',
                position: 'relative'
              }}
            >
              <div
                className="progress-bar"
                style={{ width: `${progressPercentage}%` }}
              ></div>

              {/* Dragging tooltip */}
              {isDragging && (
                <div
                  className="progress-tooltip"
                  style={{
                    left: `${progressPercentage}%`,
                    opacity: 1
                  }}
                >
                  {dragText}
                </div>
              )}

              {/* Dragging handle */}
              {isDragging && (
                <div
                  className="progress-handle"
                  style={{ left: `${progressPercentage}%` }}
                ></div>
              )}
            </div>
            <div className="button-container">
              <button
                className={`play-pause-button ${isControlBusy || !isPlayerHealthy ? 'disabled-control' : ''}`}
                onClick={skipToPrevious}
                disabled={isControlBusy || !isPlayerHealthy}
              >
                <BsSkipBackwardFill size="25px" color="inherit" />
              </button>
              <button
                className={`play-pause-button ${isControlBusy || !isPlayerHealthy ? 'disabled-control' : ''}`}
                onClick={togglePlay}
                disabled={isControlBusy || !isPlayerHealthy}
              >
                {isPlaying ? (
                  <BsPauseFill size="25px" color="inherit" />
                ) : (
                  <BsPlayFill size="25px" color="inherit" />
                )}
              </button>
              <button
                className={`play-pause-button ${isControlBusy || !isPlayerHealthy ? 'disabled-control' : ''}`}
                onClick={skipToNext}
                disabled={isControlBusy || !isPlayerHealthy}
              >
                <BsSkipForwardFill size="25px" color="inherit" />
              </button>
              <button
                className={`play-pause-button ${isShuffle ? "is-shuffle-active" : ""} ${isControlBusy || !isPlayerHealthy ? 'disabled-control' : ''}`}
                onClick={setShuffle}
                disabled={isControlBusy || !isPlayerHealthy}
              >
                <PiShuffleBold size="25px" color="inherit" />
              </button>
            </div>
            {/* Volume Controls */}
            <div className="volume-container">
              <button
                className={`volume-button ${isControlBusy || !isPlayerHealthy ? 'disabled-control' : ''}`}
                onClick={toggleMute}
                disabled={isControlBusy || !isPlayerHealthy}
              >
                {getVolumeIcon(isDraggingVolume ? dragVolume : volume, isMuted)}
              </button>

              {/* Custom draggable volume control */}
              <div
                className={`custom-volume-slider ${isDraggingVolume ? 'dragging' : ''}`}
                ref={volumeBarRef}
                onClick={handleVolumeClick}
                onMouseDown={handleVolumeDragStart}
                onTouchStart={handleVolumeDragStart}
                style={{
                  opacity: isControlBusy || !isPlayerHealthy ? 0.5 : 1,
                  cursor: isControlBusy || !isPlayerHealthy ? 'not-allowed' : 'pointer'
                }}
              >
                <div
                  className="volume-slider-track"
                >
                  <div
                    className="volume-slider-fill"
                    style={{ width: `${volumePercentage}%` }}
                  ></div>

                  {/* Volume handle */}
                  <div
                    className={`volume-slider-handle ${isDraggingVolume ? 'active' : ''}`}
                    style={{ left: `${volumePercentage}%` }}
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right side: Lyrics (conditional) */}
      {useLyrics && (
        <div className="lyrics-container">
          <SpotifyLyrics
            lyrics={lyrics}
            isPlaying={isPlaying}
            trackProgress={trackProgress}
            trackDuration={trackDuration}
            currentTrack={currentTrack}
            loadingLyrics={loadingLyrics}
          />
        </div>
      )}
    </div>
  );
};

export default SpotifyPlayer;