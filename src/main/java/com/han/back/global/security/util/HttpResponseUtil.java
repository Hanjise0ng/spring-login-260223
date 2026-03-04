package com.han.back.global.security.util;

import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class HttpResponseUtil {

    private HttpResponseUtil() {}

    public static void writeResponse(HttpServletResponse response, ObjectMapper objectMapper, BaseResponseStatus status) {
        try {
            response.setStatus(status.getHttpStatusCode());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Object body = (status == BaseResponseStatus.SUCCESS)
                    ? BaseResponse.success().getBody()
                    : BaseResponse.error(status).getBody();

            objectMapper.writeValue(response.getWriter(), body);

        } catch (IOException e) {
            throw new CustomException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}