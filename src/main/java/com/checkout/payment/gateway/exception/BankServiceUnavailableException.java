package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.PostPaymentResponse;

/**
 * Thrown when a bank simulator api is not available
 */
public class BankServiceUnavailableException extends RuntimeException{

  private final PostPaymentResponse paymentDetails;

  public BankServiceUnavailableException(String message, PostPaymentResponse paymentDetails) {
    super(message);
    this.paymentDetails = paymentDetails;
  }

  public PostPaymentResponse getPaymentDetails() {
    return paymentDetails;
  }
}
