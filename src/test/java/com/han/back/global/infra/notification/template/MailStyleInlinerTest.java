package com.han.back.global.infra.notification.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MailStyleInliner")
class MailStyleInlinerTest {

    @Nested
    @DisplayName("클래스 선택자 인라인화")
    class ClassSelector {

        @Test
        @DisplayName("클래스 선택자의 스타일이 해당 요소의 style 속성으로 병합된다")
        void classSelector_mergesStylesToElement() {
            String html = """
                    <html><head>
                    <style>.title { color:#222; font-size:22px; }</style>
                    </head><body>
                    <h2 class="title">제목</h2>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            assertThat(result).contains("color:#222");
            assertThat(result).contains("font-size:22px");
            assertThat(result).doesNotContain("<style>");
        }
    }

    @Nested
    @DisplayName("태그 선택자 인라인화")
    class TagSelector {

        @Test
        @DisplayName("태그 선택자도 인라인 스타일로 병합된다")
        void tagSelector_mergesStylesToElement() {
            String html = """
                    <html><head>
                    <style>p { color:#444; line-height:1.6; }</style>
                    </head><body>
                    <p>본문</p>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            assertThat(result).contains("color:#444");
            assertThat(result).contains("line-height:1.6");
        }
    }

    @Nested
    @DisplayName("기존 인라인 스타일 우선순위")
    class InlineStylePriority {

        @Test
        @DisplayName("기존 인라인 style 속성은 CSS 규칙보다 우선한다")
        void existingInlineStyle_takesPriorityOverCssRule() {
            String html = """
                    <html><head>
                    <style>.box { color:#000; font-size:12px; }</style>
                    </head><body>
                    <p class="box" style="color:#fff;">텍스트</p>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            assertThat(result).contains("color:#fff");
            assertThat(result).doesNotContain("color:#000");
            assertThat(result).contains("font-size:12px");
        }
    }

    @Nested
    @DisplayName("style 블록 제거")
    class StyleBlockRemoval {

        @Test
        @DisplayName("인라인화 후 style 블록이 최종 HTML 에서 제거된다")
        void afterProcess_styleBlockIsRemoved() {
            String html = """
                    <html><head>
                    <style>.foo { color:red; }</style>
                    </head><body>
                    <p class="foo">텍스트</p>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            assertThat(result).doesNotContain("<style>");
        }
    }

    @Nested
    @DisplayName("style 블록이 없는 경우")
    class NoStyleBlock {

        @Test
        @DisplayName("style 블록이 없으면 원본 HTML 을 그대로 반환한다")
        void noStyleBlock_returnsOriginalHtml() {
            String html = "<html><body><p style=\"color:red;\">텍스트</p></body></html>";

            String result = MailStyleInliner.process(html);

            assertThat(result).contains("color:red");
        }
    }

    @Nested
    @DisplayName("CSS 주석 처리")
    class CssComment {

        @Test
        @DisplayName("CSS 주석은 파싱 시 무시되고 나머지 스타일은 정상 병합된다")
        void cssComment_isIgnoredDuringParsing() {
            String html = """
                    <html><head>
                    <style>
                    /* 전체 리셋 */
                    .text { color:#333; }
                    </style>
                    </head><body>
                    <p class="text">텍스트</p>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            assertThat(result).contains("color:#333");
        }
    }

    @Nested
    @DisplayName("복수 선택자 처리")
    class MultipleSelectors {

        @Test
        @DisplayName("쉼표로 묶인 복수 선택자는 각 요소에 동일한 스타일이 병합된다")
        void multipleSelectors_mergesStylesToEachElement() {
            String html = """
                    <html><head>
                    <style>h1, h2 { font-weight:700; color:#111; }</style>
                    </head><body>
                    <h1>제목1</h1>
                    <h2>제목2</h2>
                    </body></html>
                    """;

            String result = MailStyleInliner.process(html);

            // h1, h2 모두 동일한 스타일이 적용되어야 한다
            assertThat(result.indexOf("font-weight:700")).isNotEqualTo(result.lastIndexOf("font-weight:700"));
        }
    }

}