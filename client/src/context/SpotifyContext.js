// client/src/context/SpotifyContext.js
import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import SpotifyApiService from '../layouts/spotify/SpotifyApiService';

// Create context
const SpotifyContext = createContext();

// Hook to use the Spotify context
export const useSpotify = () => useContext(SpotifyContext);

export const SpotifyProvider = ({ children }) => {
  // State for authentication
  const [token, setToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);
  const [expireTime, setExpireTime] = useState(0);
  const [isReady, setIsReady] = useState(false);

  // State for player
  const [currentTrack, setCurrentTrack] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [trackProgress, setTrackProgress] = useState(0);
  const [trackDuration, setTrackDuration] = useState(0);
  const [volume, setVolume] = useState(50);
  const [isMuted, setIsMuted] = useState(false);
  const [isShuffle, setIsShuffle] = useState(false);
  const [isControlBusy, setIsControlBusy] = useState(false);
  const [isPlayerHealthy, setIsPlayerHealthy] = useState(true);

  // Lyrics state
  const [lyrics, setLyrics] = useState('');
  const [loadingLyrics, setLoadingLyrics] = useState(false);

  // References
  const playerRef = useRef(null);
  const playerCheckIntervalRef = useRef(null);
  const progressIntervalRef = useRef(null);
  const lastTrackIdRef = useRef(null);
  const isPlayingRef = useRef(isPlaying);
  const trackDurationRef = useRef(trackDuration);


  // Update refs when state changes
  useEffect(() => {
    isPlayingRef.current = isPlaying;
  }, [isPlaying]);

  useEffect(() => {
    trackDurationRef.current = trackDuration;
  }, [trackDuration]);

  // Extract token from hash on mount
  useEffect(() => {
    // Parse hash from URL (from Spotify callback)
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const refreshTokenValue = params.get('refresh_token');
    const expiresAt = params.get('expires_at');

    if (accessToken) {
      setToken(accessToken);

      if (refreshTokenValue) {
        setRefreshToken(refreshTokenValue);
      }

      if (expiresAt) {
        setExpireTime(Number(expiresAt));
      }

      // Clean up the URL
      window.history.replaceState(null, null, window.location.pathname);
    } else {
      // Try to get token from localStorage
      const savedToken = localStorage.getItem('spotify_token');
      const savedRefreshToken = localStorage.getItem('spotify_refresh_token');
      const savedExpireTime = localStorage.getItem('spotify_expire_time');

      if (savedToken) {
        setToken(savedToken);

        if (savedRefreshToken) {
          setRefreshToken(savedRefreshToken);
        }

        if (savedExpireTime) {
          setExpireTime(Number(savedExpireTime));
        }
      }
    }
  }, []);

  // Save token to localStorage when it changes
  useEffect(() => {
    if (token) {
      localStorage.setItem('spotify_token', token);

      if (refreshToken) {
        localStorage.setItem('spotify_refresh_token', refreshToken);
      }

      if (expireTime) {
        localStorage.setItem('spotify_expire_time', expireTime);
      }

      setIsReady(true);
    } else {
      localStorage.removeItem('spotify_token');
      localStorage.removeItem('spotify_refresh_token');
      localStorage.removeItem('spotify_expire_time');

      setIsReady(false);
    }
  }, [token, refreshToken, expireTime]);

  // Initialize the Spotify Web Player
  useEffect(() => {
    if (!token) return;

    // Load the Spotify Web Player SDK
    const script = document.createElement('script');
    script.src = 'https://sdk.scdn.co/spotify-player.js';
    script.async = true;
    document.body.appendChild(script);

    // Initialize the player when the SDK is loaded
    window.onSpotifyWebPlaybackSDKReady = () => {
      const player = new window.Spotify.Player({
        name: 'SpadeBoot Web Player',
        getOAuthToken: cb => cb(token),
        volume: volume / 100
      });

      // Error handling
      player.addListener('initialization_error', ({ message }) => {
        console.error('Initialization error:', message);
        setIsPlayerHealthy(false);
      });

      player.addListener('authentication_error', ({ message }) => {
        console.error('Authentication error:', message);
        setIsPlayerHealthy(false);
        // Try to refresh the token
        refreshTokenHandler();
      });

      player.addListener('account_error', ({ message }) => {
        console.error('Account error:', message);
        setIsPlayerHealthy(false);
      });

      player.addListener('playback_error', ({ message }) => {
        console.error('Playback error:', message);
        setIsPlayerHealthy(false);
      });

      // Ready
      player.addListener('ready', ({ device_id }) => {
        console.log('Spotify Web Player ready with device ID:', device_id);
        setIsPlayerHealthy(true);
        // Transfer playback to this device
        transferPlayback(device_id);
      });

      // Not ready
      player.addListener('not_ready', ({ device_id }) => {
        console.warn('Device ID went offline:', device_id);
        setIsPlayerHealthy(false);
      });

      // State changes
      player.addListener('player_state_changed', state => {
        if (!state) return;

        // Update track information
        const currentTrackData = state.track_window.current_track;

        // Only fetch lyrics if the track ID has changed
        if (!lastTrackIdRef.current || currentTrackData.id !== lastTrackIdRef.current) {
          lastTrackIdRef.current = currentTrackData.id;
          fetchLyrics(currentTrackData);
        }

        setCurrentTrack(currentTrackData);
        setTrackDuration(state.duration);

        // Update playback state
        setIsPlaying(!state.paused);
        setIsShuffle(state.shuffle);

        // Update progress
        setTrackProgress(state.position);
      });

      // Connect to the player
      player.connect();
      playerRef.current = player;

      // Set up a progress interval to update track progress
      progressIntervalRef.current = setInterval(() => {
        if (isPlayingRef.current) { // Use ref value
          setTrackProgress(prev => {
            const newProgress = prev + 1000; // Correct increment to 2000ms
            return newProgress > trackDurationRef.current
              ? trackDurationRef.current
              : newProgress;
          });
        }
      }, 1000);

      // Set up a health check interval
      playerCheckIntervalRef.current = setInterval(() => {
        player.getCurrentState().then(state => {
          if (!state) {
            setIsPlayerHealthy(false);
          } else {
            setIsPlayerHealthy(true);
          }
        });
      }, 5000);

      return () => {
        player.disconnect();
        clearInterval(progressIntervalRef.current);
        clearInterval(playerCheckIntervalRef.current);
      };
    };

    return () => {
      if (playerRef.current) {
        playerRef.current.disconnect();
      }
      if (progressIntervalRef.current) {
        clearInterval(progressIntervalRef.current);
      }
      if (playerCheckIntervalRef.current) {
        clearInterval(playerCheckIntervalRef.current);
      }
      document.body.removeChild(script);
    };
  }, [token]);

  // Auto-refresh token before it expires
  useEffect(() => {
    if (!refreshToken || !expireTime) return;

    const timeUntilExpire = expireTime - Math.floor(Date.now() / 1000);

    // Refresh 5 minutes before expiration
    const refreshTime = Math.max(0, timeUntilExpire - 300) * 1000;

    const refreshTimeout = setTimeout(() => {
      refreshTokenHandler();
    }, refreshTime);

    return () => clearTimeout(refreshTimeout);
  }, [refreshToken, expireTime]);

  // Function to refresh the token
  const refreshTokenHandler = useCallback(async () => {
    if (!refreshToken) return;

    try {
      const response = await SpotifyApiService.refreshToken(refreshToken);

      setToken(response.access_token);

      if (response.refresh_token) {
        setRefreshToken(response.refresh_token);
      }

      if (response.expires_at) {
        setExpireTime(response.expires_at);
      } else if (response.expires_in) {
        const newExpireTime = Math.floor(Date.now() / 1000) + response.expires_in;
        setExpireTime(newExpireTime);
      }

      return response.access_token;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      // Clear token if refresh fails
      setToken(null);
      setRefreshToken(null);
      setExpireTime(0);
      return null;
    }
  }, [refreshToken]);

  // Function to transfer playback to this device
  const transferPlayback = useCallback(async (deviceId) => {
    if (!token || !deviceId) return;

    try {
      await fetch('https://api.spotify.com/v1/me/player', {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          device_ids: [deviceId],
          play: false
        })
      });
    } catch (error) {
      console.error('Error transferring playback:', error);
    }
  }, [token]);

  // Function to fetch lyrics
  const fetchLyrics = useCallback(async (track) => {
    if (!track) return;

    setLoadingLyrics(true);

    try {
      const artist = track.artists[0].name;
      const title = track.name;

      // Remove everything after "[" in both artist and title
      const cleanArtist = artist.split("[")[0].trim();
      const cleanTitle = title.split("[")[0].trim();
      const reallyCleanArtist = cleanArtist.split("(")[0].trim();
      const reallyCleanTitle = cleanTitle.split("(")[0].trim();

      // Format title (in case you still want to remove any " - " split)
      const formattedTitle = reallyCleanTitle.includes(" - ") ? reallyCleanTitle.split(" - ")[0] : reallyCleanTitle;

      const lyricsData = await SpotifyApiService.getLyrics(reallyCleanArtist, formattedTitle);

      if (lyricsData.error) {
        console.error('Lyrics error:', lyricsData.error);
        setLyrics('');
      } else {
        setLyrics(lyricsData.lyrics || '');
      }
    } catch (error) {
      console.error('Error fetching lyrics:', error);
      setLyrics('');
    } finally {
      setLoadingLyrics(false);
    }
  }, []);

  // Player control functions
  const togglePlay = useCallback(async () => {
    if (!playerRef.current || !isPlayerHealthy) return;

    setIsControlBusy(true);

    try {
      await playerRef.current.togglePlay();
    } catch (error) {
      console.error('Error toggling playback:', error);
    } finally {
      setIsControlBusy(false);
    }
  }, [isPlayerHealthy]);

  const skipToNext = useCallback(async () => {
    if (!playerRef.current || !isPlayerHealthy) return;

    setIsControlBusy(true);

    try {
      await playerRef.current.nextTrack();
    } catch (error) {
      console.error('Error skipping to next track:', error);
    } finally {
      setIsControlBusy(false);
    }
  }, [isPlayerHealthy]);

  const skipToPrevious = useCallback(async () => {
    if (!playerRef.current || !isPlayerHealthy) return;

    setIsControlBusy(true);

    try {
      await playerRef.current.previousTrack();
    } catch (error) {
      console.error('Error skipping to previous track:', error);
    } finally {
      setIsControlBusy(false);
    }
  }, [isPlayerHealthy]);

  const seek = useCallback(async (position) => {
    if (!playerRef.current || !isPlayerHealthy) return;

    setIsControlBusy(true);
    setTrackProgress(position);

    try {
      await playerRef.current.seek(position);
    } catch (error) {
      console.error('Error seeking:', error);
    } finally {
      setIsControlBusy(false);
    }
  }, [isPlayerHealthy]);

  const handleSetVolume = useCallback(async (value) => {
    if (!playerRef.current || !isPlayerHealthy) return;

    const volumeValue = isMuted ? 0 : value;
    setVolume(value);

    try {
      await playerRef.current.setVolume(volumeValue / 100);
    } catch (error) {
      console.error('Error setting volume:', error);
    }
  }, [isMuted, isPlayerHealthy]);

  const toggleMute = useCallback(async () => {
    if (!playerRef.current || !isPlayerHealthy) return;

    const newMutedState = !isMuted;
    setIsMuted(newMutedState);

    try {
      await playerRef.current.setVolume(newMutedState ? 0 : volume / 100);
    } catch (error) {
      console.error('Error toggling mute:', error);
    }
  }, [isMuted, volume, isPlayerHealthy]);

  const setShuffle = useCallback(async () => {
    if (!token || !isPlayerHealthy) return;

    setIsControlBusy(true);
    const newShuffleState = !isShuffle;

    try {
      await fetch(`https://api.spotify.com/v1/me/player/shuffle?state=${newShuffleState}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      setIsShuffle(newShuffleState);
    } catch (error) {
      console.error('Error setting shuffle:', error);
    } finally {
      setIsControlBusy(false);
    }
  }, [token, isShuffle, isPlayerHealthy]);

  // Format duration helper
  const formatDuration = useCallback((ms) => {
    if (!ms) return '0:00';

    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }, []);

  // Provide context value
  const contextValue = {
    // Authentication & state
    token,
    refreshToken,
    expireTime,
    isReady,

    // Player state
    currentTrack,
    isPlaying,
    trackProgress,
    trackDuration,
    volume,
    isMuted,
    isShuffle,
    isControlBusy,
    isPlayerHealthy,

    // Lyrics
    lyrics,
    loadingLyrics,

    // Player controls
    togglePlay,
    skipToNext,
    skipToPrevious,seek,
    setVolume: handleSetVolume,
    toggleMute,
    setShuffle,

    // Helper functions
    formatDuration,
  };

  return (
    <SpotifyContext.Provider value={contextValue}>
      {children}
    </SpotifyContext.Provider>
  );
};