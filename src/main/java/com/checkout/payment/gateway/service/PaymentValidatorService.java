package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PostPaymentRequest;

public interface PaymentValidatorService {

  public void validatePaymentRequest(PostPaymentRequest paymentRequest);

}
