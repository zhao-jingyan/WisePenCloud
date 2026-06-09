package com.oriole.wisepen.common.core.handler;

import com.oriole.wisepen.common.core.constant.CommonError;
import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice // 拦截所有 Controller
public class SpringExceptionHandler extends ResponseEntityExceptionHandler {

    private final Environment environment;

    public SpringExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        IResult result;
        if (status.is4xxClientError()){
            result = CommonError.REQUEST_ERROR;
            log.warn("request exception handled. status={} exception={}",
                    status.value(), ex.getClass().getSimpleName(), ex);
        } else {
            result = CommonError.INTERNAL_ERROR;
            log.error("spring internal error handled. status={} exception={}",
                    status.value(), ex.getClass().getSimpleName(), ex);
        }

        String uri = request.getDescription(false).replace("uri=", "");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("uri", uri);

        if (body instanceof ProblemDetail problemDetail){
            detail.put("title", problemDetail.getTitle());
            detail.put("detail", problemDetail.getDetail());
        }

        R<Map<String, Object>> customBody;
        // 判断当前是否处于 'dev' 或 'test' 环境
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev", "test"));
        if (isDev) {
            customBody = R.fail(result, ex.getMessage(), detail);
        } else {
            customBody = R.fail(result);
        }
        return new ResponseEntity<>(customBody, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        IResult result = CommonError.REQUEST_PARAM_INVALID;
        log.warn("request validation rejected. status={} exception={}",
                status.value(), ex.getClass().getSimpleName(), ex);

        ObjectError error = ex.getBindingResult().getAllErrors().getFirst();
        String constraint = error.getCode();
        if (constraint != null) {
            result = switch (constraint) {
                case "NotBlank", "NotNull", "NotEmpty" -> CommonError.REQUEST_PARAM_REQUIRED_MISSING;
                case "Min", "DecimalMin", "Positive", "PositiveOrZero" -> CommonError.REQUEST_PARAM_BELOW_LOWER_BOUND;
                case "Max", "DecimalMax", "Size" -> CommonError.REQUEST_PARAM_ABOVE_UPPER_BOUND;
                default -> CommonError.REQUEST_PARAM_INVALID;
            };
        }

        String uri = request.getDescription(false).replace("uri=", "");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("uri", uri);
        detail.put("object", error.getObjectName());
        detail.put("constraint", constraint);

        if (error instanceof FieldError fieldError) {
            detail.put("field", fieldError.getField());
        }

        R<Map<String, Object>> customBody = R.fail(result, error.getDefaultMessage(), detail);
        return new ResponseEntity<>(customBody, headers, status);
    }
}
