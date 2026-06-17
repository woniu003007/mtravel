import { areaList } from '@vant/area-data';

export type RegionPath = string[];

export interface SavedRegionFields {
  city?: string;
  district?: string;
  province?: string;
}

export interface RegionOption {
  children?: RegionOption[];
  label: string;
  value: string;
}

function cleanRegionName(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

/**
 * 将数据库保存的省、市、区县字段还原为级联选择器需要的路径。
 */
export function buildRegionPath(
  province?: string,
  city?: string,
  district?: string,
): RegionPath {
  return [province, city, district]
    .map((item) => cleanRegionName(item))
    .filter((item): item is string => Boolean(item));
}

/**
 * 将级联选择器路径拆回后端接口仍然使用的 province/city/district 三个字段。
 */
export function splitRegionPath(path?: RegionPath): SavedRegionFields {
  const [province, city, district] = path || [];
  return {
    city: cleanRegionName(city),
    district: cleanRegionName(district),
    province: cleanRegionName(province),
  };
}

/**
 * 基于 @vant/area-data 构造省市区三级选择数据，供后台所在地字段复用。
 */
export function buildRegionOptions(): RegionOption[] {
  const { city_list: cities, county_list: counties, province_list: provinces } =
    areaList;

  return Object.entries(provinces).map(([provinceCode, provinceName]) => {
    const provincePrefix = provinceCode.slice(0, 2);
    const cityOptions = Object.entries(cities)
      .filter(([cityCode]) => cityCode.startsWith(provincePrefix))
      .map(([cityCode, cityName]) => {
        const cityPrefix = cityCode.slice(0, 4);
        const countyOptions = Object.entries(counties)
          .filter(([countyCode]) => countyCode.startsWith(cityPrefix))
          .map(([, countyName]) => ({
            label: countyName,
            value: countyName,
          }));

        return {
          children: countyOptions,
          label: cityName,
          value: cityName,
        };
      });

    return {
      children: cityOptions,
      label: provinceName,
      value: provinceName,
    };
  });
}
