import React from "react";
import "./ChipStack.css";

/**
 * Chip stack visualization component
 * Used for pot and player bets
 */
const ChipStack = ({ amount, compact = false, isPot = false }) => {
  // Skip rendering if amount is invalid
  if (!amount || isNaN(amount) || amount <= 0) {
    return null;
  }

  // Define chip denominations and their visual properties
  const CHIP_TYPES = [
    { value: 500, color: { bg: "#151F30", border: "#5C92FF" } },
    { value: 100, color: { bg: "#212121", border: "#888888" } },
    { value: 50, color: { bg: "#1B5E20", border: "#66BB6A" } },
    { value: 25, color: { bg: "#0D47A1", border: "#64B5F6" } },
    { value: 10, color: { bg: "#B71C1C", border: "#EF5350" } },
    { value: 5, color: { bg: "#E0E0E0", border: "#9E9E9E" } },
    { value: 1, color: { bg: "#E0E0E0", border: "#9E9E9E" } }
  ];

  // For player bets (single stack display)
  if (!isPot) {
    // Determine number of chips to show based on amount
    // Limit chips for visual clarity
    const getChipCount = () => {
      if (amount <= 10) return 1;
      if (amount <= 50) return 2;
      if (amount <= 200) return 3;
      if (amount <= 500) return 4;
      return 5;
    };

    // Get chip color and value based on amount
    const getChipInfo = () => {
      // Find the highest denomination chip that fits the amount
      for (const chip of CHIP_TYPES) {
        if (amount >= chip.value) {
          return {
            color: chip.color,
            value: chip.value
          };
        }
      }
      return {
        color: CHIP_TYPES[CHIP_TYPES.length - 1].color,
        value: 1
      };
    };

    const chipCount = getChipCount();
    const chipInfo = getChipInfo();

    return (
      <div className={`chip-stack-container ${compact ? 'compact' : ''}`}>
        <div className="chip-stack">
          {Array.from({ length: chipCount }).map((_, index) => (
            <div
              key={index}
              className="chip"
              style={{
                backgroundColor: chipInfo.color.bg,
                borderColor: chipInfo.color.border,
                transform: `translateY(${-index * (compact ? 5 : 8)}px)`,
                width: compact ? '30px' : '40px',
                height: compact ? '30px' : '40px'
              }}
            >
              <span className="chip-value">{chipInfo.value}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // For pot display (multiple stacks of different denominations)
  const calculateChipBreakdown = () => {
    let remainingAmount = amount;
    const breakdown = [];

    // Go through each chip type from highest to lowest
    for (const chipType of CHIP_TYPES) {
      if (remainingAmount >= chipType.value) {
        // Calculate how many of this chip we need
        const count = Math.floor(remainingAmount / chipType.value);
        // Limit to 5 chips per denomination for visual clarity
        const displayCount = Math.min(count, 5);

        if (displayCount > 0) {
          breakdown.push({
            value: chipType.value,
            count: displayCount,
            color: chipType.color
          });
        }

        remainingAmount -= chipType.value * count;
      }

      // Stop if we've reached a reasonable number of stacks
      if (breakdown.length >= 4) break;
    }

    return breakdown;
  };

  const chipBreakdown = calculateChipBreakdown();

  return (
    <div className="pot-chip-container">
      <div className="pot-chips">
        {chipBreakdown.map((stack, stackIndex) => (
          <div
            key={stackIndex}
            className="pot-chip-stack"
            style={{
              marginLeft: stackIndex > 0 ? '15px' : '0'
            }}
          >
            {Array.from({ length: stack.count }).map((_, chipIndex) => (
              <div
                key={chipIndex}
                className="chip"
                style={{
                  backgroundColor: stack.color.bg,
                  borderColor: stack.color.border,
                  transform: `translateY(${-chipIndex * 8}px)`,
                  width: '40px',
                  height: '40px'
                }}
              >
                <span className="chip-value">{stack.value}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default ChipStack;