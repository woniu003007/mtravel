import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

export interface EmployeeRecord {
  code?: string;
  createTime?: string;
  department?: string;
  gender?: string;
  id?: number;
  name: string;
  phone?: string;
  role?: string;
  status: number;
  username?: string;
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '员工姓名',
      rules: z.string().min(1, '请输入员工姓名'),
    },
    {
      component: 'Input',
      fieldName: 'username',
      label: '用户名',
      rules: z.string().min(1, '请输入用户名'),
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '总经办', value: '总经办' },
          { label: '销售部', value: '销售部' },
          { label: '计调部', value: '计调部' },
          { label: '财务部', value: '财务部' },
          { label: '导游部', value: '导游部' },
        ],
        placeholder: '请选择部门',
      },
      fieldName: 'department',
      label: '部门',
      rules: 'selectRequired',
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '管理员', value: '管理员' },
          { label: '销售', value: '销售' },
          { label: '计调', value: '计调' },
          { label: '财务', value: '财务' },
          { label: '导游', value: '导游' },
        ],
        placeholder: '请选择角色',
      },
      fieldName: 'role',
      label: '角色',
      rules: 'selectRequired',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '男', value: '男' },
          { label: '女', value: '女' },
        ],
      },
      defaultValue: '男',
      fieldName: 'gender',
      label: '性别',
    },
    {
      component: 'Input',
      fieldName: 'phone',
      label: '手机号',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
        optionType: 'button',
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
    },
  ];
}

export function useColumns(
  onActionClick?: OnActionClickFn<EmployeeRecord>,
): VxeTableGridColumns<EmployeeRecord> {
  return [
    { field: 'code', title: '业务代码', width: 100 },
    { field: 'name', title: '员工姓名', width: 100 },
    { field: 'username', title: '用户名', width: 100 },
    { field: 'department', title: '部门', width: 100 },
    { field: 'role', title: '角色', width: 80 },
    { field: 'gender', title: '性别', width: 60 },
    { field: 'phone', title: '电话', width: 130 },
    { field: 'createTime', title: '创建时间', width: 170 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      cellRender: { name: 'CellTag' },
    },
    {
      field: 'operation',
      title: '操作',
      width: 150,
      align: 'right',
      fixed: 'right',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '员工',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
    },
  ];
}
