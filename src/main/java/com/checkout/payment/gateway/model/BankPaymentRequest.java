package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankPaymentRequest {
  @JsonProperty("card_number")
  private String card_number;
  @JsonProperty("expiry_date")
  private String expiry_date;
  private String currency;
  private int amount;
  private String cvv;

  public BankPaymentRequest(String card_number, String expiry_date, String currency, int amount,
      String cvv) {
    this.card_number = card_number;
    this.expiry_date = expiry_date;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  public String getCard_number() {
    return card_number;
  }

  public void setCard_number(String card_number) {
    this.card_number = card_number;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getExpiry_date() {
    return expiry_date;
  }

  public void setExpiry_date(String expiry_date) {
    this.expiry_date = expiry_date;
  }
}
