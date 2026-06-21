package com.trip.config;

import com.trip.handler.TripWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TripWebSocketHandler tripWebSocketHandler;

    public WebSocketConfig(TripWebSocketHandler tripWebSocketHandler) {
        this.tripWebSocketHandler = tripWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tripWebSocketHandler, "/ws/trips")
            .setAllowedOrigins("*");
    }
}
