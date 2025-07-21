import React, { useEffect, useState, useRef } from 'react';
import './SpotifyLyrics.css';

const SpotifyLyrics = ({
  lyrics,
  isPlaying,
  trackProgress,
  trackDuration,
  currentTrack,
  loadingLyrics
}) => {
  const [parsedLyrics, setParsedLyrics] = useState([]);
  const [currentLineIndex, setCurrentLineIndex] = useState(0);
  const lyricsContainerRef = useRef(null);
  const scrollTimeoutRef = useRef(null);

  // Parse lyrics when they change
  useEffect(() => {
    if (!lyrics) return;

    const lines = lyrics.split(/\n+/)
      .filter(line => line.trim() !== '')
      .map((line) => ({
        text: line.trim(),
        isMetadata: /^\[.*\]$|^[A-Z\s]+:$/.test(line.trim())
      }));

    setParsedLyrics(lines);
  }, [lyrics]);

  // Calculate current line based on track progress with weights for line length
  useEffect(() => {
    if (!parsedLyrics.length || !trackDuration || trackDuration <= 0) return;

    // Add buffers (3% of total duration for intro, 1% for outro)
    const INTRO_BUFFER = trackDuration * 0.065;
    const OUTRO_BUFFER = trackDuration * 0.01;
    const EFFECTIVE_DURATION = Math.max(1, trackDuration - (INTRO_BUFFER + OUTRO_BUFFER));

    // Adjust progress to account for intro buffer
    const adjustedProgress = Math.max(0, trackProgress - INTRO_BUFFER);

    // Calculate progress percentage based on effective duration
    const progressPercentage = Math.min(1, Math.max(0, adjustedProgress / EFFECTIVE_DURATION));

    // Calculate weighted distribution based on line length and metadata
    const lineWeights = parsedLyrics.map(line => {
      if (line.isMetadata) return 0.3; // Metadata lines get less weight
      const lengthWeight = Math.min(2, Math.max(0.5, line.text.length / 25));
      return lengthWeight;
    });

    const totalWeight = lineWeights.reduce((a, b) => a + b, 0);

    // Find the line that corresponds to the current progress using weights
    let accumulatedWeight = 0;
    let currentIndex = 0;

    while (
      currentIndex < parsedLyrics.length - 1 &&
      accumulatedWeight / totalWeight < progressPercentage
    ) {
      accumulatedWeight += lineWeights[currentIndex];
      currentIndex++;
    }

    setCurrentLineIndex(currentIndex);
  }, [trackProgress, trackDuration, parsedLyrics]);

  // Smooth scrolling to current line with debounce
  useEffect(() => {
    if (!lyricsContainerRef.current || currentLineIndex < 0) return;

    // Clear any pending scroll
    if (scrollTimeoutRef.current) {
      clearTimeout(scrollTimeoutRef.current);
    }

    // Delay scrolling slightly for smoother experience
    scrollTimeoutRef.current = setTimeout(() => {
      const container = lyricsContainerRef.current;
      const targetLine = container.querySelector(`[data-index="${currentLineIndex}"]`);

      if (targetLine) {
        const containerHeight = container.clientHeight;
        const targetOffset = targetLine.offsetTop;
        const targetHeight = targetLine.clientHeight;

        // Center the current line in the container
        const scrollPosition = targetOffset - (containerHeight / 2) + (targetHeight / 2);

        container.scrollTo({
          top: scrollPosition,
          behavior: isPlaying ? 'smooth' : 'auto'
        });
      }
    }, 100);

    return () => {
      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
    };
  }, [currentLineIndex, isPlaying]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
    };
  }, []);

  // Get CSS class for a line based on its position relative to current line
  const getLineClass = (index) => {
    const distance = Math.abs(index - currentLineIndex);
    let className = 'lyrics-line';

    if (index === currentLineIndex) {
      className += ' current-line';
    } else if (distance <= 1) {
      className += ' secondary-highlight';
    } else if (distance <= 3) {
      className += ' secondary-highlight-faded';
    }

    if (index < currentLineIndex) className += ' passed';
    if (parsedLyrics[index]?.isMetadata) className += ' metadata';

    return className;
  };

  // Loading state
  if (loadingLyrics) {
    return (
      <div className="spotify-lyrics-container">
        <div className="lyrics-loading">
          <div className="lyrics-loading-spinner"></div>
          <div>Loading lyrics...</div>
        </div>
      </div>
    );
  }

  // No lyrics available
  if (!lyrics || lyrics.trim() === '') {
    return (
      <div className="spotify-lyrics-container">
        <div className="no-lyrics">
          <p>No lyrics available for this track</p>
        </div>
      </div>
    );
  }

  // Calculate line progress for the current line (0-100%) with line length weights
  const lineProgressPercent = (() => {
    if (!parsedLyrics.length || currentLineIndex < 0) return 0;

    // Add buffers (3% of total duration for intro, 1% for outro)
    const INTRO_BUFFER = trackDuration * 0.065;
    const OUTRO_BUFFER = trackDuration * 0.01;
    const EFFECTIVE_DURATION = Math.max(1, trackDuration - (INTRO_BUFFER + OUTRO_BUFFER));

    // Adjust progress to account for intro buffer
    const adjustedProgress = Math.max(0, trackProgress - INTRO_BUFFER);

    // Calculate progress percentage based on effective duration
    const progressPercentage = Math.min(1, Math.max(0, adjustedProgress / EFFECTIVE_DURATION));

    // Calculate weighted distribution based on line length and metadata
    const lineWeights = parsedLyrics.map(line => {
      if (line.isMetadata) return 0.3;
      const lengthWeight = Math.min(2, Math.max(0.5, line.text.length / 25));
      return lengthWeight;
    });

    const totalWeight = lineWeights.reduce((a, b) => a + b, 0);

    // Calculate start and end percentages for the current line
    let startPercent = 0;
    for (let i = 0; i < currentLineIndex; i++) {
      startPercent += lineWeights[i] / totalWeight;
    }

    const endPercent = startPercent + lineWeights[currentLineIndex] / totalWeight;

    // Calculate how far we are through the current line (0-100%)
    const lineProgressPercent = Math.max(0, Math.min(100,
      ((progressPercentage - startPercent) / (endPercent - startPercent)) * 100
    ));

    return lineProgressPercent;
  })();

  return (
    <div className="spotify-lyrics-container">
      <div className="lyrics-title">Lyrics</div>
      <div ref={lyricsContainerRef} className="lyrics-scroll-container">
        <div className="lyrics-content">
          {parsedLyrics.map((line, index) => (
            <div
              key={index}
              className={getLineClass(index)}
              data-index={index}
            >
              {line.text}
              {index === currentLineIndex && (
                <div
                  className="current-line-progress"
                  style={{ width: `${lineProgressPercent}%` }}
                />
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SpotifyLyrics;