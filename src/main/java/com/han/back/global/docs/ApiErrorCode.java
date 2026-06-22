package com.han.back.global.docs;

import com.han.back.global.response.ApiResponseStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCode {

    Class<? extends ApiResponseStatus> value();

    String constant();

    String summary() default "";

}