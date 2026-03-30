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
 * HTTP 요청에서 디바이스 정보 추출
 *
 * <p>X-Client-Type 헤더가 {@code APP} 이면 X-Device-Os 헤더로 분류 <br/>
 * 없거나 {@code WEB} 이면 User-Agent 를 파싱해 아래 순서로 분류 </p>
 *
 * <ol>
 *   <li>device.family 가 태블릿 키워드(iPad 등) → {@code WEB_TABLET}</li>
 *   <li>os.family 가 iOS / Android → {@code WEB_MOBILE}</li>
 *   <li>device.family 가 "Other" · null, 또는 os.family 가 데스크탑 OS → {@code WEB_DESKTOP}
 *       <br><small>※ macOS Safari 는 device.family = "Mac" 으로 오므로 os.family 로 보완</small></li>
 *   <li>그 외(Bot 등) → {@code UNKNOWN}</li>
 * </ol>
 *
 * <pre>
 * UA              device.family    os.family    결과
 * ─────────────────────────────────────────────────
 * Windows Chrome  Other            Windows      WEB_DESKTOP
 * macOS Safari    Mac              Mac OS X     WEB_DESKTOP
 * iPad            iPad             iOS          WEB_TABLET
 * iPhone          iPhone           iOS          WEB_MOBILE
 * Android Chrome  Pixel 8          Android      WEB_MOBILE
 * Bot             Spider           Other        UNKNOWN
 * </pre>
 */
@Slf4j
@Component
public class UserAgentUtil {

    private static final String FALLBACK_VALUE = "알 수 없음";
    private static final Set<String> TABLET_FAMILIES = Set.of("iPad", "Kindle", "Kindle Fire", "Nexus 10", "Galaxy Tab");
    private static final Set<String> MOBILE_OS_FAMILIES = Set.of("iOS", "Android");
    private static final Set<String> DESKTOP_OS_FAMILIES = Set.of("Windows", "Mac OS X", "Linux", "Chrome OS", "Ubuntu", "Fedora");
    private static final String DESKTOP_DEVICE_FAMILY = "Other";

    private final Parser parser;

    public UserAgentUtil() {
        this.parser = new Parser();
    }

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

        if (StringUtils.hasText(userAgent)) {
            Client client = parser.parse(userAgent);
            if (StringUtils.hasText(client.os.major)) {
                osName = osName + " " + client.os.major;
                if (StringUtils.hasText(client.os.minor)) {
                    osName = osName + "." + client.os.minor;
                }
            }
        }

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

        if (TABLET_FAMILIES.stream().anyMatch(tablet -> tablet.equalsIgnoreCase(deviceFamily))) {
            return DeviceType.WEB_TABLET;
        }

        if (MOBILE_OS_FAMILIES.contains(client.os.family)) {
            return DeviceType.WEB_MOBILE;
        }

        if (!StringUtils.hasText(deviceFamily)
                || DESKTOP_DEVICE_FAMILY.equals(deviceFamily)
                || DESKTOP_OS_FAMILIES.contains(client.os.family)) {
            return DeviceType.WEB_DESKTOP;
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
            String deviceId = request.getHeader(AuthConst.HEADER_DEVICE_ID);
            if (StringUtils.hasText(deviceId)) {
                return deviceId;
            }
        }

        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_DEVICE_ID_NAME)
                .orElse(UuidUtil.generateString());
    }

    private String extractClientIp(HttpServletRequest request) {
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