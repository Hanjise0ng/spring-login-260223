package com.han.back.global.security.util;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.entity.DeviceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.Set;

/**
 * HTTP 요청에서 디바이스 정보를 추출하는 유틸리티.

 * 분류 우선순위:
 * 1. X-Client-Type == APP → X-Device-Os 기반으로 APP_ANDROID / APP_IOS
 * 2. X-Client-Type == WEB 또는 헤더 없음 → User-Agent 파싱
 * a. device.family가 태블릿 키워드 포함 → WEB_TABLET
 * b. device.family == "Other" → WEB_DESKTOP (uap-core 기본값)
 * c. os.family가 모바일 OS → WEB_MOBILE
 * d. 해당 없음 → UNKNOWN

 * uap-java 라이브러리(BrowserScope 파생)를 사용하며,
 * regexes.yaml 기반 패턴 매칭으로 User-Agent를 구조화한다.
 */
@Slf4j
@Component
public class UserAgentUtil {

    private static final String FALLBACK_VALUE = "알 수 없음";
    private static final Set<String> TABLET_FAMILIES = Set.of("iPad", "Kindle", "Kindle Fire", "Nexus 10", "Galaxy Tab");
    private static final Set<String> MOBILE_OS_FAMILIES = Set.of("iOS", "Android");
    private static final String DESKTOP_DEVICE_FAMILY = "Other";

    private final Parser parser;

    public UserAgentUtil() {
        this.parser = new Parser();
    }

    /**
     * HTTP 요청에서 디바이스 정보를 추출한다.
     *
     * @param request HTTP 요청 (User-Agent, X-Client-Type, X-Device-Id, X-Device-Os 헤더 포함)
     * @return 파싱된 디바이스 정보
     */
    public DeviceInfoDto parse(HttpServletRequest request) {
        ClientType clientType = ClientType.fromHeader(request.getHeader(AuthConst.HEADER_CLIENT_TYPE));
        String loginIp = extractClientIp(request);
        String fingerprint = extractFingerprint(request, clientType);

        if (clientType == ClientType.APP) {
            return parseAppDevice(request, fingerprint, loginIp);
        }
        return parseWebDevice(request, fingerprint, loginIp);
    }

    private DeviceInfoDto parseAppDevice(HttpServletRequest request, String fingerprint, String loginIp) {
        String deviceOs = request.getHeader(AuthConst.HEADER_DEVICE_OS);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        DeviceType deviceType;
        String osName;

        if ("iOS".equalsIgnoreCase(deviceOs)) {
            deviceType = DeviceType.APP_IOS;
            osName = "iOS";
        } else if ("Android".equalsIgnoreCase(deviceOs)) {
            deviceType = DeviceType.APP_ANDROID;
            osName = "Android";
        } else {
            deviceType = DeviceType.UNKNOWN;
            osName = StringUtils.hasText(deviceOs) ? deviceOs : FALLBACK_VALUE;
        }

        // 앱 User-Agent에서 OS 버전 추출 시도
        if (StringUtils.hasText(userAgent)) {
            Client client = parser.parse(userAgent);
            if (StringUtils.hasText(client.os.major)) {
                osName = osName + " " + client.os.major;
                if (StringUtils.hasText(client.os.minor)) {
                    osName = osName + "." + client.os.minor;
                }
            }
        }

        // 앱의 browserName은 앱 이름으로 설정 (User-Agent에서 추출하거나 기본값)
        String browserName = extractAppName(userAgent);

        return DeviceInfoDto.of(deviceType, osName, browserName, fingerprint, loginIp);
    }

    private DeviceInfoDto parseWebDevice(HttpServletRequest request, String fingerprint, String loginIp) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        if (!StringUtils.hasText(userAgent)) {
            return DeviceInfoDto.of(DeviceType.UNKNOWN, FALLBACK_VALUE, FALLBACK_VALUE, fingerprint, loginIp);
        }

        Client client = parser.parse(userAgent);

        String osName = buildOsName(client);
        String browserName = buildBrowserName(client);
        DeviceType deviceType = classifyWebDeviceType(client);

        return DeviceInfoDto.of(deviceType, osName, browserName, fingerprint, loginIp);
    }

    private DeviceType classifyWebDeviceType(Client client) {
        String deviceFamily = client.device.family;

        // 태블릿 판별
        if (TABLET_FAMILIES.stream().anyMatch(tablet -> tablet.equalsIgnoreCase(deviceFamily))) {
            return DeviceType.WEB_TABLET;
        }

        // 데스크탑 판별 (uap-core에서 "Other"는 식별되지 않은 기기 = 데스크탑 브라우저)
        if (DESKTOP_DEVICE_FAMILY.equals(deviceFamily)) {
            return DeviceType.WEB_DESKTOP;
        }

        // 모바일 OS 판별
        if (MOBILE_OS_FAMILIES.contains(client.os.family)) {
            return DeviceType.WEB_MOBILE;
        }

        return DeviceType.UNKNOWN;
    }

    private String buildOsName(Client client) {
        if (!StringUtils.hasText(client.os.family)) {
            return FALLBACK_VALUE;
        }
        StringBuilder sb = new StringBuilder(client.os.family);
        if (StringUtils.hasText(client.os.major)) {
            sb.append(" ").append(client.os.major);
            if (StringUtils.hasText(client.os.minor)) {
                sb.append(".").append(client.os.minor);
            }
        }
        return sb.toString();
    }

    private String buildBrowserName(Client client) {
        if (!StringUtils.hasText(client.userAgent.family)) {
            return FALLBACK_VALUE;
        }
        return client.userAgent.family;
    }

    /**
     * 앱 이름 추출.
     * 앱 User-Agent가 "AppName/1.0.0 (...)" 형태인 경우 앱 이름 부분을 추출한다.
     * 파싱 실패 시 기본값 반환.
     */
    private String extractAppName(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return FALLBACK_VALUE;
        }
        // "MyApp/1.0.0 (Android 14; Pixel 7)" → "MyApp"
        int slashIndex = userAgent.indexOf('/');
        int spaceIndex = userAgent.indexOf(' ');
        if (slashIndex > 0 && (spaceIndex < 0 || slashIndex < spaceIndex)) {
            return userAgent.substring(0, slashIndex);
        }
        return FALLBACK_VALUE;
    }

    private String extractFingerprint(HttpServletRequest request, ClientType clientType) {
        if (clientType == ClientType.APP) {
            // 앱: X-Device-Id 헤더에서 추출
            String deviceId = request.getHeader(AuthConst.HEADER_DEVICE_ID);
            if (StringUtils.hasText(deviceId)) {
                return deviceId;
            }
        }

        // 웹: device_id 쿠키에서 추출
        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_DEVICE_ID_NAME)
                .orElse(UuidUtil.generateString());
    }

    private String extractClientIp(HttpServletRequest request) {
        // 프록시/로드밸런서 환경에서 실제 클라이언트 IP 추출
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

}