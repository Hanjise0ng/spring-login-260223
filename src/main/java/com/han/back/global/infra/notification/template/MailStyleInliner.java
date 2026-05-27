package com.han.back.global.infra.notification.template;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MailStyleInliner {

    private static final Pattern CSS_RULE_PATTERN = Pattern.compile("([^{]+)\\{([^}]+)}", Pattern.DOTALL);

    private MailStyleInliner() {}

    public static String process(String html) {
        Document doc = Jsoup.parse(html);

        Elements styleElements = doc.select("style");
        if (styleElements.isEmpty()) {
            return html;
        }

        String combinedCss = extractStyleBlocks(styleElements);
        Map<String, Map<String, String>> selectorStyleMap = buildSelectorStyleMap(combinedCss);
        mergeStylesToElements(doc, selectorStyleMap);
        styleElements.remove();

        return doc.outerHtml();
    }

    private static String extractStyleBlocks(Elements styleElements) {
        StringBuilder sb = new StringBuilder();
        for (Element styleEl : styleElements) {
            sb.append(styleEl.html()).append("\n");
        }
        return sb.toString();
    }

    private static Map<String, Map<String, String>> buildSelectorStyleMap(String css) {
        String cleaned = css.replaceAll("/\\*.*?\\*/", "");

        Map<String, Map<String, String>> selectorStyleMap = new LinkedHashMap<>();
        Matcher matcher = CSS_RULE_PATTERN.matcher(cleaned);

        while (matcher.find()) {
            String selectorBlock = matcher.group(1).trim();
            String declarations  = matcher.group(2).trim();

            if (selectorBlock.startsWith("@")) {
                continue;
            }

            Map<String, String> properties = toStyleProperties(declarations);

            for (String selector : selectorBlock.split(",")) {
                selector = selector.trim();
                selectorStyleMap.computeIfAbsent(selector, k -> new LinkedHashMap<>())
                        .putAll(properties);
            }
        }

        return selectorStyleMap;
    }

    private static Map<String, String> toStyleProperties(String declarations) {
        Map<String, String> props = new LinkedHashMap<>();
        for (String declaration : declarations.split(";")) {
            declaration = declaration.trim();
            if (declaration.isEmpty()) continue;

            int colonIdx = declaration.indexOf(':');
            if (colonIdx < 0) continue;

            String property = declaration.substring(0, colonIdx).trim().toLowerCase();
            String value     = declaration.substring(colonIdx + 1).trim();
            props.put(property, value);
        }
        return props;
    }

    private static void mergeStylesToElements(Document doc, Map<String, Map<String, String>> selectorStyleMap) {
        for (Map.Entry<String, Map<String, String>> entry : selectorStyleMap.entrySet()) {
            String selector = entry.getKey();
            Map<String, String> newProps = entry.getValue();

            try {
                Elements elements = doc.select(selector);
                for (Element el : elements) {
                    Map<String, String> existingProps = toStyleProperties(el.attr("style"));

                    Map<String, String> merged = new LinkedHashMap<>(newProps);
                    merged.putAll(existingProps);

                    el.attr("style", toStyleAttribute(merged));
                }
            } catch (Exception e) {
                log.debug("CSS 선택자 처리 중 건너뜀: selector={}, reason={}", selector, e.getMessage());
            }
        }
    }

    private static String toStyleAttribute(Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> prop : props.entrySet()) {
            sb.append(prop.getKey()).append(':').append(prop.getValue()).append(';');
        }
        return sb.toString();
    }

}