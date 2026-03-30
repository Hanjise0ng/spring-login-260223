package com.han.back.global.security.util;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.entity.DeviceType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserAgentUtil")
class UserAgentUtilTest {

    private UserAgentUtil userAgentUtil;

    private static final String FIXED_FINGERPRINT = "fixed-device-fingerprint-001";
    private static final String DEVICE_ID_HEADER = "X-Device-Id";
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String DEVICE_OS_HEADER = "X-Device-Os";

    @BeforeEach
    void setUp() {
        userAgentUtil = new UserAgentUtil();
    }

    private void setDeviceIdCookie(MockHttpServletRequest request, String fingerprint) {
        request.setCookies(new Cookie(AuthConst.COOKIE_DEVICE_ID_NAME, fingerprint));
    }

    private MockHttpServletRequest webRequest(String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        setDeviceIdCookie(request, FIXED_FINGERPRINT);
        return request;
    }

    @Nested
    @DisplayName("웹 디바이스 분류")
    class WebDeviceClassification {

        @ParameterizedTest(name = "[{index}] {2} → {1}")
        @CsvSource({
                "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36', WEB_DESKTOP, Windows Chrome",
                "'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15', WEB_DESKTOP, macOS Safari",
                "'Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1', WEB_TABLET, iPad Safari",
                "'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1', WEB_MOBILE, iPhone Safari",
                "'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36', WEB_MOBILE, Android Chrome",
                "'UnknownCrawler/1.0 (+http://example.com/bot)', UNKNOWN, Unknown Bot",
        })
        @DisplayName("User-Agent 에 따라 올바른 DeviceType 을 분류한다")
        void classifiesWebDeviceType(String userAgent, DeviceType expected, String label) {
            DeviceInfoDto result = userAgentUtil.parse(webRequest(userAgent));

            assertThat(result.getDeviceType()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{index}] User-Agent 없거나 빈값 → UNKNOWN")
        @NullAndEmptySource
        @DisplayName("User-Agent 가 없거나 빈 값이면 UNKNOWN 을 반환한다")
        void noUserAgent_returnsUnknown(String userAgent) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (userAgent != null) {
                request.addHeader("User-Agent", userAgent);
            }
            setDeviceIdCookie(request, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceType()).isEqualTo(DeviceType.UNKNOWN);
            assertThat(result.getOsName()).isEqualTo("알 수 없음");
            assertThat(result.getBrowserName()).isEqualTo("알 수 없음");
        }
    }

    @Nested
    @DisplayName("앱 디바이스 분류")
    class AppDeviceClassification {

