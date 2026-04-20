package com.checkout.payment.gateway.model;

public class BankPaymentResponse {
  private final boolean authorized;
  private final String authorization_code;

  public BankPaymentResponse(boolean authorized, String authorization_code) {
    this.authorized = authorized;
    this.authorization_code = authorization_code;
  }

  public boolean isAuthorized() {
    return authorized;
  }

  public String getAuthorization_code() {
    return authorization_code;
  }
}
