# MTravel 详细业务流程图

> 说明：本流程图基于 2026-05-09 对 `http://www.mtravel.cn` 的只读调研整理，重点描述系统中销售、计调、财务、统计之间的标准业务链。

## 一、系统全流程图

```mermaid
flowchart TB
    Start([开始使用系统]) --> Setup[准备基础资料]

    Setup --> Base1[企业资料<br/>部门 角色 员工 导游 银行账号]
    Setup --> Base2[系统设置<br/>参数 电子合同 日志 快捷菜单]
    Setup --> Base3[客户资料<br/>客户单位 客户分类 客户合同]
    Setup --> Base4[采购资料<br/>资源 供应商 采购关系 采购合同 车辆]

    Base1 --> Sales
    Base2 --> Sales
    Base3 --> Sales
    Base4 --> Sales

    Sales[销售管理] --> Product[产品管理]
    Product --> Schedule[团期管理]
    Schedule --> Team[团队管理]
    Team --> Order[订单管理]
    Order --> Tourist[客人信息]
    Tourist --> Duplicate[名单查重]
    Order --> Contract[电子合同]
    Order --> ExpenseChange[订单费用变更]
    Order --> Files[订单文件]

    Team --> Ops[计调操作]
    Order --> Ops

    Ops --> Arrange[团队安排]
    Arrange --> GuidePlan[导游安排]
    Arrange --> TrafficPlan[交通安排]
    Arrange --> HotelPlan[住宿安排]
    Arrange --> BusPlan[用车安排]
    Arrange --> TicketPlan[景区安排]
    Arrange --> MealPlan[用餐安排]
    Arrange --> OtherPlan[其他安排]
    Arrange --> ZifeiPlan[自费安排]
    Arrange --> ShopPlan[购物安排]
    Arrange --> TravelPlan[地接安排]
    Arrange --> BusInfo[接送信息]
    Arrange --> RoomStatus[房态控制]
    Arrange --> GuideSchedule[导游排班汇总]

    GuidePlan --> OpAudit[计调审核]
    TrafficPlan --> OpAudit
    HotelPlan --> OpAudit
    BusPlan --> OpAudit
    TicketPlan --> OpAudit
    MealPlan --> OpAudit
    OtherPlan --> OpAudit
    ZifeiPlan --> OpAudit
    ShopPlan --> OpAudit
    TravelPlan --> OpAudit

    OpAudit --> FinanceAudit[财务审核]

    FinanceAudit --> AR[应收账款 / 应收明细]
    FinanceAudit --> AP[应付账款 / 应付明细]
    FinanceAudit --> Prepay[团队预付款]
    FinanceAudit --> GuideCash[导游备用金]
    FinanceAudit --> GuideSettle[导游结算]
    FinanceAudit --> Rebate[购物返佣]
    FinanceAudit --> Invoice[发票记录 / 付款发票]
    FinanceAudit --> Progress[财务收支进度]

    AR --> Income[收款记录]
    AP --> Payment[付款记录]
    Income --> CashBook[银行现金账]
    Payment --> CashBook
    GuideCash --> Payment
    GuideSettle --> Income
    GuideSettle --> Payment
    Rebate --> Income
    AR --> Offset[应收应付冲抵]
    AP --> Offset

    FinanceAudit --> Stats[数据统计]
    Income --> Stats
    Payment --> Stats
    GuideSettle --> Stats
    Rebate --> Stats

    Stats --> GroupStat[团队进度统计]
    Stats --> ReceiveStat[收客统计]
    Stats --> ProfitStat[利润统计]
    Stats --> ResourceStat[资源采购统计]
    Stats --> AccountStat[账款统计]
    Stats --> GuideStat[导游统计]
    Stats --> IncomeStat[收款汇总统计]
    Stats --> PaymentStat[付款汇总统计]
    Stats --> ReportCenter[报表中心]

    ReportCenter --> End([经营分析 / 管理决策])
```

## 二、销售业务流程图

```mermaid
flowchart LR
    A[客户单位] --> B[产品管理]
    B --> C[团期管理]
    C --> D[团队管理]
    D --> E[订单管理]
    E --> F[游客名单]
    F --> G[名单查重]
    E --> H[电子合同]
    E --> I[订单费用变更]
    E --> J[订单文件]
    E --> K[形成团队收客结果]

    subgraph SalesKey[销售阶段关键结果]
        K1[团队已建立]
        K2[订单已录入]
        K3[游客名单已补全]
        K4[合同和费用说明已准备]
    end

    D --> K1
    E --> K2
    F --> K3
    H --> K4
    I --> K4
```

### 销售阶段说明

- `产品`解决卖什么
- `团期`解决什么时候发
- `团队`解决这批业务归到哪个团
- `订单`解决客户实际报名和应收金额
- `游客名单/电子合同`解决出团资料和合规留痕

## 三、计调履约流程图

```mermaid
flowchart TB
    Start[销售团队/订单已形成] --> TeamEntry[进入团队安排]

    TeamEntry --> Overview[团队总览]

    Overview --> P1[导游安排]
    Overview --> P2[交通安排]
    Overview --> P3[住宿安排]
    Overview --> P4[用车安排]
    Overview --> P5[景区安排]
    Overview --> P6[用餐安排]
    Overview --> P7[其他安排]
    Overview --> P8[自费安排]
    Overview --> P9[购物安排]
    Overview --> P10[地接安排]

    P1 --> Cost[形成团队成本]
    P2 --> Cost
    P3 --> Cost
    P4 --> Cost
    P5 --> Cost
    P6 --> Cost
    P7 --> Cost
    P8 --> Cost
    P9 --> Cost
    P10 --> Cost

    TeamEntry --> Support1[接送信息]
    TeamEntry --> Support2[房态控制]
    TeamEntry --> Support3[导游排班汇总]

    Cost --> Check{资源是否排全}
    Check -->|否| Fix[补排资源 / 修正费用]
    Fix --> Check
    Check -->|是| OpAudit[进入计调审核]

    OpAudit --> Result[形成可财务审核的团队账单]
```

