package com.mtravel.platform.enterprise.productdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionaryResponse;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionarySaveRequest;
import com.mtravel.platform.enterprise.productdictionary.entity.EnterpriseProductDictionaryEntity;
import com.mtravel.platform.enterprise.productdictionary.mapper.EnterpriseProductDictionaryMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 产品字典服务测试。
 *
 * <p>产品字典会被后续产品模板和团期管理复用，因此这里优先验证租户隔离、默认值、去重、
 * 启用列表和软删除等核心业务规则。</p>
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseProductDictionaryServiceTest {

    @Mock
    private EnterpriseProductDictionaryMapper mapper;

    @InjectMocks
    private EnterpriseProductDictionaryService service;

    @Test
    void createDefaultsStatusAndSortThenWritesTenantAndOperator() {
        AtomicReference<EnterpriseProductDictionaryEntity> saved = new AtomicReference<>();
        when(mapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(EnterpriseProductDictionaryEntity.class))).thenAnswer(invocation -> {
            EnterpriseProductDictionaryEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            saved.set(entity);
            return 1;
        });
        when(mapper.selectOne(any(QueryWrapper.class))).thenAnswer(invocation -> saved.get());

        EnterpriseProductDictionaryResponse response = service.create(
                new EnterpriseProductDictionarySaveRequest(" business_type ", " 疗休养 ", null, null, " 适合企事业疗休养 "),
                1001L,
                "admin"
        );

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.dictType()).isEqualTo("business_type");
        assertThat(response.dictName()).isEqualTo("疗休养");
        assertThat(response.sortOrder()).isZero();
        assertThat(response.status()).isEqualTo("active");
        assertThat(response.remark()).isEqualTo("适合企事业疗休养");
        assertThat(saved.get().getTenantId()).isEqualTo(1001L);
        assertThat(saved.get().getCreatedBy()).isEqualTo("admin");
        assertThat(saved.get().getIsDeleted()).isFalse();
    }

    @Test
    void createRejectsDuplicateNameInSameDictionaryType() {
        when(mapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(
                new EnterpriseProductDictionarySaveRequest("business_type", "疗休养", 1, "active", null),
                1001L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("产品字典已存在");
    }

    @Test
    void createAllowsSameNameInDifferentDictionaryType() {
        when(mapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(EnterpriseProductDictionaryEntity.class))).thenAnswer(invocation -> {
            EnterpriseProductDictionaryEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return 1;
        });
        when(mapper.selectOne(any(QueryWrapper.class))).thenAnswer(invocation -> {
            EnterpriseProductDictionaryEntity entity = new EnterpriseProductDictionaryEntity();
            entity.setId(12L);
            entity.setTenantId(1001L);
            entity.setDictType("product_theme");
            entity.setDictName("亲子游");
            entity.setSortOrder(3);
            entity.setStatus("active");
            entity.setIsDeleted(false);
            return entity;
        });

        EnterpriseProductDictionaryResponse response = service.create(
                new EnterpriseProductDictionarySaveRequest("product_theme", "亲子游", 3, "active", null),
                1001L,
                "admin"
        );

        assertThat(response.dictType()).isEqualTo("product_theme");
        assertThat(response.dictName()).isEqualTo("亲子游");
    }

    @Test
    void listActiveReturnsOnlyEnabledRowsInServiceOrder() {
        EnterpriseProductDictionaryEntity first = new EnterpriseProductDictionaryEntity();
        first.setId(1L);
        first.setDictType("business_type");
        first.setDictName("定制团");
        first.setSortOrder(1);
        first.setStatus("active");

        EnterpriseProductDictionaryEntity second = new EnterpriseProductDictionaryEntity();
        second.setId(2L);
        second.setDictType("business_type");
        second.setDictName("疗休养");
        second.setSortOrder(2);
        second.setStatus("active");

        when(mapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(first, second));

        List<EnterpriseProductDictionaryResponse> rows = service.listActive(1001L, "business_type");

        assertThat(rows).extracting(EnterpriseProductDictionaryResponse::dictName)
                .containsExactly("定制团", "疗休养");

        ArgumentCaptor<QueryWrapper<EnterpriseProductDictionaryEntity>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("status");
    }

    @Test
    void deleteSoftDeletesDictionaryRow() {
        when(mapper.update(any(EnterpriseProductDictionaryEntity.class), any(UpdateWrapper.class))).thenReturn(1);

        service.delete(10L, 1001L, "admin");

        ArgumentCaptor<EnterpriseProductDictionaryEntity> captor = ArgumentCaptor.forClass(EnterpriseProductDictionaryEntity.class);
        verify(mapper).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }
}
