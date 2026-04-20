package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankRestClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankRestClient bankRestClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankRestClient bankRestClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankRestClient = bankRestClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }


  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.info("Processing payment request: {}", paymentRequest);
    BankPaymentRequest bankPaymentRequest = buildBankRequest(paymentRequest);
    try{
      BankPaymentResponse bankPaymentResponse = bankRestClient.processPayment(bankPaymentRequest);

      PaymentStatus resolvedStatus = bankPaymentResponse.isAuthorized()
          ? PaymentStatus.AUTHORIZED
          : PaymentStatus.DECLINED;

      PostPaymentResponse paymentResponse =
          buildPaymentResponse(paymentRequest, resolvedStatus);

      paymentsRepository.add(paymentResponse);
      LOG.info("Payment {} – id={}", resolvedStatus, paymentResponse.getId());
      return paymentResponse;

    } catch (HttpClientErrorException bankServerError) {

      LOG.error("Bank simulator returned client error {} – treating payment as pending",
          bankServerError.getStatusCode());
      PostPaymentResponse paymentResponse = getPostPaymentResponse(paymentRequest);
      throw new BankIntegrationException("Bank server error",paymentResponse);

    } catch (HttpServerErrorException bankServerError) {

      LOG.error("Bank simulator returned server error {} – treating payment as pending",
          bankServerError.getStatusCode());
      PostPaymentResponse paymentResponse = getPostPaymentResponse(paymentRequest);

      if (bankServerError.getStatusCode().value() == 503) {
        throw new BankServiceUnavailableException("Bank is temporarily unavailable", paymentResponse);
      }

      throw new BankIntegrationException("Bank server error", paymentResponse);

    } catch (RestClientException e) {
      PostPaymentResponse paymentResponse = getPostPaymentResponse(paymentRequest);

      throw new BankServiceUnavailableException("Network issue with bank", paymentResponse);
    }
  }

  private PostPaymentResponse getPostPaymentResponse(PostPaymentRequest paymentRequest) {
    PostPaymentResponse paymentResponse = buildPaymentResponse(paymentRequest, PaymentStatus.PENDING);
    paymentsRepository.add(paymentResponse);
    return paymentResponse;
  }

  /** prepare request for bank simulator api */
  private BankPaymentRequest buildBankRequest(PostPaymentRequest paymentRequest) {
    return new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv()
    );
  }

  /** prepare payment response */
  private PostPaymentResponse buildPaymentResponse(PostPaymentRequest paymentRequest,
      PaymentStatus resolvedStatus) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(resolvedStatus);
    response.setCardNumberLastFour(extractLastFourDigitsAsInt(paymentRequest.getCardNumber()));
    response.setExpiryMonth(paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest.getCurrency());
    response.setAmount(paymentRequest.getAmount());
    return response;
  }

  /** Parses the last 4 characters of the card number as an integer display value. */
  private int extractLastFourDigitsAsInt(String fullCardNumber) {
    String lastFour = fullCardNumber.substring(fullCardNumber.length() - 4);
    return Integer.parseInt(lastFour);
  }
}
