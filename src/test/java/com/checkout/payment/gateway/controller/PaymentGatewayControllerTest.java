package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentRequestValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.service.PaymentValidatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentGatewayService paymentGatewayService;

    @MockBean
    private PaymentValidatorService paymentValidatorService;

    @Test
    void givenServiceReturnsAuthorizedPayment_whenPostPayments_thenHttp200WithAuthorizedBody()
            throws Exception {

        // Given
        PostPaymentRequest validGbpPaymentRequest = new PostPaymentRequest(
                "2222405343248877", 4, 2027, "GBP", 100, "123"
        );
        UUID assignedPaymentId = UUID.randomUUID();

        PostPaymentResponse authorizedPaymentResponse = new PostPaymentResponse();
        authorizedPaymentResponse.setId(assignedPaymentId);
        authorizedPaymentResponse.setStatus(PaymentStatus.AUTHORIZED);
        authorizedPaymentResponse.setCardNumberLastFour(8877);
        authorizedPaymentResponse.setExpiryMonth(4);
        authorizedPaymentResponse.setExpiryYear(2027);
        authorizedPaymentResponse.setCurrency("GBP");
        authorizedPaymentResponse.setAmount(100);

        doNothing().when(paymentValidatorService).validatePaymentRequest(any(PostPaymentRequest.class));
        when(paymentGatewayService.processPayment(any(PostPaymentRequest.class)))
                .thenReturn(authorizedPaymentResponse);

        // When / Then
        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGbpPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedPaymentId.toString()))
                .andExpect(jsonPath("$.status").value("Authorized"))
                .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
                .andExpect(jsonPath("$.expiryMonth").value(4))
                .andExpect(jsonPath("$.expiryYear").value(2027))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void givenServiceReturnsDeclinedPayment_whenPostPayments_thenHttp200WithDeclinedBody()
            throws Exception {

        // Given
        PostPaymentRequest validUsdPaymentRequest = new PostPaymentRequest(
                "2222405343248112", 1, 2028, "USD", 60000, "456"
        );

        UUID assignedPaymentId = UUID.randomUUID();
        PostPaymentResponse declinedPaymentResponse = new PostPaymentResponse();
        declinedPaymentResponse.setId(assignedPaymentId);
        declinedPaymentResponse.setStatus(PaymentStatus.DECLINED);
        declinedPaymentResponse.setCardNumberLastFour(8112);
        declinedPaymentResponse.setExpiryMonth(1);
        declinedPaymentResponse.setExpiryYear(9999);
        declinedPaymentResponse.setCurrency("USD");
        declinedPaymentResponse.setAmount(60000);

      doNothing().when(paymentValidatorService).validatePaymentRequest(any(PostPaymentRequest.class));
        when(paymentGatewayService.processPayment(any(PostPaymentRequest.class)))
                .thenReturn(declinedPaymentResponse);

        // When / Then
        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUsdPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Declined"))
                .andExpect(jsonPath("$.id").value(assignedPaymentId.toString()))
                .andExpect(jsonPath("$.cardNumberLastFour").value(8112))
                .andExpect(jsonPath("$.expiryMonth").value(1))
                .andExpect(jsonPath("$.expiryYear").value(9999))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(60000));
    }

  @Test
  void givenInvalidCardNumber_whenPostPayments_thenReturnHttp400()
      throws Exception {

    // Given
    PostPaymentRequest validUsdPaymentRequest = new PostPaymentRequest(
        "2222405A43248112", 1, 9999, "USD", 60000, "456"
    );

    doThrow(new PaymentRequestValidationException("Card is invalid")).when(paymentValidatorService).validatePaymentRequest(any(PostPaymentRequest.class));

    // When / Then
    mockMvc.perform(post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validUsdPaymentRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Card is invalid"));
  }

  @Test
  void givenExistingPaymentId_whenGetPaymentById_thenHttp200WithStoredPaymentDetails()
          throws Exception {

      // Given
      UUID existingAuthorizedPaymentId = UUID.randomUUID();

      PostPaymentResponse storedAuthorizedPayment = new PostPaymentResponse();
      storedAuthorizedPayment.setId(existingAuthorizedPaymentId);
      storedAuthorizedPayment.setStatus(PaymentStatus.AUTHORIZED);
      storedAuthorizedPayment.setCardNumberLastFour(8877);
      storedAuthorizedPayment.setExpiryMonth(4);
      storedAuthorizedPayment.setExpiryYear(2027);
      storedAuthorizedPayment.setCurrency("GBP");
      storedAuthorizedPayment.setAmount(100);

      when(paymentGatewayService.getPaymentById(existingAuthorizedPaymentId))
              .thenReturn(storedAuthorizedPayment);

      // When / Then
      mockMvc.perform(get("/payment/{paymentId}", existingAuthorizedPaymentId))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.id").value(existingAuthorizedPaymentId.toString()))
              .andExpect(jsonPath("$.status").value("Authorized"))
              .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
              .andExpect(jsonPath("$.expiryMonth").value(4))
              .andExpect(jsonPath("$.expiryYear").value(2027))
              .andExpect(jsonPath("$.currency").value("GBP"))
              .andExpect(jsonPath("$.amount").value(100));
  }

    @Test
    void givenNonExistentPaymentId_whenGetPaymentById_thenHttp404()
            throws Exception {

        // Given
        UUID nonExistentPaymentId = UUID.randomUUID();

        when(paymentGatewayService.getPaymentById(nonExistentPaymentId))
                .thenThrow(new EventProcessingException(nonExistentPaymentId.toString()));

        // When / Then
        mockMvc.perform(get("/payment/{paymentId}", nonExistentPaymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
