package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.PostPaymentResponse;

/**
 * Thrown when bank simulator api throws any 5XX error
 */
public class BankIntegrationException extends RuntimeException{

  private final PostPaymentResponse paymentDetails;

  public BankIntegrationException(String message, PostPaymentResponse paymentDetails) {
    super(message);
    this.paymentDetails = paymentDetails;
  }

  public PostPaymentResponse getPaymentDetails() {
    return paymentDetails;
  }
}
