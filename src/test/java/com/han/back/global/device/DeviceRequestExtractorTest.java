package com.han.back.global.device;

import com.han.back.global.security.token.AuthConst;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceRequestExtractorTest {

    private final DeviceRequestExtractor deviceRequestExtractor = new DeviceRequestExtractor();

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("APP 클라이언트이고 DeviceId 헤더가 존재하면 해당 값을 지문으로 사용한다")
    void extractFingerprint_AppHeader() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(AuthConst.CLIENT_TYPE_APP);
        given(request.getHeader(AuthConst.HEADER_DEVICE_ID)).willReturn("app-device-uuid");
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.isNativeApp()).isTrue();
        assertThat(data.getFingerprint()).isEqualTo("app-device-uuid");
    }

    @Test
    @DisplayName("웹 클라이언트이거나 DeviceId 헤더가 없으면 쿠키에서 지문을 추출한다")
    void extractFingerprint_FromCookie() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn("WEB");
        Cookie[] cookies = {new Cookie(AuthConst.COOKIE_DEVICE_ID_NAME, "cookie-device-uuid")};
        given(request.getCookies()).willReturn(cookies);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.isNativeApp()).isFalse();
        assertThat(data.getFingerprint()).isEqualTo("cookie-device-uuid");
    }

    @Test
    @DisplayName("쿠키와 헤더 모두 없으면 새로운 UUID를 생성하여 반환한다")
    void extractFingerprint_GenerateNewUuid() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(null);
        given(request.getCookies()).willReturn(null);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.getFingerprint()).isNotBlank();
        assertThat(data.getFingerprint().length()).isGreaterThan(10);
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더에 여러 IP가 있을 경우 첫 번째 IP를 추출한다")
    void extractIp_XForwardedFor() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(null);
        given(request.getHeader("X-Forwarded-For")).willReturn("192.168.0.1, 10.0.0.1");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.getLoginIp()).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("X-Forwarded-For가 없고 X-Real-IP 헤더가 존재하면 이를 추출한다")
    void extractIp_XRealIp() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(null);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn("10.10.10.10");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.getLoginIp()).isEqualTo("10.10.10.10");
    }

    @Test
    @DisplayName("프록시 헤더들이 모두 없거나 unknown일 경우 RemoteAddr를 반환한다")
    void extractIp_RemoteAddr() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(null);
        given(request.getHeader("X-Forwarded-For")).willReturn("unknown");
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn("172.16.0.1");

        // when
        RawDeviceData data = deviceRequestExtractor.extract(request);

        // then
        assertThat(data.getLoginIp()).isEqualTo("172.16.0.1");
    }

}