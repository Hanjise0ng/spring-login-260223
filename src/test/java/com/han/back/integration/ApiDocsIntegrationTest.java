package com.han.back.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// API 문서 생성 통합 테스트 — springdoc + OperationCustomizer(ApiErrorCodeParser) + OpenApiCustomizer 연동 검증
//
// 이 클래스의 책임 (ApiErrorCodeParserTest / ApiErrorCodeWriterTest와의 경계):
//   - 실제 컨트롤러에 선언된 모든 @ApiErrorCode 참조가 해석 가능한가 (오타 시 문서 생성 자체가 실패)
//   - 선언된 에러 코드가 HTTP 상태별 예시로 실제 문서에 노출되는가
//
// 어노테이션 수집 규칙, 예시 병합 규칙 → 단위 테스트 담당
//
// @ApiErrorCode(constant = "...")는 문자열 참조라 컴파일러가 검증하지 못한다.
// 이 테스트가 그 검증을 런타임(문서 조회 시점)에서 빌드 시점으로 끌어온다.
@DisplayName("API 문서 생성 통합 테스트")
class ApiDocsIntegrationTest extends IntegrationTestBase {

    private static final String API_DOCS_URI = "/v3/api-docs";
    private static final String SIGN_UP_RESPONSES = "$.paths['/api/v1/auth/sign-up'].post.responses";

    @Test
    @DisplayName("모든 엔드포인트의 @ApiErrorCode 참조가 유효하여 문서가 정상 생성된다")
    void apiDocs_allDeclaredErrorCodesResolve() throws Exception {
        mockMvc.perform(get(API_DOCS_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").isNotEmpty());
    }

    @Test
    @DisplayName("같은 HTTP 상태로 선언된 에러 코드들은 하나의 응답 아래 예시로 병합된다")
    void apiDocs_sameStatusCodesAreMergedAsExamples() throws Exception {
        mockMvc.perform(get(API_DOCS_URI))
                .andExpect(jsonPath(SIGN_UP_RESPONSES + "['400'].content['application/json'].examples.*.value.code",
                        hasItems("AUTH_LOGIN_ID_CHECK_REQUIRED", "VERIFY_NOT_COMPLETED")))
                .andExpect(jsonPath(SIGN_UP_RESPONSES + "['409'].content['application/json'].examples.*.value.code",
                        hasItems("ACCOUNT_DUPLICATE_LOGIN_ID", "ACCOUNT_DUPLICATE_EMAIL")));
    }

    @Test
    @DisplayName("공통 서버 에러(INTERNAL_SERVER_ERROR, DB_ERROR)가 모든 엔드포인트의 500 응답에 노출된다")
    void apiDocs_commonServerErrorsAreAttached() throws Exception {
        mockMvc.perform(get(API_DOCS_URI))
                .andExpect(jsonPath(SIGN_UP_RESPONSES + "['500'].content['application/json'].examples.*.value.code",
                        hasItems("INTERNAL_SERVER_ERROR", "DB_ERROR")));
    }

}