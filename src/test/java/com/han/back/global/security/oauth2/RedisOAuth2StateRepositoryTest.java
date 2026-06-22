package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.redis.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RedisOAuth2StateRepository")
class RedisOAuth2StateRepositoryTest {

    private static final String STATE = "test-state-value";
    private static final String KEY = OAuth2Const.OAUTH2_STATE_PREFIX + STATE;

    private RedisUtil redisUtil;
    private ObjectMapper objectMapper;
    private RedisOAuth2StateRepository repository;

    @BeforeEach
    void setUp() {
        redisUtil = mock(RedisUtil.class);
        objectMapper = new ObjectMapper();
        repository = new RedisOAuth2StateRepository(redisUtil, objectMapper);
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("test-client-id")
                .redirectUri("https://service.com/login/oauth2/code/kakao")
                .scope("profile_nickname")
                .state(STATE)
                .additionalParameters(Map.of("prompt", "login"))
                .attributes(Map.of("registration_id", "kakao"))
                .build();
    }

    @Test
    @DisplayName("저장 후 동일 state로 다시 읽으면 모든 필드가 보존된 채 복원된다")
    void serializeRoundTrip_preservesAllFields() {
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        OAuth2AuthorizationRequest original = authorizationRequest();

        repository.saveAuthorizationRequest(original, saveRequest, new MockHttpServletResponse());

        var jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(redisUtil).setDataExpire(eq(KEY), jsonCaptor.capture(), eq(OAuth2Const.OAUTH2_STATE_TTL));
        String storedJson = jsonCaptor.getValue();

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setParameter(OAuth2Const.PARAM_STATE, STATE);
        given(redisUtil.getData(KEY)).willReturn(Optional.of(storedJson));

        OAuth2AuthorizationRequest restored = repository.loadAuthorizationRequest(loadRequest);

        assertThat(restored).isNotNull();
        assertThat(restored.getState()).isEqualTo(original.getState());
        assertThat(restored.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
        assertThat(restored.getClientId()).isEqualTo(original.getClientId());
        assertThat(restored.getRedirectUri()).isEqualTo(original.getRedirectUri());
        assertThat(restored.getScopes()).isEqualTo(original.getScopes());
        assertThat(restored.getAdditionalParameters()).isEqualTo(original.getAdditionalParameters());
        assertThat(restored.getAttributes()).isEqualTo(original.getAttributes());
    }

    @Test
    @DisplayName("state 파라미터가 없으면 Redis를 조회하지 않고 null을 반환한다")
    void loadWithoutState_returnsNullWithoutLookup() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertThat(result).isNull();
        verify(redisUtil, never()).getData(anyString());
    }

    @Test
    @DisplayName("저장되지 않은 state로 조회하면 null을 반환한다 (콜백 위조/직접 접근 방어)")
    void loadUnknownState_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2Const.PARAM_STATE, STATE);
        given(redisUtil.getData(KEY)).willReturn(Optional.empty());

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("load는 비파괴적 조회(getData)를, remove는 파괴적 일회성 소비(getAndDelete)를 사용한다")
    void loadIsNonDestructive_removeIsDestructive() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2Const.PARAM_STATE, STATE);

        String storedJson = serialize(authorizationRequest());
        given(redisUtil.getData(KEY)).willReturn(Optional.of(storedJson));
        given(redisUtil.getAndDelete(KEY)).willReturn(Optional.of(storedJson));

        repository.loadAuthorizationRequest(request);
        verify(redisUtil).getData(KEY);
        verify(redisUtil, never()).getAndDelete(anyString());

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, new MockHttpServletResponse());

        assertThat(removed).isNotNull();
        verify(redisUtil).getAndDelete(KEY);
    }

    @Test
    @DisplayName("remove 시 state 파라미터가 없으면 null을 반환하고 삭제를 시도하지 않는다")
    void removeWithoutState_returnsNullWithoutDelete() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, new MockHttpServletResponse());

        assertThat(result).isNull();
        verify(redisUtil, never()).getAndDelete(anyString());
    }

    @Test
    @DisplayName("save에 null 요청이 들어오면 해당 state 키를 삭제한다 (Spring Security 정리 계약)")
    void saveNullRequest_deletesStateKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2Const.PARAM_STATE, STATE);

        repository.saveAuthorizationRequest(null, request, new MockHttpServletResponse());

        verify(redisUtil).deleteData(KEY);
        verify(redisUtil, never()).setDataExpire(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("save에 null 요청이고 state도 없으면 아무 동작도 하지 않는다")
    void saveNullRequestWithoutState_noOp() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        repository.saveAuthorizationRequest(null, request, new MockHttpServletResponse());

        verify(redisUtil, never()).deleteData(anyString());
        verify(redisUtil, never()).setDataExpire(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("저장된 값이 손상되어 역직렬화에 실패하면 CustomException으로 변환한다")
    void corruptedJson_throwsCustomException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2Const.PARAM_STATE, STATE);
        given(redisUtil.getData(KEY)).willReturn(Optional.of("{ broken json"));

        assertThatThrownBy(() -> repository.loadAuthorizationRequest(request))
                .isInstanceOf(CustomException.class);
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        repository.saveAuthorizationRequest(request, saveRequest, new MockHttpServletResponse());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(redisUtil).setDataExpire(eq(KEY), captor.capture(), eq(OAuth2Const.OAUTH2_STATE_TTL));
        org.mockito.Mockito.clearInvocations(redisUtil);
        return captor.getValue();
    }

}