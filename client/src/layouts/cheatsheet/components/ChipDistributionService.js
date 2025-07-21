import axios from 'axios';
import EnvironmentConfig from '../../../config/environment';

const API_BASE_URL = EnvironmentConfig.getCheatsheetApiUrl();

class ChipDistributionService {
  static async calculateOptimalDistribution(chipInventory) {
    try {
      const response = await axios.post(`${API_BASE_URL}/chips/optimize`, chipInventory);
      return response.data;
    } catch (error) {
      console.error('Error calculating chip distribution:', error);
      throw error;
    }
  }

  static async getPresetDistribution(playerCount) {
    try {
      const response = await axios.get(`${API_BASE_URL}/chips/presets`, {
        params: { players: playerCount }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching preset distributions:', error);
      throw error;
    }
  }

  static async saveCustomDistribution(distribution) {
    try {
      const response = await axios.post(`${API_BASE_URL}/chips/save-preset`, distribution);
      return response.data;
    } catch (error) {
      console.error('Error saving custom distribution:', error);
      throw error;
    }
  }
}

export default ChipDistributionService;