import type { SalesProductApi } from '#/api/sales/product';
import type { SalesTeamApi } from '#/api/sales/team';

import { Modal } from 'ant-design-vue';
import dayjs from 'dayjs';
import { h } from 'vue';

import './itinerary-modal.css';

export type ItineraryModalDay =
  | SalesProductApi.ItineraryDay
  | SalesTeamApi.OperationItineraryDay;

export type ShowTeamItineraryModalOptions = {
  departureDate?: string;
  fallbackDescription?: string;
  itineraryDays?: ItineraryModalDay[];
  title?: string;
};

function formatDayDate(departureDate?: string, dayNo?: number) {
  if (!departureDate || !dayNo) return '--';
  return dayjs(departureDate).add(dayNo - 1, 'day').format('YYYY/MM/DD');
}

function formatDistanceMeters(value?: number) {
  if (!value || value <= 0) return '--';
  if (value < 1000) return `${value}米`;
  return `${(value / 1000).toFixed(1)}公里`;
}

function formatDurationSeconds(value?: number) {
  if (!value || value <= 0) return '--';
  const hours = Math.floor(value / 3600);
  const minutes = Math.round((value % 3600) / 60);
  if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  return `${minutes}分钟`;
}

function formatMeal(day: ItineraryModalDay) {
  const meals = [
    day.breakfastIncluded ? '早' : '',
    day.lunchIncluded ? '中' : '',
    day.dinnerIncluded ? '晚' : '',
  ].filter(Boolean);
  return meals.length ? meals.join(' / ') : '--';
}

export function showTeamItineraryModal(options: ShowTeamItineraryModalOptions) {
  const itineraryDays = options.itineraryDays || [];
  Modal.info({
    content: itineraryDays.length
      ? h('div', { class: 'itinerary-modal-content' }, itineraryDays.map((day, index) => h('section', { class: 'itinerary-day-card', key: day.id || day.dayNo || index }, [
          h('div', { class: 'itinerary-day-head' }, [
            h('span', { class: 'itinerary-day-badge' }, `D${day.dayNo || '-'}`),
            h('div', { class: 'itinerary-day-title' }, [
              h('strong', day.dayTitle || `第${day.dayNo || '-'}天行程`),
              h('span', formatDayDate(options.departureDate, day.dayNo)),
            ]),
          ]),
          h('div', { class: 'itinerary-day-body' }, day.itineraryContent || '暂无行程内容'),
          h('div', { class: 'itinerary-day-meta' }, [
            h('span', `用餐：${formatMeal(day)}`),
            h('span', `住宿：${day.relatedHotel || day.accommodationNote || '--'}`),
            h('span', `路程：${formatDistanceMeters(day.roadbookTotalDistanceMeters)}`),
            h('span', `车程：${formatDurationSeconds(day.roadbookTotalDurationSeconds)}`),
          ]),
          day.roadbookSummary
            ? h('div', { class: 'itinerary-roadbook' }, `路书：${day.roadbookSummary}`)
            : null,
          day.remark ? h('div', { class: 'itinerary-remark' }, `备注：${day.remark}`) : null,
        ])))
      : h('div', { class: 'itinerary-modal-content' }, [
          h('div', { class: 'itinerary-day-body' }, options.fallbackDescription || '当前产品暂无行程说明。'),
        ]),
    title: options.title || '查看行程',
    width: 920,
  });
}
