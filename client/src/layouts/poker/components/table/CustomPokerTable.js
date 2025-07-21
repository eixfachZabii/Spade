import React from "react";
import "./CustomPokerTable.css";

/**
 * A custom poker table background created entirely with CSS/SVG
 * Enhanced with multiple spade patterns and logos
 */
const CustomPokerTable = () => {
  return (
    <div className="custom-table-container">
      {/* Outer table edge */}
      <div className="table-edge">
        {/* Inner padding/cushion with spade pattern */}
        <div className="table-padding">
          {/* Table felt surface with spade pattern */}
          <div className="table-felt">
            {/* Felt spade pattern */}
            <div className="felt-pattern"></div>

            {/* Scattered small spades */}
            <div className="scattered-spades">
              {Array.from({ length: 30 }).map((_, index) => (
                <div key={index} className={`small-spade spade-${index + 1}`}></div>
              ))}
            </div>

            {/* Center emblem/logo */}
            <div className="table-emblem">
              <svg
                className="emblem-svg"
                viewBox="0 0 200 200"
                xmlns="http://www.w3.org/2000/svg"
              >


                {/* Pattern of small spades in a circle */}
                <g className="small-spades" opacity="0.15">
                  {Array.from({ length: 12 }).map((_, index) => {
                    const angle = (index * Math.PI * 2) / 12;
                    const radius = 70;
                    const x = 100 + radius * Math.cos(angle);
                    const y = 100 + radius * Math.sin(angle);

                    return (
                      <path
                        key={index}
                        d="M0,0 C-3,5 -8,8 -8,15 C-8,20 -5,23 -1,23 C1,23 3,22 5,19 C5,25 2,28 2,30 L12,30 C12,28 9,25 9,19 C11,22 13,23 15,23 C19,23 22,20 22,15 C22,8 17,5 14,0 Z"
                        fill="rgba(0,0,0,0.4)"
                        transform={`translate(${x}, ${y}) rotate(${angle * (180/Math.PI) + 90}) scale(0.7)`}
                      />
                    );
                  })}
                </g>

                {/* Decorative circles */}
                <circle
                  cx="100"
                  cy="100"
                  r="45"
                  fill="none"
                  stroke="rgba(255,255,255,0.15)"
                  strokeWidth="2"
                />

                <circle
                  cx="100"
                  cy="100"
                  r="65"
                  fill="none"
                  stroke="rgba(255,255,255,0.1)"
                  strokeWidth="1"
                  strokeDasharray="3,3"
                />

                {/* "SPADE" text emblem */}
                <text
                  x="100"
                  y="95"
                  textAnchor="middle"
                  fontFamily="Arial, sans-serif"
                  fontWeight="bold"
                  fontSize="14"
                  fill="rgba(255,255,255,0.15)"
                >
                  SPADE
                </text>

                {/* Spade symbol above text */}
                <path
                  d="M100,65 C98,70 94,72 94,76 C94,78 96,80 98,80 C99,80 100,79.5 100,78.5 C100,79.5 101,80 102,80 C104,80 106,78 106,76 C106,72 102,70 100,65 Z"
                  fill="rgba(255,255,255,0.15)"
                />
              </svg>
            </div>

            {/* Felt texture overlay */}
            <div className="felt-texture"></div>

            {/* Light reflection effects */}
            <div className="light-reflection top-left"></div>
            <div className="light-reflection bottom-right"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CustomPokerTable;