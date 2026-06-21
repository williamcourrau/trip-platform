package com.trip.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.trip.config.RabbitMQConfig.*;

@Component
public class TripWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public TripWebSocketHandler(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        session.sendMessage(new TextMessage("Echo: " + payload));
    }

    @RabbitListener(queues = QUEUE_NOTIFY_NEW_TRIP)
    public void notifyNewTrip(TripCreatedEvent event) {
        broadcast(event);
    }

    @RabbitListener(queues = QUEUE_NOTIFY_DRIVER_ASSIGNMENT)
    public void notifyDriverAssignment(DriverAssignmentEvent event) {
        broadcast(event);
    }

    @RabbitListener(queues = QUEUE_NOTIFY_NO_DRIVERS_FOUND)
    public void notifyNoDriversFound(String tripId) {
        Map<String, String> msg = new HashMap<>();
        msg.put("type", "no_drivers_found");
        msg.put("tripId", tripId);
        broadcast(msg);
    }

    @RabbitListener(queues = QUEUE_NOTIFY_PAYMENT_STATUS)
    public void notifyPaymentStatus(PaymentSessionEvent event) {
        broadcast(event);
    }

    private void broadcast(Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (Exception e) {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            // logging would go here
        }
    }
}
