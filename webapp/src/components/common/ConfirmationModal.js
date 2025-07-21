import React from "react";

/**
 * Reusable confirmation modal component
 *
 * @param {Object} props Component props
 * @param {boolean} props.show Whether the modal is visible
 * @param {string} props.title Modal title text
 * @param {string} props.message Confirmation message
 * @param {string} props.confirmText Text for confirm button
 * @param {string} props.cancelText Text for cancel button
 * @param {string} props.confirmButtonClass CSS class for confirm button
 * @param {function} props.onConfirm Function to execute on confirmation
 * @param {function} props.onCancel Function to execute on cancel
 * @returns {JSX.Element} ConfirmationModal component
 */
function ConfirmationModal({
  show,
  title,
  message,
  confirmText = "Confirm",
  cancelText = "Cancel",
  confirmButtonClass = "danger",
  onConfirm,
  onCancel,
}) {
  if (!show) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content confirmation-modal">
        <h3>{title}</h3>
        <div className="confirmation-message">
          <p>{message}</p>
        </div>
        <div className="form-buttons">
          <button
            className={`primary-button ${confirmButtonClass}`}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
          <button
            className="secondary-button"
            onClick={onCancel}
          >
            {cancelText}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmationModal;