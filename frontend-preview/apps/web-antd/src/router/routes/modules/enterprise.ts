import type { RouteRecordRaw } from 'vue-router';

const BankAccountPage = () => import('#/views/enterprise/bank-account/index.vue');
const CompanyInfoPage = () => import('#/views/enterprise/company-info/index.vue');
const DepartmentPage = () => import('#/views/enterprise/department/index.vue');
const EmployeePage = () => import('#/views/enterprise/employee/index.vue');
const ExpenseItemPage = () => import('#/views/enterprise/expense-item/index.vue');
const GuidePage = () => import('#/views/enterprise/guide/index.vue');
const ProductDictionaryPage = () => import('#/views/enterprise/product-dictionary/index.vue');
const RolePage = () => import('#/views/enterprise/role/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:building-2', order: 7, title: '企业资料' },
    name: 'Enterprise',
    path: '/enterprise',
    redirect: '/enterprise/company-info',
    children: [
      { name: 'CompanyInfo', path: '/enterprise/company-info', component: CompanyInfoPage, meta: { icon: 'lucide:building-2', title: '公司信息' } },
      { name: 'BankAccount', path: '/enterprise/bank-account', component: BankAccountPage, meta: { icon: 'lucide:landmark', title: '银行账号' } },
      { name: 'Department', path: '/enterprise/department', component: DepartmentPage, meta: { icon: 'lucide:network', title: '部门管理' } },
      { name: 'Role', path: '/enterprise/role', component: RolePage, meta: { icon: 'lucide:shield', title: '角色权限' } },
      { name: 'Employee', path: '/enterprise/employee', component: EmployeePage, meta: { icon: 'lucide:users', title: '员工管理' } },
      { name: 'Guide', path: '/enterprise/guide', component: GuidePage, meta: { icon: 'lucide:map-pin', title: '导游管理' } },
      { name: 'ExpenseItem', path: '/enterprise/expense-item', component: ExpenseItemPage, meta: { icon: 'lucide:receipt', title: '费用项目' } },
      { name: 'ProductDictionary', path: '/enterprise/product-dictionary', component: ProductDictionaryPage, meta: { icon: 'lucide:list-checks', title: '产品字典' } },
    ],
  },
];

export default routes;
