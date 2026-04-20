package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankRestClient.class);
  private static final String BANK_PAYMENTS_PATH = "/payments";
  private final String bankSimulatorBaseUrl;
  private final RestTemplate restTemplate;


  public BankRestClient(RestTemplate restTemplate, @Value("${bank.api.base-url}") String bankSimulatorBaseUrl) {
    this.restTemplate = restTemplate;
    this.bankSimulatorBaseUrl = bankSimulatorBaseUrl;
  }

  @CircuitBreaker(name = "bankPaymentAuthorisation")
  @Retry(name = "bankPaymentAuthorisation")
  public BankPaymentResponse processPayment(BankPaymentRequest bankPaymentRequest){
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add("Content-Type","application/json");

      String bankEndpointUrl = bankSimulatorBaseUrl + BANK_PAYMENTS_PATH;
      LOG.debug("Forwarding payment authorisation request to bank simulator at {}", bankEndpointUrl);
      BankPaymentResponse bankPaymentResponse= restTemplate.postForObject(bankEndpointUrl, new HttpEntity<>(bankPaymentRequest,
          httpHeaders), BankPaymentResponse.class);

      LOG.debug("Bank simulator responded: authorized={}", bankPaymentResponse != null && bankPaymentResponse.isAuthorized());
      return bankPaymentResponse;

  }
}
