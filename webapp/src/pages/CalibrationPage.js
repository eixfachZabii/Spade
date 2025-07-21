import React, { useState, useEffect, useRef } from "react";

/**
 * Calibration page for camera setup and card recognition system
 *
 * @param {Object} props Component props
 * @param {Object} props.socket Socket.io connection
 * @param {boolean} props.socketConnected Socket connection status
 * @param {number} props.tableId Current table ID
 * @returns {JSX.Element} CalibrationPage component
 */
function CalibrationPage({ socket, socketConnected, tableId }) {
  const [calibrationImage, setCalibrationImage] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [calibrationStatus, setCalibrationStatus] = useState("idle"); // idle, loading, success, error
  const frameIntervalRef = useRef(null);

  // Function to request current frame from backend
  const getFrame = async () => {
    if (!socketConnected) {
      setErrorMessage("Socket not connected");
      return;
    }

    setIsLoading(true);

    try {
      const response = await new Promise((resolve) => {
        socket.emit("getFrame", { tableId }, resolve);
      });

      if (response && response.image) {
        // Convert array buffer to base64 image
        const base64Image = arrayBufferToBase64(response.image);
        setCalibrationImage(`data:image/jpeg;base64,${base64Image}`);
        setErrorMessage(""); // Clear any previous errors
      } else {
        setErrorMessage("Invalid frame received");
      }
    } catch (error) {
      console.error("Error getting frame:", error);
      setErrorMessage("Failed to get frame from camera");
    } finally {
      setIsLoading(false);
    }
  };

  // Function to request calibration
  const recalibrate = async () => {
    if (!socketConnected) {
      setErrorMessage("Socket not connected");
      return;
    }

    // Stop fetching frames during calibration
    if (frameIntervalRef.current) {
      clearInterval(frameIntervalRef.current);
      frameIntervalRef.current = null;
    }

    setCalibrationStatus("loading");

    try {
      const response = await new Promise((resolve) => {
        socket.emit("recalibrate", { tableId }, resolve);
      });

      if (response && response.success) {
        setCalibrationStatus("success");

        // Reset status after 2 seconds
        setTimeout(() => {
          setCalibrationStatus("idle");
        }, 2000);
      } else {
        setErrorMessage(response?.message || "Calibration failed");
        setCalibrationStatus("error");

        // Reset status after 2 seconds
        setTimeout(() => {
          setCalibrationStatus("idle");
        }, 2000);
      }
    } catch (error) {
      console.error("Error during calibration:", error);
      setErrorMessage("Failed to calibrate");
      setCalibrationStatus("error");

      // Reset status after 2 seconds
      setTimeout(() => {
        setCalibrationStatus("idle");
      }, 2000);
    } finally {
      // Resume frame fetching after calibration
      startFrameFetching();
    }
  };

  // Helper function to convert array buffer to base64
  const arrayBufferToBase64 = (buffer) => {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  };

  // Function to start continuous frame fetching
  const startFrameFetching = () => {
    // Clear any existing interval
    if (frameIntervalRef.current) {
      clearInterval(frameIntervalRef.current);
    }

    // Set up new interval for fetching frames
    frameIntervalRef.current = setInterval(() => {
      getFrame();
    }, 1000); // Fetch frame every second
  };

  // Start fetching frames when component mounts or socket connects
  useEffect(() => {
    if (socketConnected) {
      startFrameFetching();
    } else {
      // Clear interval if socket disconnects
      if (frameIntervalRef.current) {
        clearInterval(frameIntervalRef.current);
        frameIntervalRef.current = null;
      }
      setErrorMessage("Socket not connected");
    }

    // Clean up on unmount
    return () => {
      if (frameIntervalRef.current) {
        clearInterval(frameIntervalRef.current);
      }
    };
  }, [socketConnected, tableId]);

  return (
    <div className="calibration-container">
      <div className="calibration-card">
        <h2>Camera Calibration</h2>

        <div className="calibration-frame">
          {isLoading && (
            <div className="loading-spinner-overlay">
              <div className="loading-spinner"></div>
            </div>
          )}

          {calibrationImage ? (
            <img
              src={calibrationImage}
              alt="Camera calibration frame"
              className="calibration-image"
            />
          ) : (
            <div className="no-frame-placeholder">
              <p>No camera frame available</p>
              {errorMessage ? (
                <p className="frame-error-message">{errorMessage}</p>
              ) : (
                <p>Waiting for camera feed...</p>
              )}
            </div>
          )}

          {errorMessage && calibrationImage && (
            <div className="frame-error-overlay">
              <p>{errorMessage}</p>
            </div>
          )}
        </div>

        <div className="calibration-controls">
          <button
            className={`calibrate-button ${calibrationStatus}`}
            onClick={recalibrate}
            disabled={calibrationStatus !== "idle"}
          >
            {calibrationStatus === "loading" && (
              <span className="button-spinner"></span>
            )}
            {calibrationStatus === "success" ? "Calibrated!" : "Recalibrate"}
          </button>
        </div>

        <div className="calibration-info">
          <h3>Calibration Instructions</h3>
          <ol>
            <li>Ensure the poker table is well lit and the camera is properly positioned.</li>
            <li>The camera feed is displayed automatically above.</li>
            <li>Make sure the entire playing area is visible in the frame.</li>
            <li>Press "Recalibrate" to update the card recognition system.</li>
            <li>Wait for confirmation that calibration is complete.</li>
          </ol>
          <div className="owner-note">
            <p>Note: This calibration page is only accessible to table owners and helps optimize the card recognition system for all players at this table.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CalibrationPage;