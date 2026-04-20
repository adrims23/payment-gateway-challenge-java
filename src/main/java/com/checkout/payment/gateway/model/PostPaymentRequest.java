package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  private String cardNumber;
  @JsonProperty("expiry_month")
  private int expiryMonth;
  @JsonProperty("expiry_year")
  private int expiryYear;
  private String currency;
  private int amount;
  private String cvv;

  public PostPaymentRequest(String cardNumber, int expiryMonth, int expiryYear, String currency,
      int amount, String cvv) {
    this.cardNumber = cardNumber;
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  public PostPaymentRequest() {
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    String maskedCardNumber = (cardNumber != null && cardNumber.length() >= 4)
        ? "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4)
        : "****";
    return "PostPaymentRequest{" +
        "cardNumber=" + maskedCardNumber +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
