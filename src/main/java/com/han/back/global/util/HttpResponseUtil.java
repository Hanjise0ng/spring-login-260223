package com.han.back.global.util;

import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.response.Empty;
import com.han.back.global.exception.CustomException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class HttpResponseUtil {

    private final ObjectMapper objectMapper;

    public void writeResponse(HttpServletResponse response, BaseResponseStatus status) {
        try {
            response.setStatus(status.getHttpStatusCode());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            BaseResponse<Empty> body = (status == BaseResponseStatus.SUCCESS)
                    ? BaseResponse.successBody()
                    : BaseResponse.errorBody(status);

            objectMapper.writeValue(response.getWriter(), body);
        } catch (IOException e) {
            throw new CustomException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}