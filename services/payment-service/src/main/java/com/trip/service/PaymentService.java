package com.trip.service;

import com.trip.dto.PaymentSessionEvent;
import com.trip.dto.PaymentStatusEvent;
import com.trip.event.PaymentEvent;
import com.trip.model.Payment;
import com.trip.repository.PaymentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.trip.config.RabbitMQConfig.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentService(PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = QUEUE_CREATE_PAYMENT_SESSION)
    public void handleCreatePaymentSession(String tripId) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setTripId(tripId);
        payment.setAmount(25.00);
        payment.setCurrency("usd");
        payment.setStatus(Payment.PaymentStatus.PENDING);

        String sessionId = UUID.randomUUID().toString();
        payment.setSessionId(sessionId);
        payment.setPaymentUrl("https://checkout.stripe.com/pay/" + sessionId);

        paymentRepository.save(payment);

        PaymentSessionEvent event = new PaymentSessionEvent(
            tripId, sessionId, payment.getPaymentUrl(), payment.getAmount()
        );

        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PaymentEvent.SESSION_CREATED.getRoutingKey(), event);
    }

    public void handleStripeWebhook(String sessionId, String status) {
        String paymentStatus = "complete".equalsIgnoreCase(status) ? "SUCCESS" : "FAILED";
        PaymentEvent eventType = "SUCCESS".equals(paymentStatus) ? PaymentEvent.SUCCESS : PaymentEvent.FAILED;

        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(() ->
            new RuntimeException("Payment not found for session: " + sessionId));
        payment.setStatus(Payment.PaymentStatus.valueOf(paymentStatus));
        payment.setUpdatedAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        PaymentStatusEvent event = new PaymentStatusEvent(payment.getTripId(), sessionId, paymentStatus);
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, eventType.getRoutingKey(), event);
    }
}
