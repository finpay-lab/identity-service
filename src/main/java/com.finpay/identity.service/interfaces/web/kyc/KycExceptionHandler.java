package com.finpay.identity.service.interfaces.web.kyc;

import com.finpay.common.web.error.ErrorCode;
import com.finpay.common.web.error.ProblemDetail;
import com.finpay.common.web.filter.CorrelationIdFilter;
import com.finpay.identity.service.domain.kyc.DocumentExtractionException;
import com.finpay.identity.service.domain.kyc.InvalidKycTransitionException;
import com.finpay.identity.service.domain.kyc.KycValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps KYC domain exceptions to RFC-9457 problem responses (common-web). Never
 * leaks internal exception text; extraction failures never include document PII.
 */
@RestControllerAdvice
public class KycExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(KycExceptionHandler.class);

    @ExceptionHandler(KycValidationException.class)
    ResponseEntity<ProblemDetail> handleValidation(KycValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(InvalidKycTransitionException.class)
    ResponseEntity<ProblemDetail> handleInvalidTransition(InvalidKycTransitionException ex) {
        return problem(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION.name(), ex.getMessage());
    }

    @ExceptionHandler(DocumentExtractionException.class)
    ResponseEntity<ProblemDetail> handleExtractionFailure(DocumentExtractionException ex) {
        log.error("KYC document extraction failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "AI_EXTRACTION_FAILED",
                "Document extraction failed; the document has not been accepted");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String code, String message) {
        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return ResponseEntity.status(status)
                .body(new ProblemDetail(status.value(), code, message, traceId, Map.of()));
    }
}
