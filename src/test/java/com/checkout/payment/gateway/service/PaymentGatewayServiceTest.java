package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankRestClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankIntegrationException;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
public class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankRestClient bankRestClient;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  /** Valid GBP request, card ending in 7 (odd → bank will authorize). */
  private PostPaymentRequest validGbpPaymentWithOddEndingCard;

  /** Valid USD request, card ending in 2 (even → bank will decline). */
  private PostPaymentRequest validUsdPaymentWithEvenEndingCard;

  @BeforeEach
  void buildValidPaymentFixtures() {
    validGbpPaymentWithOddEndingCard = new PostPaymentRequest(
        "2222405343248877", 4, 2027, "GBP", 100, "123");

    validUsdPaymentWithEvenEndingCard = new PostPaymentRequest(
        "2222405343248112", 1, 2028, "USD", 60000, "456");
  }

  @Test
  void givenBankAuthorizes_whenProcessPayment_thenReturnsAuthorizedAndPersists(){
    when(bankRestClient.processPayment(any()))
        .thenReturn(bankResponse(true));

    PostPaymentResponse result =
        paymentGatewayService.processPayment(validGbpPaymentWithOddEndingCard);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(result.getCardNumberLastFour()).isEqualTo(8877);
    assertThat(result.getCurrency()).isEqualTo("GBP");
    assertThat(result.getAmount()).isEqualTo(100);
    assertThat(result.getExpiryMonth()).isEqualTo(4);
    assertThat(result.getExpiryYear()).isEqualTo(2027);
    assertThat(result.getId()).isNotNull();

    verify(paymentsRepository).add(result);

  }

  @Test
  void givenBankDeclines_whenProcessPayment_thenReturnsDeclinedAndPersists() {
    when(bankRestClient.processPayment(any()))
        .thenReturn(bankResponse(false));

    PostPaymentResponse result =
        paymentGatewayService.processPayment(validUsdPaymentWithEvenEndingCard);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(result.getCardNumberLastFour()).isEqualTo(8112);

    verify(paymentsRepository).add(result);
  }

  @Test
  void givenBankThrowsClientException_whenProcessPayment_thenThrowsCustomException() {
    doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(bankRestClient).processPayment(any());

    assertThrows(BankIntegrationException.class, () -> paymentGatewayService.processPayment(validUsdPaymentWithEvenEndingCard));

  }

  @Test
  void givenBankThrowsServerException_whenProcessPayment_thenThrowsCustomException() {
    doThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)).when(bankRestClient).processPayment(any());

    assertThrows(BankServiceUnavailableException.class, () -> paymentGatewayService.processPayment(validUsdPaymentWithEvenEndingCard));

  }

  @Test
  void givenBankThrowsNetworkException_whenProcessPayment_thenThrowsCustomException() {
    doThrow(new ResourceAccessException("Connect timeout")).when(bankRestClient).processPayment(any());

    assertThrows(BankServiceUnavailableException.class, () -> paymentGatewayService.processPayment(validUsdPaymentWithEvenEndingCard));

  }

  @Test
  void givenKnownPaymentId_whenGetById_thenReturnsStoredRecord() {
    UUID storedId = UUID.randomUUID();
    PostPaymentResponse storedPayment = new PostPaymentResponse();
    storedPayment.setId(storedId);
    storedPayment.setStatus(PaymentStatus.AUTHORIZED);

    when(paymentsRepository.get(storedId)).thenReturn(Optional.of(storedPayment));

    PostPaymentResponse retrieved = paymentGatewayService.getPaymentById(storedId);

    assertThat(retrieved).isSameAs(storedPayment);
    assertThat(retrieved.getId()).isEqualTo(storedId);
  }

  @Test
  void givenUnknownPaymentId_whenGetById_thenThrowsEventProcessingException() {
    UUID unknownId = UUID.randomUUID();
    when(paymentsRepository.get(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentGatewayService.getPaymentById(unknownId))
        .isInstanceOf(EventProcessingException.class);
  }

  private BankPaymentResponse bankResponse(boolean authorized) {
    return new BankPaymentResponse(authorized, authorized ?
        UUID.randomUUID().toString() : "");
  }

}
