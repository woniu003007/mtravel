<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const weekDates = ['6/17', '6/18', '6/19', '6/20', '6/21', '6/22', '6/23'];

interface ScheduleItem {
  day: number;
  teamNo: string;
  product: string;
  color: string;
}

interface GuideSchedule {
  id: number;
  name: string;
  phone: string;
  schedule: ScheduleItem[];
}

const guides = ref<GuideSchedule[]>([
  {
    id: 1,
    name: '陈导',
    phone: '138****1234',
    schedule: [
      { day: 0, teamNo: 'TM0101', product: '桂林三日游', color: 'bg-blue-100 text-blue-700' },
      { day: 1, teamNo: 'TM0101', product: '桂林三日游', color: 'bg-blue-100 text-blue-700' },
      { day: 2, teamNo: 'TM0101', product: '桂林三日游', color: 'bg-blue-100 text-blue-700' },
      { day: 5, teamNo: 'TM0106', product: '市区文化游', color: 'bg-purple-100 text-purple-700' },
      { day: 6, teamNo: 'TM0106', product: '市区文化游', color: 'bg-purple-100 text-purple-700' },
    ],
  },
  {
    id: 2,
    name: '刘导',
    phone: '139****5678',
    schedule: [
      { day: 1, teamNo: 'TM0102', product: '阳朔四日游', color: 'bg-green-100 text-green-700' },
      { day: 2, teamNo: 'TM0102', product: '阳朔四日游', color: 'bg-green-100 text-green-700' },
      { day: 3, teamNo: 'TM0102', product: '阳朔四日游', color: 'bg-green-100 text-green-700' },
      { day: 4, teamNo: 'TM0102', product: '阳朔四日游', color: 'bg-green-100 text-green-700' },
    ],
  },
  {
    id: 3,
    name: '赵导',
    phone: '137****9012',
    schedule: [
      { day: 0, teamNo: 'TM0103', product: '龙脊两日游', color: 'bg-orange-100 text-orange-700' },
      { day: 1, teamNo: 'TM0103', product: '龙脊两日游', color: 'bg-orange-100 text-orange-700' },
      { day: 4, teamNo: 'TM0105', product: '竹筏一日游', color: 'bg-teal-100 text-teal-700' },
    ],
  },
  {
    id: 4,
    name: '周导',
    phone: '136****3456',
    schedule: [
      { day: 3, teamNo: 'TM0107', product: '银子岩三日', color: 'bg-pink-100 text-pink-700' },
      { day: 4, teamNo: 'TM0107', product: '银子岩三日', color: 'bg-pink-100 text-pink-700' },
      { day: 5, teamNo: 'TM0107', product: '银子岩三日', color: 'bg-pink-100 text-pink-700' },
    ],
  },
  {
    id: 5,
    name: '吴导',
    phone: '135****7890',
    schedule: [
      { day: 2, teamNo: 'TM0108', product: '亲子研学游', color: 'bg-indigo-100 text-indigo-700' },
      { day: 3, teamNo: 'TM0108', product: '亲子研学游', color: 'bg-indigo-100 text-indigo-700' },
      { day: 4, teamNo: 'TM0108', product: '亲子研学游', color: 'bg-indigo-100 text-indigo-700' },
      { day: 5, teamNo: 'TM0108', product: '亲子研学游', color: 'bg-indigo-100 text-indigo-700' },
    ],
  },
]);

function getScheduleForDay(guide: GuideSchedule, day: number) {
  return guide.schedule.find((s) => s.day === day);
}
</script>

<template>
  <Page title="导游排班汇总" description="查看导游排班日历">
    <Card>
      <div class="mb-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <Button>上一周</Button>
          <span class="text-lg font-medium">2024年6月17日 - 6月23日</span>
          <Button>下一周</Button>
        </div>
        <Button type="primary">排班设置</Button>
      </div>
      <div class="overflow-x-auto">
        <div class="grid min-w-[900px] grid-cols-8 gap-px rounded-lg border bg-gray-200">
          <!-- Header -->
          <div class="bg-gray-50 p-3 font-medium">导游</div>
          <div
            v-for="(day, index) in weekDays"
            :key="index"
            class="bg-gray-50 p-3 text-center font-medium"
          >
            <div>{{ day }}</div>
            <div class="text-xs text-gray-500">{{ weekDates[index] }}</div>
          </div>
          <!-- Guide rows -->
          <template v-for="guide in guides" :key="guide.id">
            <div class="flex items-center bg-white p-3">
              <div>
                <div class="font-medium">{{ guide.name }}</div>
                <div class="text-xs text-gray-400">{{ guide.phone }}</div>
              </div>
            </div>
            <div
              v-for="dayIndex in 7"
              :key="dayIndex"
              class="flex items-center justify-center bg-white p-2"
            >
              <div
                v-if="getScheduleForDay(guide, dayIndex - 1)"
                :class="getScheduleForDay(guide, dayIndex - 1)!.color"
                class="w-full rounded px-2 py-1 text-center text-xs"
              >
                <div class="font-medium">{{ getScheduleForDay(guide, dayIndex - 1)!.teamNo }}</div>
                <div>{{ getScheduleForDay(guide, dayIndex - 1)!.product }}</div>
              </div>
              <span v-else class="text-gray-300">-</span>
            </div>
          </template>
        </div>
      </div>
      <div class="mt-4 flex items-center gap-4 text-sm text-gray-500">
        <span>图例：</span>
        <Tag color="blue">已确认</Tag>
        <Tag color="orange">待确认</Tag>
        <Tag color="default">休息</Tag>
      </div>
    </Card>
  </Page>
</template>
