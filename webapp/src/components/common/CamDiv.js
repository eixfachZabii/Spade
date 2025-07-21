import React, { useEffect } from "react";
import Webcam from "react-webcam";

/**
 * Camera component for scanning cards
 * Updated to support privacy mode with hidden camera
 *
 * @param {Object} props Component props
 * @param {boolean} props.cameraEnabled Whether the camera is active
 * @param {React.RefObject} props.webcamRef Reference to webcam element
 * @param {function} props.onCapture Function to handle captured frames
 * @param {boolean} props.privacyMode Whether to hide the camera feed (default: false)
 * @returns {JSX.Element} CamDiv component
 */
function CamDiv({ cameraEnabled, webcamRef, onCapture, privacyMode = false }) {
  useEffect(() => {
    if (cameraEnabled && webcamRef.current) {
      const captureImage = () => {
        const video = webcamRef.current.video;
        if (video && onCapture) {
          onCapture(video);
        }
      };

      // Capture an image in specified intervals
      const interval = setInterval(captureImage, 500);
      return () => clearInterval(interval);
    }
  }, [cameraEnabled, webcamRef, onCapture]);

  return (
      <div className="cam-container">
        {cameraEnabled ? (
            <div className="scanning-status">
              <span>Scanning for cards...</span>
              <div className={`webcam-container ${privacyMode ? 'hidden' : ''}`}>
                <Webcam
                    ref={webcamRef}
                    audio={false}
                    mirrored={false}
                    screenshotFormat="image/jpeg"
                    videoConstraints={{
                      width: 720,
                      height: 480,
                      facingMode: "user",
                    }}
                />
              </div>
            </div>
        ) : (
            <div className="cam-placeholder">
              <span>Enable camera to scan cards</span>
            </div>
        )}
      </div>
  );
}

export default CamDiv;