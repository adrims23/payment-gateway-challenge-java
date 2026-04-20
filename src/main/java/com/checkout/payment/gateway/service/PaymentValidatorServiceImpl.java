package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.PaymentRequestValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentValidatorServiceImpl implements PaymentValidatorService{

  private static final Logger logger = LoggerFactory.getLogger(PaymentValidatorServiceImpl.class);

  private static final Set<String> ACCEPTED_CURRENCY_CODES =
      Set.of("GBP", "USD", "EUR");

  private static final int CARD_NUMBER_MIN_DIGITS = 14;
  private static final int CARD_NUMBER_MAX_DIGITS = 19;
  private static final int CVV_MIN_DIGITS = 3;
  private static final int CVV_MAX_DIGITS = 4;
  private static final int EXPIRY_MONTH_MIN = 1;
  private static final int EXPIRY_MONTH_MAX = 12;

  @Override
  public void validatePaymentRequest(PostPaymentRequest incomingPaymentRequest) {
    List<String> validationFailureReasons = validateAllPaymentRequestFields(incomingPaymentRequest);

    if (!validationFailureReasons.isEmpty()) {
      logger.warn("Payment request failed validation. Reasons: {}", validationFailureReasons);
      throw new PaymentRequestValidationException(
          "Payment rejected – invalid fields: " + validationFailureReasons);
    }
  }

  /**
   * Runs all validation rules against the payment request.
   * Returns a list of error messages; an empty list means all rules passed.
   */
  private List<String> validateAllPaymentRequestFields(PostPaymentRequest paymentRequest) {
    List<String> validationErrors = new ArrayList<>();

    validateCardNumber(paymentRequest.getCardNumber(), validationErrors);
    validateExpiryMonth(paymentRequest.getExpiryMonth(), validationErrors);
    validateExpiryDateIsNotExpired(paymentRequest.getExpiryMonth(),
        paymentRequest.getExpiryYear(), validationErrors);
    validateCurrencyCode(paymentRequest.getCurrency(), validationErrors);
    validateCvv(paymentRequest.getCvv(), validationErrors);

    return validationErrors;
  }

  /** Card number must be a numeric-only string, 14–19 digits long. */
  private void validateCardNumber(String cardNumber, List<String> errors) {
    if (cardNumber == null
        || !cardNumber.matches("\\d+")
        || cardNumber.length() < CARD_NUMBER_MIN_DIGITS
        || cardNumber.length() > CARD_NUMBER_MAX_DIGITS) {
      errors.add(String.format(
          "card_number must be a numeric string between %d and %d digits (received: '%s')",
          CARD_NUMBER_MIN_DIGITS, CARD_NUMBER_MAX_DIGITS,
          cardNumber == null ? "null" : cardNumber));
    }
  }

  /** Expiry month must be an integer in the range 1–12. */
  private void validateExpiryMonth(int expiryMonth, List<String> errors) {
    if (expiryMonth < EXPIRY_MONTH_MIN || expiryMonth > EXPIRY_MONTH_MAX) {
      errors.add(String.format(
          "expiry_month must be between %d and %d (received: %d)",
          EXPIRY_MONTH_MIN, EXPIRY_MONTH_MAX, expiryMonth));
    }
  }

  /** The combination of expiry month + year must not be in the past. */
  private void validateExpiryDateIsNotExpired(int expiryMonth, int expiryYear,
      List<String> errors) {
    int safeMonth = Math.max(EXPIRY_MONTH_MIN, Math.min(EXPIRY_MONTH_MAX, expiryMonth));
    YearMonth cardExpiry   = YearMonth.of(expiryYear, safeMonth);
    YearMonth currentMonth = YearMonth.now();
    if (cardExpiry.isBefore(currentMonth)) {
      errors.add(String.format(
          "Card has expired – expiry %02d/%d is in the past", expiryMonth, expiryYear));
    }
  }

  /** Currency must be exactly 3 characters and should be one of these currency codes (USD, EUR, GBP). */
  private void validateCurrencyCode(String currency, List<String> errors) {
    if (currency == null || currency.length() != 3
        || !ACCEPTED_CURRENCY_CODES.contains(currency)) {
      errors.add(String.format(
          "currency must be one of GBP,USD,EUR but (received: '%s')", currency));
    }
  }

  /** CVV must be a purely numeric string of exactly 3 or 4 characters. */
  private void validateCvv(String cvv, List<String> errors) {
    if (cvv == null || !cvv.matches("\\d+")
        || cvv.length() < CVV_MIN_DIGITS || cvv.length() > CVV_MAX_DIGITS) {
      errors.add(String.format(
          "cvv must be %d or %d numeric digits (received: '%s')",
          CVV_MIN_DIGITS, CVV_MAX_DIGITS, cvv == null ? "null" : cvv));
    }
  }

}
