package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.service.PaymentValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;
  private final PaymentValidatorService validatorService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService,
      PaymentValidatorService validatorService) {
    this.paymentGatewayService = paymentGatewayService;
    this.validatorService = validatorService;
  }

  /**
   * Retrieves a previously processed (Authorized, Declined or Pending) payment by its unique ID.
   *
   * @param paymentId UUID assigned when the payment was processed
   * @return HTTP 200 + stored payment details, or HTTP 404 if not found
   */
  @Operation(
      summary = "Retrieve a payment by ID",
      description = "Returns the details of a previously Authorized, Declined or Pending payment. "
          + "Returns HTTP 404 if no payment exists for the given ID.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found and returned"),
      @ApiResponse(responseCode = "404", description = "No payment found for the given ID")
  })

  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable(value = "id") UUID paymentId) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(paymentId), HttpStatus.OK);
  }

  /**
   * Submits a new payment for processing.
   *
   * Validates the card details, forwards the request to the acquiring bank simulator,
   * stores the result, and returns the payment outcome to the merchant.
   *
   * @param incomingPaymentRequest the merchant's payment request body
   * @return HTTP 200 with the processed payment (Authorized or Declined), or
   *         HTTP 400 if the request is invalid or the bank is unavailable (Rejected)
   */

  @Operation(
      summary     = "Submit a new payment",
      description = "Validates card details, contacts the acquiring bank, and returns Authorized or Declined. "
          + "Returns Rejected (HTTP 400) for invalid input or bank unavailability."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment processed (Authorized or Declined)"),
      @ApiResponse(responseCode = "400", description = "Payment rejected – invalid fields or bank error")
  })

  @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PostPaymentResponse> processPayment(@Valid @RequestBody PostPaymentRequest incomingPaymentRequest) {
      validatorService.validatePaymentRequest(incomingPaymentRequest);
      return new ResponseEntity<>(paymentGatewayService.processPayment(incomingPaymentRequest),
          HttpStatus.OK);
    }
}
