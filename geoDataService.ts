/**
 * Geo Data Service - Administrative Hierarchy
 * Covers Cumilla (17 Upazilas) and Brahmanbaria (9 Upazilas)
 * Compatible with District -> Upazila[] -> Mouza[] schema
 */

import { District, Upazila, Mouza } from './types';
import { GEO_DATA } from './geoData';

export class GeoDataService {
  static getDistricts(): District[] {
    return GEO_DATA;
  }

  static getDistrictById(districtId: string): District | undefined {
    return GEO_DATA.find(d => d.id === districtId);
  }

  static getUpazilas(districtId: string): Upazila[] {
    const district = this.getDistrictById(districtId);
    return district ? district.upazilas : [];
  }

  static getUpazilaById(districtId: string, upazilaId: string): Upazila | undefined {
    const upazilas = this.getUpazilas(districtId);
    return upazilas.find(u => u.id === upazilaId);
  }

  static getMouzas(districtId: string, upazilaId: string): Mouza[] {
    const upazila = this.getUpazilaById(districtId, upazilaId);
    return upazila ? upazila.mouzas : [];
  }
}

export { GEO_DATA };
