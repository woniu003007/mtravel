package com.mtravel.platform.enterprise.companyinfo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoSaveRequest;
import com.mtravel.platform.enterprise.companyinfo.entity.EnterpriseCompanyInfoEntity;
import com.mtravel.platform.enterprise.companyinfo.mapper.EnterpriseCompanyInfoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseCompanyInfoServiceTest {

    @Test
    void saveShouldCreateCompanyInfoWhenTenantHasNoRecord() {
        EnterpriseCompanyInfoMapper mapper = mock(EnterpriseCompanyInfoMapper.class);
        EnterpriseCompanyInfoService service = new EnterpriseCompanyInfoService(mapper);
        EnterpriseCompanyInfoSaveRequest request = request("测试地接社");
        ArgumentCaptor<EnterpriseCompanyInfoEntity> captor = ArgumentCaptor.forClass(EnterpriseCompanyInfoEntity.class);

        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null).thenAnswer(invocation -> {
            verify(mapper).insert(captor.capture());
            return captor.getValue();
        });
        doAnswer((Answer<Integer>) invocation -> {
            EnterpriseCompanyInfoEntity entity = invocation.getArgument(0);
            entity.setId(18L);
            return 1;
        }).when(mapper).insert(any(EnterpriseCompanyInfoEntity.class));

        service.save(request, 1L, "admin");

        EnterpriseCompanyInfoEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(1L);
        assertThat(saved.getCompanyName()).isEqualTo("测试地接社");
        assertThat(saved.getContactName()).isEqualTo("老板测试");
        assertThat(saved.getContactPhone()).isEqualTo("13501750765");
        assertThat(saved.getFaxNumber()).isEqualTo("142");
        assertThat(saved.getOfficeAddress()).isEqualTo("浙江省杭州市湖墅南路103号百大花园商务楼7楼");
        assertThat(saved.getAlipayEnterpriseName()).isEqualTo("杭州博客国际旅行社有限公司");
        assertThat(saved.getAlipayAccount()).isEqualTo("bkzhanghu04@163.com");
        assertThat(saved.getAlipayNickname()).isEqualTo("博客旅行");
        assertThat(saved.getSignStatus()).isEqualTo("signed");
        assertThat(saved.getCreatedBy()).isEqualTo("admin");
        assertThat(saved.getIsDeleted()).isFalse();
    }

    @Test
    void saveShouldUpdateExistingCompanyInfoForSameTenant() {
        EnterpriseCompanyInfoMapper mapper = mock(EnterpriseCompanyInfoMapper.class);
        EnterpriseCompanyInfoService service = new EnterpriseCompanyInfoService(mapper);
        EnterpriseCompanyInfoEntity existing = new EnterpriseCompanyInfoEntity();
        existing.setId(9L);
        existing.setTenantId(1L);
        ArgumentCaptor<EnterpriseCompanyInfoEntity> captor = ArgumentCaptor.forClass(EnterpriseCompanyInfoEntity.class);

        when(mapper.selectOne(any(Wrapper.class))).thenReturn(existing).thenAnswer(invocation -> {
            verify(mapper).update(captor.capture(), any(Wrapper.class));
            EnterpriseCompanyInfoEntity updated = captor.getValue();
            updated.setId(9L);
            updated.setTenantId(1L);
            return updated;
        });
        when(mapper.update(any(EnterpriseCompanyInfoEntity.class), any(Wrapper.class))).thenReturn(1);

        service.save(request("修改后的地接社"), 1L, "admin");

        EnterpriseCompanyInfoEntity updated = captor.getValue();
        assertThat(updated.getCompanyName()).isEqualTo("修改后的地接社");
        assertThat(updated.getStatus()).isEqualTo("active");
        assertThat(updated.getIsDeleted()).isNull();
        assertThat(updated.getCreatedBy()).isNull();
    }

    private EnterpriseCompanyInfoSaveRequest request(String companyName) {
        return new EnterpriseCompanyInfoSaveRequest(
                companyName,
                "浙江省",
                "杭州市",
                "杭州市区",
                "老板测试",
                "13501750765",
                "142",
                "浙江省杭州市湖墅南路103号百大花园商务楼7楼",
                "杭州博客国际旅行社有限公司",
                "bkzhanghu04@163.com",
                "博客旅行",
                "signed",
                "https://example.com/sign",
                "active",
                "公司信息备注"
        );
    }
}
