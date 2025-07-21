import React, { useRef, useState } from "react";
import CamDiv from "../common/CamDiv";

/**
 * Card scanner component for detecting and displaying player cards
 * Updated to use a privacy placeholder during scanning
 *
 * @param {Object} props Component props
 * @param {boolean} props.socketConnected Socket connection status
 * @param {Object} props.socket Socket.io connection
 * @param {function} props.onCapture Optional function to override default capture behavior
 * @returns {JSX.Element} CardScanner component
 */
const CardScanner = ({ socketConnected, socket, onCapture }) => {
  // State for camera and scanning
  const webcamRef = useRef(null);
  const [cameraEnabled, setCameraEnabled] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [actionStatus, setActionStatus] = useState("");

  // States for card scanning process
  const [cardsScanned, setCardsScanned] = useState(false);
  const [cardsRevealed, setCardsRevealed] = useState(false);
  const [cardsConfirmed, setCardsConfirmed] = useState(false);
  const [cards, setCards] = useState([]);

  // Function to handle capturing frames from the webcam
  const handleCapture = async (frame) => {
    if (!frame || !frame.videoWidth || !frame.videoHeight) {
      console.error("Invalid video frame");
      return;
    }

    // If custom capture function is provided, use it
    if (onCapture) {
      onCapture(frame);
      return;
    }

    setIsLoading(true);

    try {
      // Draw the frame to a canvas
      const canvas = document.createElement("canvas");
      canvas.width = frame.videoWidth;
      canvas.height = frame.videoHeight;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(frame, 0, 0, canvas.width, canvas.height);

      // Convert canvas to blob
      const blob = await new Promise((resolve, reject) => {
        canvas.toBlob(
            (blob) =>
                blob ? resolve(blob) : reject(new Error("Canvas is empty")),
            "image/jpeg",
            0.8
        );
      });

      // Convert blob to array buffer
      const arrayBuffer = await blob.arrayBuffer();

      // Send frame to server for card detection
      const response = await new Promise((resolve) => {
        socket.emit(
            "frame",
            {
              n: 2,
              image: arrayBuffer,
            },
            resolve
        );
      });

      // If cards are found, display them
      if (response?.found) {
        setCardsScanned(true);
        setCardsRevealed(false);
        setCardsConfirmed(false);
        setCards(response.predictions);
        setActionStatus("Cards detected! Click to reveal.");
        setTimeout(() => setActionStatus(""), 3000);
      }
    } catch (error) {
      console.error("Capture error:", error);
      setActionStatus("Failed to scan cards. Try again.");
    } finally {
      setIsLoading(false);
    }
  };

  // Toggle card visibility
  const revealCards = () => {
    if (cardsScanned) {
      setCardsRevealed((prevState) => !prevState);

      if (!cardsRevealed) {
        setActionStatus("Cards revealed!");
      } else {
        setActionStatus("Cards hidden!");
      }
      setTimeout(() => setActionStatus(""), 1500);
    }
  };

  // Confirm the scanned cards
  const handleConfirmCards = () => {
    setCardsConfirmed(true);
    setActionStatus("Cards confirmed!");
    setTimeout(() => setActionStatus(""), 2000);
  };

  // Retry scanning cards
  const handleRetryScan = () => {
    setCardsScanned(false);
    setCardsRevealed(false);
    setCardsConfirmed(false);
    setCards([]);
    setCameraEnabled(true);
    setActionStatus("Restarting card scan.");
    setTimeout(() => setActionStatus(""), 2000);
  };

  // Reset the card scanner
  const resetScan = () => {
    setCardsScanned(false);
    setCardsRevealed(false);
    setCardsConfirmed(false);
    setCards([]);
    setCameraEnabled(false);
  };

  // Render the cards
  const renderCards = () => {
    if (!cards.length) return null;

    const hintText = !cardsRevealed ? "Click to reveal" : "Click to hide";

    // Function to parse card string and extract rank and suit
    const parseCard = (cardStr) => {
      // The last character is the suit, everything before is the rank
      const rank = cardStr.slice(0, -1);
      const suitLetter = cardStr.slice(-1).toUpperCase();

      // Map suit letters to symbols
      let suitSymbol;
      switch (suitLetter) {
        case "S":
          suitSymbol = "♠";
          break;
        case "H":
          suitSymbol = "♥";
          break;
        case "D":
          suitSymbol = "♦";
          break;
        case "C":
          suitSymbol = "♣";
          break;
        default:
          suitSymbol = suitLetter;
      }

      return { rank, suit: suitSymbol };
    };

    return (
        <div
            className={`poker-cards clickable`}
            onClick={revealCards}
            data-action-hint={hintText}
        >
          {cards.map((card, index) => {
            const { rank, suit } = parseCard(card);

            return (
                <div
                    key={index}
                    className={`poker-card ${!cardsRevealed ? "covered" : ""} ${
                        suit === "♥" || suit === "♦" ? "red-card" : "black-card"
                    }`}
                >
                  {!cardsRevealed ? (
                      <div className="card-back">
                        <span className="reveal-hint">Click to reveal</span>
                      </div>
                  ) : (
                      <div className="card-content">
                        <div className="card-rank">{rank}</div>
                        <div className="card-suit">{suit}</div>
                        <span className="hide-hint">Click to hide</span>
                      </div>
                  )}
                </div>
            );
          })}
        </div>
    );
  };

  return (
      <div className="card-scanner">
        {cardsScanned ? (
            <div className="scanned-result">
              <h2>Your Cards</h2>
              {renderCards()}

              {/* Verification controls - only show after revealing */}
              {cardsRevealed && !cardsConfirmed ? (
                  <div className="card-verification">
                    <p className="verification-text">Are these cards correct?</p>
                    <div className="verification-buttons">
                      <button
                          className="confirm-button"
                          onClick={handleConfirmCards}
                      >
                        Yes, Correct
                      </button>
                      <button className="retry-button" onClick={handleRetryScan}>
                        No, Retry Scan
                      </button>
                    </div>
                    <p className="toggle-hint">
                      You can click the cards to hide them again
                    </p>
                  </div>
              ) : cardsConfirmed ? (
                  <>
                    <p className="toggle-hint">Click cards to show or hide them</p>
                    <button className="reset-button" onClick={resetScan}>
                      Scan New Cards
                    </button>
                  </>
              ) : null}
            </div>
        ) : (
            <div className="scanner-container">
              {socketConnected ? (
                  <div className="scanner-overlay">
                    <div className={`scan-area ${cameraEnabled ? "active" : ""}`}>
                      {/* We've modified this section to hide the actual webcam feed and show a placeholder instead */}
                      {cameraEnabled ? (
                          <div className="privacy-placeholder">
                            <p>Scanning cards</p>
                            <div className="scanning-animation"></div>
                            {/* Hidden webcam for processing - not visible to user */}
                            <div className="hidden-webcam">
                              <CamDiv
                                  cameraEnabled={cameraEnabled}
                                  webcamRef={webcamRef}
                                  onCapture={handleCapture}
                              />
                            </div>
                          </div>
                      ) : (
                          <CamDiv
                              cameraEnabled={false}
                              webcamRef={webcamRef}
                              onCapture={handleCapture}
                          />
                      )}
                    </div>
                    <button
                        className={`scan-button ${cameraEnabled ? "active" : ""}`}
                        onClick={() => setCameraEnabled(!cameraEnabled)}
                        disabled={isLoading}
                    >
                      {cameraEnabled ? "Stop Scanning" : "Scan Cards"}
                    </button>
                  </div>
              ) : (
                  <div className="connection-error">
                    Server connection failed. <br /> Please restart server or debug.
                  </div>
              )}
            </div>
        )}

        {actionStatus && (
            <div className="action-status">
              {isLoading && <div className="loading-spinner"></div>}
              <p>{actionStatus}</p>
            </div>
        )}
      </div>
  );
};

export default CardScanner;