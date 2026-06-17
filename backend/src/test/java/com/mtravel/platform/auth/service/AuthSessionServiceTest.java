package com.mtravel.platform.auth.service;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSessionServiceTest {

    @Test
    void createSessionShouldAllowSameUserToLoginAgainDuringDevelopment() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn("old-session");
        AuthSessionService service = new AuthSessionService(provider);

        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));

        String sessionId = service.createSession(user, Duration.ofMinutes(120));

        assertThat(sessionId).isNotBlank();
        verify(valueOperations).set(eq("auth:session:" + sessionId), any(String.class), eq(Duration.ofMinutes(120)));
    }

    @Test
    void createSessionShouldStoreSessionAndUserMappingWithSameTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn(null);
        AuthSessionService service = new AuthSessionService(provider);

        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));
        String sessionId = service.createSession(user, Duration.ofMinutes(120));

        assertThat(sessionId).isNotBlank();
        verify(valueOperations).set(eq("auth:session:" + sessionId), any(String.class), eq(Duration.ofMinutes(120)));
        verify(valueOperations).set("auth:user-session:1:9", sessionId, Duration.ofMinutes(120));
    }

    @Test
    void validateSessionShouldRefreshTtlWhenSessionMatchesCurrentUserMapping() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("auth:session:sid-1")).thenReturn(true);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn("sid-1");
        AuthSessionService service = new AuthSessionService(provider);

        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));

        assertThat(service.validateAndRefresh(user, "sid-1", Duration.ofMinutes(30))).isTrue();
        verify(redisTemplate).expire("auth:session:sid-1", Duration.ofMinutes(30));
        verify(redisTemplate).expire("auth:user-session:1:9", Duration.ofMinutes(30));
    }

    @Test
    void validateSessionShouldAllowOlderSessionWhenSameUserLoggedInAgainDuringDevelopment() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("auth:session:sid-1")).thenReturn(true);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn("sid-2");
        AuthSessionService service = new AuthSessionService(provider);

        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));

        assertThat(service.validateAndRefresh(user, "sid-1", Duration.ofMinutes(30))).isTrue();
        verify(redisTemplate).expire("auth:session:sid-1", Duration.ofMinutes(30));
    }

    @Test
    void logoutShouldDeleteSessionAndUserMapping() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn("sid-1");
        AuthSessionService service = new AuthSessionService(provider);
        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));

        service.logout(user, "sid-1");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, org.mockito.Mockito.times(2)).delete(keyCaptor.capture());
        assertThat(keyCaptor.getAllValues()).containsExactlyInAnyOrder("auth:session:sid-1", "auth:user-session:1:9");
    }

    @Test
    void logoutShouldKeepLatestUserMappingWhenOldSessionLogsOutDuringDevelopment() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mockProvider(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:user-session:1:9")).thenReturn("sid-2");
        AuthSessionService service = new AuthSessionService(provider);
        AuthenticatedUser user = new AuthenticatedUser(9L, "demo01", "测试用户", 1L, List.of("admin"));

        service.logout(user, "sid-1");

        verify(redisTemplate).delete("auth:session:sid-1");
        verify(redisTemplate, org.mockito.Mockito.never()).delete("auth:user-session:1:9");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> mockProvider(StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        return provider;
    }
}
