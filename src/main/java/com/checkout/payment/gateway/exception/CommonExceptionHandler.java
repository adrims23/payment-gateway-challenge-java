package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PaymentRequestValidationException.class)
  public ResponseEntity<ErrorResponse> handleRequestValidationException(PaymentRequestValidationException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BankServiceUnavailableException.class)
  public ResponseEntity<?> handleUnavailable(BankServiceUnavailableException ex) {

    return ResponseEntity.status(503).body(Map.of(
        "id",ex.getPaymentDetails().getId(),
        "error", "PAYMENT_PROVIDER_UNAVAILABLE",
        "message", ex.getMessage()
    ));
  }

  @ExceptionHandler(BankIntegrationException.class)
  public ResponseEntity<?> handleIntegration(BankIntegrationException ex) {

    return ResponseEntity.status(502).body(Map.of(
        "id",ex.getPaymentDetails().getId(),
        "error", "BANK_INTEGRATION_ERROR",
        "message", ex.getMessage()
    ));
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<?> circuitOpenException(Exception ex) {

    return ResponseEntity.status(503).body(Map.of(
        "error", "PAYMENT_PROVIDER_UNAVAILABLE",
        "message", "Bank is temporarily unavailable. Please try again later"
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> fallback(Exception ex) {

    return ResponseEntity.status(500).body(Map.of(
        "error", "INTERNAL_ERROR",
        "message", "Unexpected error occurred"
    ));
  }
}
