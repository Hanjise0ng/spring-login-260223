package com.han.back.global.response;

import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponseStatus 추상화 및 인프라 추상 의존")
class ResponseStatusAbstractionTest {

    private enum FakeResponseStatus implements ApiResponseStatus {
        FAKE_TEAPOT_CONSTANT("FAKE_TEAPOT_PUBLISHED", HttpStatus.I_AM_A_TEAPOT, "나는 주전자입니다.");

        private final String code;
        private final HttpStatus httpStatus;
        private final String message;

        FakeResponseStatus(String code, HttpStatus httpStatus, String message) {
            this.code = code;
            this.httpStatus = httpStatus;
            this.message = message;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public HttpStatus getHttpStatus() {
            return httpStatus;
        }
    }

    @Nested
    @DisplayName("ApiResponseStatus 인터페이스 계약")
    class InterfaceContract {

        @Test
        @DisplayName("getCode는 상수명이 아니라 명시된 code 필드를 반환한다")
        void getCodeReturnsExplicitField() {
            ApiResponseStatus status = FakeResponseStatus.FAKE_TEAPOT_CONSTANT;

            assertThat(status.getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(status.getCode()).isNotEqualTo(((Enum<?>) status).name());
        }

        @Test
        @DisplayName("getHttpStatusCode default 메서드는 getHttpStatus의 숫자 값을 반환한다")
        void httpStatusCodeDefaultMethod() {
            ApiResponseStatus status = FakeResponseStatus.FAKE_TEAPOT_CONSTANT;

            assertThat(status.getHttpStatusCode()).isEqualTo(418);
            assertThat(status.getHttpStatusCode()).isEqualTo(status.getHttpStatus().value());
        }

        @Test
        @DisplayName("도메인 enum은 명시된 code 필드를 발행한다")
        void domainEnumPublishesExplicitCode() {
            ApiResponseStatus status = ResponseStatus.SUCCESS;

            assertThat(status.getCode()).isEqualTo("SUCCESS");
            assertThat(status.getMessage()).isEqualTo("성공");
            assertThat(status.getHttpStatus()).isEqualTo(HttpStatus.OK);
            assertThat(status.getHttpStatusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("BaseResponse 래퍼의 추상 의존")
    class BaseResponseAbstraction {

        @Test
        @DisplayName("임의 ApiResponseStatus 구현체로 errorBody를 만들 수 있다 (구체 enum 비의존)")
        void errorBodyAcceptsAnyApiResponseStatus() {
            BaseResponse<Empty> body = BaseResponse.errorBody(FakeResponseStatus.FAKE_TEAPOT_CONSTANT);

            assertThat(body.getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(body.getMessage()).isEqualTo("나는 주전자입니다.");
            assertThat(body.getResult()).isInstanceOf(Empty.class);
        }

        @Test
        @DisplayName("customMessage를 주면 메시지가 대체되고 result는 빈 객체다")
        void errorBodyWithCustomMessage() {
            BaseResponse<Empty> body =
                    BaseResponse.errorBody(FakeResponseStatus.FAKE_TEAPOT_CONSTANT, "상세 디버그 메시지");

            assertThat(body.getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(body.getMessage()).isEqualTo("상세 디버그 메시지");
            assertThat(body.getResult()).isInstanceOf(Empty.class);
        }

        @Test
        @DisplayName("error 팩토리는 ApiResponseStatus의 httpStatus를 ResponseEntity 상태로 사용한다")
        void errorEntityUsesHttpStatus() {
            ResponseEntity<BaseResponse<Empty>> entity =
                    BaseResponse.error(FakeResponseStatus.FAKE_TEAPOT_CONSTANT);

            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
            assertThat(entity.getBody()).isNotNull();
            assertThat(entity.getBody().getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
        }
    }

    @Nested
    @DisplayName("예외 클래스의 추상 의존")
    class ExceptionAbstraction {

        @Test
        @DisplayName("CustomException은 임의 ApiResponseStatus를 보관한다")
        void customExceptionHoldsAnyApiResponseStatus() {
            CustomException ex = new CustomException(FakeResponseStatus.FAKE_TEAPOT_CONSTANT);

            ApiResponseStatus status = ex.getStatus();
            assertThat(status.getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(ex.getMessage()).isEqualTo("나는 주전자입니다.");
        }

        @Test
        @DisplayName("CustomException detailMessage 생성자는 메시지만 대체하고 status는 유지한다")
        void customExceptionWithDetailMessage() {
            CustomException ex =
                    new CustomException(FakeResponseStatus.FAKE_TEAPOT_CONSTANT, "구체 원인");

            assertThat(ex.getStatus().getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(ex.getMessage()).isEqualTo("구체 원인");
        }

        @Test
        @DisplayName("CustomAuthenticationException도 임의 ApiResponseStatus를 보관한다")
        void customAuthExceptionHoldsAnyApiResponseStatus() {
            CustomAuthenticationException ex =
                    new CustomAuthenticationException(FakeResponseStatus.FAKE_TEAPOT_CONSTANT);

            assertThat(ex.getStatus().getCode()).isEqualTo("FAKE_TEAPOT_PUBLISHED");
            assertThat(ex.getMessage()).isEqualTo("나는 주전자입니다.");
        }
    }

}