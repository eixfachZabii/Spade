import axios from "axios";


const PROTOCOL="http";
const HOST_URL="127.0.0.1";
const HOST_PORT="5000";
const URI=`${PROTOCOL}://${HOST_URL}:${HOST_PORT}`

// Function to fetch players data
export const fetchPlayersData = async () => {
  try {
    const response = await axios.get(`${URI}/api/players`);
    return response.data;
  } catch (error) {
    console.error('Error fetching players data:', error);
    throw error; // Propagate the error to handle it in the calling component
  }
};

// Function to fetch community cards data
export const fetchCommunityCardsData = async () => {
  try {
    const response = await axios.get(`${URI}/api/community-cards`);
    return response.data;
  } catch (error) {
    console.error('Error fetching community cards data:', error);
    throw error; // Propagate the error to handle it in the calling component
  }
};

// Function to fetch dealer index
export const fetchDealerIndex = async () => {
  try {
    const response = await axios.get(`${URI}/api/dealer`);
    return response.data.dealerIndex;
  } catch (error) {
    console.error('Error fetching dealer index:', error);
    throw error;
  }
};

// Function to fetch the current pot
export const fetchPot = async () => {
  try {
    const response = await axios.get(`${URI}/api/pot`);
    return response.data.pot;
  } catch (error) {
    console.error('Error fetching pot:', error);
    throw error;
  }
};


