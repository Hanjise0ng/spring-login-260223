package com.han.back.global.docs;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiErrorCodeParser")
class ApiErrorCodeParserTest {

    private static final String JSON = "application/json";

    // 실제 컨트롤러 대신 어노테이션 조합만 갖는 테스트 전용 핸들러
    static class FakeHandler {

        void noAnnotation() {}

        @ApiErrorCode(value = AccountResponseStatus.class, constant = "ACCOUNT_DUPLICATE_LOGIN_ID", summary = "아이디 중복")
        void single() {}

        @ApiErrorCodes({
                @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_LOGIN_ID_CHECK_REQUIRED"),
                @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_NOT_COMPLETED")
        })
        void container() {}

        @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_NOT_EXIST_CONSTANT")
        void typo() {}
    }

    private static HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new FakeHandler(), FakeHandler.class.getDeclaredMethod(methodName));
    }

    private static Operation emptyOperation() {
        return new Operation().responses(new ApiResponses());
    }

    private static Map<String, Example> examplesOf(Operation operation, int httpStatus) {
        return operation.getResponses().get(String.valueOf(httpStatus))
                .getContent().get(JSON).getExamples();
    }

    @Nested
    @DisplayName("parse() — 어노테이션 수집")
    class Collect {

        @Test
        @DisplayName("@ApiErrorCode가 없으면 응답을 변경하지 않는다")
        void noAnnotation_leavesResponsesUntouched() throws Exception {
            Operation operation = emptyOperation();

            ApiErrorCodeParser.parse(operation, handler("noAnnotation"));

            assertThat(operation.getResponses()).isEmpty();
        }

        @Test
        @DisplayName("단일 @ApiErrorCode는 해당 HTTP 상태 응답에 summary 이름의 예시로 등록된다")
        void single_registersExampleUnderItsHttpStatus() throws Exception {
            Operation operation = emptyOperation();

            ApiErrorCodeParser.parse(operation, handler("single"));

            var status = AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID;
            assertThat(operation.getResponses()).containsOnlyKeys(String.valueOf(status.getHttpStatusCode()));
            assertThat(examplesOf(operation, status.getHttpStatusCode())).containsOnlyKeys("아이디 중복");
        }

        @Test
        @DisplayName("@ApiErrorCodes 컨테이너의 모든 코드가 등록되고, 같은 HTTP 상태끼리는 한 응답에 모인다")
        void container_registersAllCodesGroupedByHttpStatus() throws Exception {
            Operation operation = emptyOperation();

            ApiErrorCodeParser.parse(operation, handler("container"));

            // 두 코드 모두 400 → 400 응답 하나에 예시 2개
            assertThat(operation.getResponses()).containsOnlyKeys("400");
            assertThat(examplesOf(operation, 400)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("parse() — 응답 상수 해석")
    class Resolve {

        @Test
        @DisplayName("존재하지 않는 상수를 참조하면 enum 이름과 상수명을 담아 즉시 실패한다")
        void unknownConstant_failsFastWithLocation() {
            assertThatThrownBy(() -> ApiErrorCodeParser.parse(emptyOperation(), handler("typo")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AuthResponseStatus.AUTH_NOT_EXIST_CONSTANT")
                    .hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }

}
