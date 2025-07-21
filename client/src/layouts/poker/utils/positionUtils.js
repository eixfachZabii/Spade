/**
 * Enhanced positioning utility for poker players
 * Creates a perfect oval regardless of container dimensions or aspect ratio
 * Optimized for compact player layout
 */
export const getPlayerPositions = (numPlayers, containerWidth, containerHeight) => {
  // Calculate optimal oval parameters based on container dimensions
  // Increased ellipse size to accommodate more compact player boxes
  const tableWidth = containerWidth * 0.88;  // Increased from 0.85
  const tableHeight = containerHeight * 0.72; // Increased from 0.72

  // Calculate ellipse parameters (semi-major and semi-minor axes)
  const a = tableWidth * 0.47; // horizontal semi-axis (increased from 0.45)
  const b = tableHeight * 0.52; // vertical semi-axis (increased from 0.5)

  // Center point of the table - moved slightly higher
  const centerX = containerWidth / 2;
  const centerY = containerHeight / 2 - 20; // Moved higher by 5px

  // Calculate positions around the ellipse
  // Starting position is at the bottom of the ellipse (closest to the user)
  const positions = [];

  // Special case handling for different numbers of players
  // This ensures optimal distribution around the table
  let startAngle, endAngle;

  if (numPlayers <= 2) {
    // For 2 players, position them at opposite ends of the table
    startAngle = -Math.PI / 2; // top
    endAngle = Math.PI / 2;    // bottom
  } else if (numPlayers <= 4) {
    // For 3-4 players, use 3/4 of the ellipse (leave bottom quarter empty)
    startAngle = -Math.PI * 7/8;
    endAngle = Math.PI * 7/8;
  } else if (numPlayers <= 6) {
    // For 5-6 players, use a bit more of the ellipse
    startAngle = -Math.PI * 0.92;
    endAngle = Math.PI * 0.92;
  } else {
    // For 7+ players, use the full ellipse
    startAngle = 0;
    endAngle = 2 * Math.PI;
  }

  // Distribute players evenly within the angle range
  const angleStep = (endAngle - startAngle) / numPlayers;

  for (let i = 0; i < numPlayers; i++) {
    // Calculate angle for this player
    let angle;

    if (numPlayers <= 2) {
      // For 1-2 players, place them directly at the specified positions
      angle = (i === 0) ? startAngle : endAngle;
    } else {
      // For 3+ players, distribute evenly
      angle = startAngle + angleStep * i;
    }

    // Convert polar coordinates to Cartesian
    const x = centerX + a * Math.cos(angle);
    const y = centerY + b * Math.sin(angle);

    // Add position to array
    positions.push({
      x,
      y,
      // Include the angle for rotation calculations if needed
      angle,
      // Add distance from center for scaling calculations
      distanceFromCenter: Math.sqrt(
        Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
      )
    });
  }

  return positions;
};

/**
 * Calculate scaling factor based on fullscreen state and position
 * This ensures players further from the center are properly sized
 */
export const getPlayerScaling = (position, containerWidth, isFullscreen) => {
  // Base scale adjusted by container width
  const baseScale = containerWidth / 1300; // Reduced base scale

  // Adjust for fullscreen
  const fullscreenMultiplier = isFullscreen ? 1.2 : 0.9; // Reduced from 1 to 0.9

  return {
    scale: Math.min(Math.max(baseScale * fullscreenMultiplier, 0.5), 1.4) // Lower min/max bounds
  };
};