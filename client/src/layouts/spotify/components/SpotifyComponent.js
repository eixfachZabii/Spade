import React from "react";
import SpotifyLogin from "./SpotifyLogin";
import SpotifyPlayer from "./SpotifyPlayer";
import { useSpotify } from "../../../context/SpotifyContext";

const SpotifyComponent = ({ useLyrics = true }) => {
  // Get token from context instead of managing local state
  const { token } = useSpotify();

  return (
    <div>
      {token ? (
        <SpotifyPlayer useLyrics={useLyrics} />
      ) : (
        <SpotifyLogin />
      )}
    </div>
  );
};

export default SpotifyComponent;