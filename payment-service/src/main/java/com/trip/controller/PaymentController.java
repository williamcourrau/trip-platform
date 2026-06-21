package com.trip.controller;

import com.trip.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> handleStripeWebhook(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        String status = (String) payload.get("status");
        paymentService.handleStripeWebhook(sessionId, status);
        return ResponseEntity.ok().build();
    }
}
