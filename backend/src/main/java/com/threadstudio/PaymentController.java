package com.threadstudio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository users;

    public PaymentController(
            PaymentService paymentService,
            UserRepository users) {
        this.paymentService = paymentService;
        this.users = users;
    }

    record CreatePaymentRequest(
            @NotBlank String shippingAddress,
            @NotEmpty List<Long> productIds) {}

    record VerifyPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature) {}

    private AppUser current(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new RuntimeException("Account not found"));
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication auth) {

        try {
            Map<String, Object> result =
                    paymentService.createPaymentOrder(
                            current(auth),
                            request.shippingAddress(),
                            request.productIds()
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        }  catch(Exception e) {
    e.printStackTrace();
    return ResponseEntity.internalServerError()
        .body(Map.of("message", e.getMessage()));
}
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication auth) {

        try {
            Map<String, Object> result =
                    paymentService.verifyPayment(
                            current(auth),
                            request.razorpayOrderId(),
                            request.razorpayPaymentId(),
                            request.razorpaySignature()
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch(Exception e) {
    e.printStackTrace();

    return ResponseEntity.internalServerError()
        .body(Map.of(
            "message", "Payment verification failed",
            "error", String.valueOf(e.getMessage())
        ));
}
    }
}