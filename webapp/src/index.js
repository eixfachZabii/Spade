// webapp/src/index.js
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";

// Import the split CSS files
import "./styles/base.css";
import "./styles/layout.css";
import "./styles/components.css";
import "./styles/debug.css"
import "./styles/pages.css";

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);