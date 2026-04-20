package com.checkout.payment.gateway.exception;

/**
 * Thrown when a payment is rejected before reaching the acquiring bank because
 * one or more request fields failed validation.
 */
public class PaymentRequestValidationException extends RuntimeException {

  public PaymentRequestValidationException(String rejectionReason) {
    super(rejectionReason);
  }
}