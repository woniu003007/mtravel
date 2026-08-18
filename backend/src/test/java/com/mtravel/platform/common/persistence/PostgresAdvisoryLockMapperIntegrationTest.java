package com.mtravel.platform.common.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalRequestMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestPaymentMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL 事务级建议锁 Mapper 集成测试。
 *
 * <p>测试只获取当前事务内的建议锁，不修改业务表；需显式启用，避免普通单元测试依赖外部数据库。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class PostgresAdvisoryLockMapperIntegrationTest {

    @Autowired
    private SalesTeamMapper teamMapper;

    @Autowired
    private SalesBookingOrderMapper orderMapper;

    @Autowired
    private CustomerRiskApprovalRequestMapper riskApprovalMapper;

    @Autowired
    private FinanceGuideImprestMapper guideImprestMapper;

    @Autowired
    private FinanceGuideImprestPaymentMapper guideImprestPaymentMapper;

    /**
     * 验证 PostgreSQL void 锁函数的返回值可被 MyBatis 正确映射。
     */
    @Test
    void shouldMapAllAdvisoryLockResultsWithoutVoidConstructionError() {
        assertDoesNotThrow(() -> {
            teamMapper.lockTeamNoGeneration(1L, "mapper-integration-test");
            orderMapper.lockOrderNoGeneration(1L, "mapper-integration-test");
            riskApprovalMapper.lockRequestNoGeneration(1L, "mapper-integration-test");
            guideImprestMapper.lockRequestNoGeneration(1L, "mapper-integration-test");
            guideImprestPaymentMapper.lockPaymentNoGeneration(1L, "mapper-integration-test");
        });
    }
}
