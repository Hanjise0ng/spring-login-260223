package com.han.back.domain.device.mapper;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.entity.DeviceConst;
import com.han.back.domain.device.entity.DeviceType;
import com.han.back.global.security.util.RawDeviceData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * 원시 디바이스 데이터를 분석하여 도메인 객체 {@link DeviceInfo} 조립
 *
 * <p>uap-java 라이브러리로 User-Agent 파싱
 * 디바이스 분류 규칙에 따라 {@link DeviceType} 결정</p>
 *
 * <p>의존 방향: {@code domain → global}</p>
 *
 * <p>분류 우선순위:</p>
 * <ol>
 *   <li>{@code app == true} → X-Device-Os 기반으로 {@code APP_ANDROID} / {@code APP_IOS}</li>
 *   <li>{@code app == false} → User-Agent 파싱:
 *     <ol>
 *       <li>device.family가 태블릿 키워드(iPad 등) → {@code WEB_TABLET}</li>
 *       <li>os.family가 iOS / Android → {@code WEB_MOBILE}</li>
 *       <li>device.family가 "Other" · null, 또는 os.family가 데스크탑 OS → {@code WEB_DESKTOP}</li>
 *       <li>그 외) → {@code UNKNOWN}</li>
 *     </ol>
 *   </li>
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
 *
 * @see RawDeviceData
 * @see DeviceInfo
 */
@Component
public class DeviceInfoMapper {

    private final Parser parser;

    public DeviceInfoMapper() {
        this.parser = new Parser();
    }

    public DeviceInfo create(RawDeviceData raw) {
        if (raw.isApp()) {
            return createAppDevice(raw);
        }
        return createWebDevice(raw);
    }

    private DeviceInfo createAppDevice(RawDeviceData raw) {
        DeviceType deviceType = classifyAppDeviceType(raw.getDeviceOs());
        String osName = buildAppOsName(raw);
        String browserName = extractAppName(raw.getUserAgent());

        return DeviceInfo.of(deviceType, osName, browserName, raw.getFingerprint(), raw.getLoginIp());
    }

    private DeviceType classifyAppDeviceType(String deviceOs) {
        if (DeviceConst.OS_IOS.equalsIgnoreCase(deviceOs)) return DeviceType.APP_IOS;
        if (DeviceConst.OS_ANDROID.equalsIgnoreCase(deviceOs)) return DeviceType.APP_ANDROID;
        return DeviceType.UNKNOWN;
    }

    private String buildAppOsName(RawDeviceData raw) {
        String baseName;
        if (DeviceConst.OS_IOS.equalsIgnoreCase(raw.getDeviceOs())) {
            baseName = DeviceConst.OS_IOS;
        } else if (DeviceConst.OS_ANDROID.equalsIgnoreCase(raw.getDeviceOs())) {
            baseName = DeviceConst.OS_ANDROID;
        } else {
            baseName = StringUtils.hasText(raw.getDeviceOs()) ? raw.getDeviceOs() : DeviceConst.FALLBACK_VALUE;
        }

        if (StringUtils.hasText(raw.getUserAgent())) {
            return appendOsVersion(baseName, parser.parse(raw.getUserAgent()));
        }
        return baseName;
    }

    private String extractAppName(String userAgent) {
        if (!StringUtils.hasText(userAgent)) return DeviceConst.FALLBACK_VALUE;

        int slashIndex = userAgent.indexOf('/');
        int spaceIndex = userAgent.indexOf(' ');
        if (slashIndex > 0 && (spaceIndex < 0 || slashIndex < spaceIndex)) {
            return userAgent.substring(0, slashIndex);
        }
        return DeviceConst.FALLBACK_VALUE;
    }

    private DeviceInfo createWebDevice(RawDeviceData raw) {
        if (!StringUtils.hasText(raw.getUserAgent())) {
            return DeviceInfo.of(DeviceType.UNKNOWN, DeviceConst.FALLBACK_VALUE, DeviceConst.FALLBACK_VALUE,
                    raw.getFingerprint(), raw.getLoginIp());
        }

        Client client = parser.parse(raw.getUserAgent());
        DeviceType deviceType = classifyWebDeviceType(client);
        String osName = buildWebOsName(client);
        String browserName = buildBrowserName(client);

        return DeviceInfo.of(deviceType, osName, browserName, raw.getFingerprint(), raw.getLoginIp());
    }

    private DeviceType classifyWebDeviceType(Client client) {
        String deviceFamily = client.device.family;

        if (isTablet(deviceFamily)) return DeviceType.WEB_TABLET;
        if (DeviceConst.MOBILE_OS_FAMILIES.contains(client.os.family)) return DeviceType.WEB_MOBILE;
        if (isDesktop(deviceFamily, client.os.family)) return DeviceType.WEB_DESKTOP;
        return DeviceType.UNKNOWN;
    }

    private boolean isTablet(String deviceFamily) {
        return StringUtils.hasText(deviceFamily)
                && DeviceConst.TABLET_FAMILIES.contains(deviceFamily.toLowerCase());
    }

    private boolean isDesktop(String deviceFamily, String osFamily) {
        return !StringUtils.hasText(deviceFamily)
                || DeviceConst.DESKTOP_DEVICE_FAMILY.equals(deviceFamily)
                || DeviceConst.DESKTOP_OS_FAMILIES.contains(osFamily);
    }

    private String buildWebOsName(Client client) {
        if (!StringUtils.hasText(client.os.family)) return DeviceConst.FALLBACK_VALUE;
        return appendOsVersion(client.os.family, client);
    }

    private String buildBrowserName(Client client) {
        if (!StringUtils.hasText(client.userAgent.family)) return DeviceConst.FALLBACK_VALUE;
        return client.userAgent.family;
    }

    private String appendOsVersion(String baseName, Client client) {
        if (!StringUtils.hasText(client.os.major)) return baseName;

        StringBuilder sb = new StringBuilder(baseName)
                .append(" ").append(client.os.major);
        if (StringUtils.hasText(client.os.minor)) {
            sb.append(".").append(client.os.minor);
        }
        return sb.toString();
    }

}