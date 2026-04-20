package com.checkout.payment.gateway.integration;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
    "bank.api.base-url=http://localhost:8080",
    "resilience4j.retry.instances.bankPaymentAuthorisation.wait-duration=10ms"
})
@AutoConfigureMockMvc
class PaymentGatewayIntegrationTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private WireMockServer wireMockServer;

  @BeforeEach
  void setup() {
    wireMockServer = new WireMockServer(8080);
    wireMockServer.start();
    configureFor("localhost", 8080);
  }
  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  void givenBankAuthorizes_whenPostPayment_thenHttp200WithAuthorizedStatus() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber( "2222405343248877");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(2028);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);

    stubBankResponse(200, true, UUID.randomUUID().toString());

    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2028))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(50));
  }

  @Test
  void givenBankDeclines_whenPostPayment_thenHttp200WithDeclinedStatus() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248878");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(2028);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);
    stubBankResponse(200, false, "");
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8878))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2028))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(50));
  }

  @Test
  void givenBankReturns503_whenPostValidPayment_thenRetriedThreeTimesAndHttp503Rejected() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248870");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);

    wireMockServer.stubFor(post(urlEqualTo("/payments"))
        .willReturn(aResponse().withStatus(503)));

    String postResponseJson = mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Bank is temporarily unavailable"))
        .andReturn().getResponse().getContentAsString();

    // Retry must fire max-attempts (3) times before giving up
    wireMockServer.verify(3, postRequestedFor(urlEqualTo("/payments")));

    String pendingPaymentId = OBJECT_MAPPER.readTree(postResponseJson).get("id").asText();
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + pendingPaymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Pending"))
        .andExpect(jsonPath("$.id").value(pendingPaymentId));

  }

  @Test
  void givenCardNumberLengthLessThan14_whenPostPayment_thenHttp400RejectedWithReasonAndBankNotCalled() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment rejected – invalid fields: [card_number must be a numeric string between 14 and 19 digits (received: '2222405')]"));

    wireMockServer.verify(0, postRequestedFor(urlEqualTo("/payments")));
  }

  @Test
  void rejectPaymentRequestIfExpiryMonthIsGreaterTha12() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248878");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(13);
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment rejected – invalid fields: [expiry_month must be between 1 and 12 (received: 13)]"));
  }

  @Test
  void givenUnsupportedCurrency_whenPostPayment_thenHttp400RejectedWithCurrencyReason() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248878");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("INR");
    paymentRequest.setExpiryMonth(12);
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment rejected – invalid fields: [currency must be one of GBP,USD,EUR but (received: 'INR')]"));
  }

  @Test
  void givenExpiredCard_whenPostPayment_thenHttp400RejectedWithExpiryReason() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248878");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(2025);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment rejected – invalid fields: [Card has expired – expiry 12/2025 is in the past]"));
  }

  @Test
  void givenCvvLengthLessThan3_whenPostPayment_thenHttp400RejectedWithCvvReason() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber("2222405343248878");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("78");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);
    mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment rejected – invalid fields: [cvv must be 3 or 4 numeric digits (received: '78')]"));
  }

  @Test
  void givenBankAuthorizes_whenPostPayment_thenHttp200WithAuthorizedStatusAndThenRetrievePayment() throws Exception {
    PostPaymentRequest paymentRequest = new PostPaymentRequest();
    paymentRequest.setCardNumber( "2222405343248877");
    paymentRequest.setAmount(50);
    paymentRequest.setExpiryYear(9999);
    paymentRequest.setCvv("789");
    paymentRequest.setCurrency("USD");
    paymentRequest.setExpiryMonth(12);

    stubBankResponse(200, true, UUID.randomUUID().toString());

    String postPaymentResponseJson = mvc.perform(MockMvcRequestBuilders.post("/payment").content(OBJECT_MAPPER.writeValueAsString(paymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(9999))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(50))
        .andReturn().getResponse().getContentAsString();

    String authorizedPaymentId = OBJECT_MAPPER.readTree(postPaymentResponseJson).get("id").asText();
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + authorizedPaymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.id").value(authorizedPaymentId))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(9999))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(50));
  }

  private void stubBankResponse(int httpStatus, boolean authorized, String authorizationCode) {
    String bankResponseBody = String.format(
        "{\"authorized\": %b, \"authorization_code\": \"%s\"}", authorized, authorizationCode);
    wireMockServer.stubFor(post(urlEqualTo("/payments"))
        .willReturn(aResponse()
            .withStatus(httpStatus)
            .withHeader("Content-Type", "application/json")
            .withBody(bankResponseBody)));
  }

}