        @Test
        @DisplayName("X-Client-Type:APP + X-Device-Os:iOS → APP_IOS")
        void appIos_returnsAppIosType() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "iOS");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceType()).isEqualTo(DeviceType.APP_IOS);
            assertThat(result.getOsName()).startsWith("iOS");
        }

        @Test
        @DisplayName("X-Client-Type:APP + X-Device-Os:Android → APP_ANDROID")
        void appAndroid_returnsAppAndroidType() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "Android");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceType()).isEqualTo(DeviceType.APP_ANDROID);
            assertThat(result.getOsName()).startsWith("Android");
        }

        @Test
        @DisplayName("X-Client-Type:APP + X-Device-Os 없음 → UNKNOWN")
        void appWithoutDeviceOs_returnsUnknown() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceType()).isEqualTo(DeviceType.UNKNOWN);
        }

        @Test
        @DisplayName("앱 User-Agent 에서 OS 버전이 포함된다")
        void appWithUserAgent_includesOsVersion() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "iOS");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);
            request.addHeader("User-Agent", "MyApp/2.1.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X)");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getOsName()).startsWith("iOS");
            assertThat(result.getOsName()).contains("17");
        }

        @Test
        @DisplayName("앱 User-Agent 가 'AppName/버전' 형식이면 browserName 에 앱 이름이 들어간다")
        void appUserAgent_extractsAppName() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "Android");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);
            request.addHeader("User-Agent", "MyApp/2.1.0 (Android 14; Pixel 8)");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getBrowserName()).isEqualTo("MyApp");
        }

        @Test
        @DisplayName("X-Client-Type:APP + 알 수 없는 X-Device-Os → UNKNOWN 이고 osName 은 헤더값 그대로다")
        void appWithUnknownDeviceOs_returnsUnknownWithRawOsName() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "HarmonyOS");
            request.addHeader(DEVICE_ID_HEADER, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceType()).isEqualTo(DeviceType.UNKNOWN);
            assertThat(result.getOsName()).isEqualTo("HarmonyOS");
        }
    }

    @Nested
    @DisplayName("Fingerprint 추출")
    class FingerprintExtraction {

        @Test
        @DisplayName("앱 요청에서 X-Device-Id 헤더값을 fingerprint 로 사용한다")
        void app_usesXDeviceIdHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "iOS");
            request.addHeader(DEVICE_ID_HEADER, "my-device-uuid-001");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceFingerprint()).isEqualTo("my-device-uuid-001");
        }

        @Test
        @DisplayName("앱 요청에서 X-Device-Id 가 없으면 쿠키로 fallback 한다")
        void app_noDeviceIdHeader_fallbackToCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(CLIENT_TYPE_HEADER, "APP");
            request.addHeader(DEVICE_OS_HEADER, "iOS");
            setDeviceIdCookie(request, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceFingerprint()).isEqualTo(FIXED_FINGERPRINT);
        }

        @Test
        @DisplayName("웹 요청에서 device_id 쿠키값을 fingerprint 로 사용한다")
        void web_usesDeviceIdCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0.0.0 Safari/537.36");
            setDeviceIdCookie(request, FIXED_FINGERPRINT);

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getDeviceFingerprint()).isEqualTo(FIXED_FINGERPRINT);
        }

        @Test
        @DisplayName("쿠키도 없는 신규 웹 요청은 UUID 를 신규 생성한다")
        void web_noCookie_generatesNewUuid() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0.0.0 Safari/537.36");

            DeviceInfoDto result = userAgentUtil.parse(request);

            // UUID 포맷 검증: 8-4-4-4-12 (하이픈 포함 36자)
            assertThat(result.getDeviceFingerprint())
                    .isNotBlank()
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("쿠키 없는 신규 요청 두 번은 서로 다른 fingerprint 를 생성한다")
        void web_noCookie_eachCallGeneratesDifferentFingerprint() {
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request1.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request2.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");

            DeviceInfoDto result1 = userAgentUtil.parse(request1);
            DeviceInfoDto result2 = userAgentUtil.parse(request2);

            assertThat(result1.getDeviceFingerprint())
                    .isNotEqualTo(result2.getDeviceFingerprint());
        }
    }

    @Nested
    @DisplayName("클라이언트 IP 추출")
    class ClientIpExtraction {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP 를 사용한다")
        void xForwardedFor_usesFirstIp() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 172.16.0.1");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("X-Forwarded-For 첫 번째 IP 의 앞뒤 공백을 제거한다")
        void xForwardedFor_trimsWhitespace() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Forwarded-For", "  203.0.113.1  , 10.0.0.1");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("X-Forwarded-For 와 X-Real-IP 가 둘 다 있으면 X-Forwarded-For 를 우선한다")
        void xForwardedFor_takesPriorityOverXRealIp() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Forwarded-For", "203.0.113.1");
            request.addHeader("X-Real-IP", "203.0.113.99");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("X-Forwarded-For 가 unknown 이면 X-Real-IP 로 fallback 한다")
        void xForwardedFor_unknown_fallbackToXRealIp() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("X-Real-IP", "203.0.113.50");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("X-Forwarded-For 가 unknown 이고 X-Real-IP 도 없으면 remoteAddr 를 사용한다")
        void xForwardedFor_unknown_noXRealIp_fallbackToRemoteAddr() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Forwarded-For", "unknown");
            request.setRemoteAddr("192.168.0.200");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("192.168.0.200");
        }

        @Test
        @DisplayName("X-Real-IP 헤더가 있으면 해당 IP 를 사용한다")
        void xRealIp_usesHeaderValue() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.addHeader("X-Real-IP", "203.0.113.50");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("프록시 헤더 없으면 remoteAddr 를 사용한다")
        void noProxyHeader_usesRemoteAddr() {
            MockHttpServletRequest request = webRequest("Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
            request.setRemoteAddr("192.168.0.100");

            DeviceInfoDto result = userAgentUtil.parse(request);

            assertThat(result.getLoginIp()).isEqualTo("192.168.0.100");
        }
    }

}