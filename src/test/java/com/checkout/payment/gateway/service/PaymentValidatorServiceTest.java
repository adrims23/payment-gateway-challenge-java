package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.checkout.payment.gateway.exception.PaymentRequestValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentValidatorServiceTest {

  @InjectMocks
  PaymentValidatorServiceImpl paymentValidatorService;

  @Test
  void givenNonNumericCardNumber_whenProcessPayment_thenRejectsWithReasonWithoutCallingBankAndPersists() {
    PostPaymentRequest requestWithAlphaCard = new PostPaymentRequest(
        "ABCD-NOT-NUMERIC", 4, 2027, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithAlphaCard))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("card_number must be a numeric string between 14 and 19 digits");
        });
  }

  @Test
  void givenCardNumberTooShort_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithShortCard = new PostPaymentRequest(
        "1234567890123",   // 13 digits
        4, 2027, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithShortCard))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("card_number must be a numeric string between 14 and 19 digits");
        });
  }

  @Test
  void givenCardNumberTooLong_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithLongCard = new PostPaymentRequest(
        "12345678901234567890",   // 20 digits
        4, 2027, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithLongCard))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("card_number must be a numeric string between 14 and 19 digits");
        });
  }

  @Test
  void givenExpiryMonthZero_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithMonthZero = new PostPaymentRequest(
        "2222405343248877", 0, 2027, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithMonthZero))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("expiry_month must be between 1 and 12");
        });
  }

  @Test
  void givenExpiryMonthThirteen_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithMonth13 = new PostPaymentRequest(
        "2222405343248877", 13, 2027, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithMonth13))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("expiry_month must be between 1 and 12");
        });
  }

  @Test
  void givenExpiredCardYear_whenProcessPayment_thenRejectsWithExpiryReasonAndPersists() {
    PostPaymentRequest requestWithExpiredCard = new PostPaymentRequest(
        "2222405343248877", 1, 2020, "GBP", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithExpiredCard))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("Card has expired");
        });
  }

  @Test
  void givenUnsupportedCurrency_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithBadCurrency = new PostPaymentRequest(
        "2222405343248877", 4, 2027, "INR", 100, "123");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithBadCurrency))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("currency must be one of GBP,USD,EUR");
        });
  }

  @Test
  void givenAlphaCvv_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithAlphaCvv = new PostPaymentRequest(
        "2222405343248877", 4, 2027, "GBP", 100, "AB3");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithAlphaCvv))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("cvv must be 3 or 4 numeric digits");
        });
  }

  @Test
  void givenCvvTooShort_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithShortCvv = new PostPaymentRequest(
        "2222405343248877", 4, 2027, "GBP", 100, "12");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithShortCvv))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("cvv must be 3 or 4 numeric digits");
        });
  }

  @Test
  void givenCvvTooLong_whenProcessPayment_thenRejectsAndPersists() {
    PostPaymentRequest requestWithLongCvv = new PostPaymentRequest(
        "2222405343248877", 4, 2027, "GBP", 100, "12345");

    assertThatThrownBy(() -> paymentValidatorService.validatePaymentRequest(requestWithLongCvv))
        .isInstanceOf(PaymentRequestValidationException.class)
        .satisfies(ex -> {
          assertThat(ex.getMessage()).contains("cvv must be 3 or 4 numeric digits");
        });
  }


}
