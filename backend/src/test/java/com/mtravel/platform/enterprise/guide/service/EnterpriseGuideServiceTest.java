package com.mtravel.platform.enterprise.guide.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideSaveRequest;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagEntity;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagRelationEntity;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideTagMapper;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideTagRelationMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseGuideServiceTest {

    @Test
    void createShouldRejectDuplicateGuideCode() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideService service = service(mapper);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request(), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("导游编码已存在");

        verify(mapper, never()).insert(any(EnterpriseGuideEntity.class));
    }

    @Test
    void createShouldPersistGuideProfileFieldsAndTagRelations() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideTagMapper tagMapper = mock(EnterpriseGuideTagMapper.class);
        EnterpriseGuideTagRelationMapper relationMapper = mock(EnterpriseGuideTagRelationMapper.class);
        EnterpriseEmployeeMapper employeeMapper = mock(EnterpriseEmployeeMapper.class);
        EnterpriseGuideService service = new EnterpriseGuideService(mapper, tagMapper, relationMapper, employeeMapper);
        ArgumentCaptor<EnterpriseGuideEntity> captor = ArgumentCaptor.forClass(EnterpriseGuideEntity.class);
        ArgumentCaptor<EnterpriseGuideTagRelationEntity> relationCaptor =
                ArgumentCaptor.forClass(EnterpriseGuideTagRelationEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(employee(7L, "张导管"));
        when(tagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag(31L, "金牌导游"), tag(32L, "研学")));
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation(12L, 31L), relation(12L, 32L)));
        doAnswer((Answer<Integer>) invocation -> {
            EnterpriseGuideEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return 1;
        }).when(mapper).insert(any(EnterpriseGuideEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            verify(mapper).insert(captor.capture());
            EnterpriseGuideEntity entity = captor.getValue();
            entity.setTenantId(1L);
            return entity;
        });

        service.create(request(), 1L, "admin");

        verify(relationMapper, times(2)).insert(relationCaptor.capture());
        EnterpriseGuideEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getGuideCode()).isEqualTo("GD001");
        assertThat(entity.getGuideName()).isEqualTo("陈导");
        assertThat(entity.getUsername()).isEqualTo("chendao");
        assertThat(entity.getGuideManagerEmployeeId()).isEqualTo(7L);
        assertThat(entity.getGuideManagerName()).isEqualTo("张导管");
        assertThat(entity.getGender()).isEqualTo("male");
        assertThat(entity.getFax()).isEqualTo("0571-88001002");
        assertThat(entity.getCertificateNo()).isEqualTo("D-330100001");
        assertThat(entity.getAge()).isEqualTo(36);
        assertThat(entity.getNativePlace()).isEqualTo("浙江杭州");
        assertThat(entity.getWorkingYears()).isEqualTo(12);
        assertThat(entity.getMobilePhone()).isEqualTo("13900139001");
        assertThat(entity.getBankName()).isEqualTo("招商银行杭州分行");
        assertThat(entity.getBankAccountNo()).isEqualTo("6222000000000011");
        assertThat(entity.getAlipayName()).isEqualTo("陈导");
        assertThat(entity.getAlipayAccount()).isEqualTo("chendao@alipay");
        assertThat(entity.getLanguages()).isEqualTo("普通话,英语");
        assertThat(entity.getPersonalIntro()).isEqualTo("擅长研学和亲子团队");
        assertThat(entity.getCertificateFileUrl()).isEqualTo("https://example.com/certificate.jpg");
        assertThat(entity.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(entity.getEnterpriseCodeStatus()).isEqualTo("signed_success");
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getRating()).isEqualByComparingTo("4.80");
        assertThat(entity.getTotalTours()).isEqualTo(18);
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
        assertThat(relationCaptor.getAllValues()).extracting(EnterpriseGuideTagRelationEntity::getTagId)
                .containsExactly(31L, 32L);
    }

    @Test
    void createShouldRejectRatingOutOfRange() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideService service = service(mapper);
        EnterpriseGuideSaveRequest bad = new EnterpriseGuideSaveRequest(
                "GD001", "陈导", "chendao", null, null, "male", "D-330100001", "3301",
                "0571-88001001", "0571-88001002", "13900139001", "招商银行", "账户",
                "支付宝姓名", "支付宝", "ecode", "signed_success", null, null, null, null,
                null, null, null, new BigDecimal("5.50"), 18, 10, "active", "备注"
        );

        assertThatThrownBy(() -> service.create(bad, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("导游评分必须在0到5之间");
    }

    @Test
    void disableShouldUpdateStatus() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideService service = service(mapper);
        ArgumentCaptor<EnterpriseGuideEntity> captor = ArgumentCaptor.forClass(EnterpriseGuideEntity.class);

        when(mapper.update(any(EnterpriseGuideEntity.class), any(Wrapper.class))).thenReturn(1);

        service.disable(12L, 1L);

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo("disabled");
    }

    @Test
    void deleteShouldSoftDeleteGuide() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideService service = service(mapper);
        ArgumentCaptor<EnterpriseGuideEntity> captor = ArgumentCaptor.forClass(EnterpriseGuideEntity.class);

        when(mapper.update(any(EnterpriseGuideEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(12L, 1L, "admin");

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void sendEnterpriseCodeInviteShouldMarkInviteLinkAndTime() {
        EnterpriseGuideMapper mapper = mock(EnterpriseGuideMapper.class);
        EnterpriseGuideService service = service(mapper);
        ArgumentCaptor<EnterpriseGuideEntity> captor = ArgumentCaptor.forClass(EnterpriseGuideEntity.class);

        when(mapper.update(any(EnterpriseGuideEntity.class), any(Wrapper.class))).thenReturn(1);

        service.sendEnterpriseCodeInvite(12L, 1L);

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getEnterpriseCodeStatus()).isEqualTo("invite_link");
        assertThat(captor.getValue().getEnterpriseCodeInvitedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    private EnterpriseGuideSaveRequest request() {
        return new EnterpriseGuideSaveRequest(
                "GD001",
                "陈导",
                "chendao",
                7L,
                List.of(31L, 32L),
                "male",
                "D-330100001",
                "330100199001010011",
                "0571-88001001",
                "0571-88001002",
                "13900139001",
                "招商银行杭州分行",
                "6222000000000011",
                "陈导",
                "chendao@alipay",
                "ECODE-001",
                "signed_success",
                36,
                "浙江杭州",
                12,
                "普通话,英语",
                "擅长研学和亲子团队",
                "https://example.com/certificate.jpg",
                "https://example.com/photo.jpg",
                new BigDecimal("4.80"),
                18,
                10,
                "active",
                "擅长研学团队"
        );
    }

    private EnterpriseGuideService service(EnterpriseGuideMapper mapper) {
        return new EnterpriseGuideService(
                mapper,
                mock(EnterpriseGuideTagMapper.class),
                mock(EnterpriseGuideTagRelationMapper.class),
                mock(EnterpriseEmployeeMapper.class)
        );
    }

    private EnterpriseEmployeeEntity employee(Long id, String name) {
        EnterpriseEmployeeEntity entity = new EnterpriseEmployeeEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setEmployeeName(name);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private EnterpriseGuideTagEntity tag(Long id, String name) {
        EnterpriseGuideTagEntity entity = new EnterpriseGuideTagEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTagName(name);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private EnterpriseGuideTagRelationEntity relation(Long guideId, Long tagId) {
        EnterpriseGuideTagRelationEntity entity = new EnterpriseGuideTagRelationEntity();
        entity.setId(tagId);
        entity.setTenantId(1L);
        entity.setGuideId(guideId);
        entity.setTagId(tagId);
        entity.setIsDeleted(false);
        return entity;
    }
}