### 计调阶段说明

- 计调的核心不是只安排导游，而是把整个团队的资源和成本排完整。
- 资源页是分开的，但最终都汇总到同一个团队账单。
- 计调审核完成，说明履约信息和主要成本已经成型。

## 四、财务结算流程图

```mermaid
flowchart TB
    A[计调审核完成] --> B[财务团队审核]

    B --> C1[应收账款]
    B --> C2[应付账款]
    B --> C3[团队预付款]
    B --> C4[导游备用金]
    B --> C5[导游结算]
    B --> C6[购物返佣]
    B --> C7[发票记录]
    B --> C8[财务收支进度]

    C1 --> D1[收款记录]
    C2 --> D2[付款记录]
    C3 --> D2
    C4 --> D2
    C5 --> D1
    C5 --> D2
    C6 --> D1

    D1 --> E[银行现金账]
    D2 --> E
    C1 --> F[应收汇总]
    C2 --> G[应付汇总]
    D1 --> H[收款汇总统计]
    D2 --> I[付款汇总统计]
    C7 --> J[票据和开票台账]
    C8 --> K[团队收支进度总览]

    E --> L[最终形成团队收支闭环]
```

### 财务阶段说明

- 财务不是从零记账，而是对团队已有业务单据做确认和结算。
- `应收`对应客户，`应付`对应供应商。
- `收款记录`、`付款记录`、`银行现金账`必须保持一致。
- `导游结算`和`购物返佣`是财务链里单独的重要环节。

## 五、老板看数流程图

```mermaid
flowchart LR
    A[销售结果] --> E[数据统计]
    B[计调结果] --> E
    C[财务结果] --> E
    D[导游结算 / 返佣结果] --> E

    E --> F1[团队进度统计]
    E --> F2[收客统计]
    E --> F3[利润统计]
    E --> F4[资源采购统计]
    E --> F5[账款统计]
    E --> F6[导游统计]
    E --> F7[报表中心]

    F1 --> G[看团队推进情况]
    F2 --> G2[看收客情况]
    F3 --> G3[看利润情况]
    F4 --> G4[看采购结构]
    F5 --> G5[看资金回收和支付]
    F6 --> G6[看导游使用和收益]
    F7 --> G7[导出正式管理报表]
```

### 管理层使用说明

老板通常不需要深入所有录入页，重点看：

- 团队进度是否卡住
- 哪些团利润异常
- 哪些客户没回款
- 哪些供应商待付款
- 哪些导游和购物返佣影响利润

## 六、系统控制点流程图

```mermaid
flowchart TB
    Start[开始业务] --> Rule1[系统参数]
    Rule1 --> Rule2[角色权限]
    Rule2 --> Rule3[员工和导游账号]

    Rule3 --> Sales[销售建团下单]
    Sales --> Ops[计调排团]
    Ops --> Check1{团队安排完成?}
    Check1 -->|否| OpsFix[继续补排]
    OpsFix --> Check1
    Check1 -->|是| Audit1[计调审核]

    Audit1 --> Check2{财务审核通过?}
    Check2 -->|否| FinanceFix[补充成本/账款/资料]
    FinanceFix --> Check2
    Check2 -->|是| Lock[进入财务闭环]

    Lock --> Money[收款 付款 导游结算 返佣 发票]
    Money --> Log[系统操作日志]
    Log --> Report[统计报表]
```

### 控制点说明

系统最关键的控制点有四个：

1. `系统参数`
   决定流程规则和统计口径。

2. `团队安排完成`
   决定能否顺利进入审核。

3. `财务审核通过`
   决定能否进入稳定结算状态。

4. `系统操作日志`
   决定关键操作是否可追溯。

## 七、异常处理路径图

```mermaid
flowchart LR
    A[订单已建] --> B{游客名单完整?}
    B -->|否| B1[补游客名单 / 名单查重]
    B1 --> B
    B -->|是| C{资源安排完整?}

    C -->|否| C1[补导游/房/车/景/餐/地接]
    C1 --> C
    C -->|是| D{计调审核通过?}

    D -->|否| D1[修正资源或成本]
    D1 --> D
    D -->|是| E{财务审核通过?}

    E -->|否| E1[修正应收应付/票据/结算]
    E1 --> E
    E -->|是| F[进入正常收付款和统计]
```

### 异常处理说明

系统的标准修正路径是：

- 名单不全，回到游客名单
- 排团不全，回到团队安排
- 计调审核不过，回到资源和成本
- 财务审核不过，回到账款和票据

## 八、业务流程结论

这套系统的业务流程可以压缩成一句话：

`先建基础资料，再建产品和团队，再挂订单和游客，再做计调履约，再做财务审核和结算，最后做统计和管理分析。`

如果只抓最核心的主线，就是：

`产品 -> 团期 -> 团队 -> 订单 -> 团队安排 -> 计调审核 -> 财务审核 -> 收付款 -> 利润统计`
