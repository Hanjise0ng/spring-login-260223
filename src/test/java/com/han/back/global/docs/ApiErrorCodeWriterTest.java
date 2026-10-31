package com.han.back.global.docs;

import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.global.response.ResponseStatus;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiErrorCodeWriter")
class ApiErrorCodeWriterTest {

    private static final String JSON = "application/json";

    private ApiResponses responses;

    @BeforeEach
    void setUp() {
        responses = new ApiResponses();
    }

    private MediaType jsonOf(String httpStatus) {
        return responses.get(httpStatus).getContent().get(JSON);
    }

    @Nested
    @DisplayName("write() — 신규 응답 생성")
    class CreateResponse {

        @Test
        @DisplayName("응답이 없으면 HTTP 상태 문구를 설명으로, BaseResponse 스키마 참조를 가진 JSON 응답을 만든다")
        void absentStatus_createsJsonResponseWithReasonPhrase() {
            ApiErrorCodeWriter.write(responses, AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL, "이메일 중복");

            ApiResponse response = responses.get("409");
            assertThat(response.getDescription()).isEqualTo("Conflict");
            assertThat(jsonOf("409").getSchema().get$ref()).isEqualTo("#/components/schemas/BaseResponse");
        }

        @Test
        @DisplayName("예시 본문은 실제 에러 응답과 같은 code·message·result 구조다")
        @SuppressWarnings("unchecked")
        void example_mirrorsErrorResponseBody() {
            var status = AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL;

            ApiErrorCodeWriter.write(responses, status, "이메일 중복");

            Example example = jsonOf("409").getExamples().get("이메일 중복");
            assertThat(example.getSummary()).isEqualTo("이메일 중복");
            assertThat((Map<String, Object>) example.getValue())
                    .containsEntry("code", status.getCode())
                    .containsEntry("message", status.getMessage())
                    .containsEntry("result", Map.of());
        }

        @ParameterizedTest(name = "summary = [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("summary가 null·빈 문자열·공백이면 응답 상수의 message를 예시 이름과 요약으로 쓴다")
        void blankSummary_fallsBackToStatusMessage(String summary) {
            var status = ResponseStatus.DB_ERROR;

            ApiErrorCodeWriter.write(responses, status, summary);

            assertThat(jsonOf("500").getExamples()).containsOnlyKeys(status.getMessage());
            assertThat(jsonOf("500").getExamples().get(status.getMessage()).getSummary())
                    .isEqualTo(status.getMessage());
        }
    }

    @Nested
    @DisplayName("write() — 기존 응답 병합")
    class MergeResponse {

        @Test
        @DisplayName("같은 HTTP 상태로 다시 쓰면 응답을 새로 만들지 않고 예시만 추가한다")
        void sameStatus_appendsExample() {
            ApiErrorCodeWriter.write(responses, ResponseStatus.INTERNAL_SERVER_ERROR, null);
            ApiErrorCodeWriter.write(responses, ResponseStatus.DB_ERROR, null);

            assertThat(responses).containsOnlyKeys("500");
            assertThat(jsonOf("500").getExamples()).hasSize(2);
        }

        @Test
        @DisplayName("@ApiResponse로 직접 작성한 설명은 덮어쓰지 않는다")
        void existingDescription_isPreserved() {
            responses.addApiResponse("409", new ApiResponse().description("중복 리소스"));

            ApiErrorCodeWriter.write(responses, AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL, "이메일 중복");

            assertThat(responses.get("409").getDescription()).isEqualTo("중복 리소스");
            assertThat(jsonOf("409").getExamples()).containsOnlyKeys("이메일 중복");
        }

        @Test
        @DisplayName("기존 JSON 미디어 타입의 스키마는 교체하지 않는다")
        void existingMediaType_schemaIsPreserved() {
            ApiErrorCodeWriter.write(responses, ResponseStatus.INTERNAL_SERVER_ERROR, null);
            var schemaBefore = jsonOf("500").getSchema();

            ApiErrorCodeWriter.write(responses, ResponseStatus.DB_ERROR, null);

            assertThat(jsonOf("500").getSchema()).isSameAs(schemaBefore);
        }
    }

}
