package com.trip.repository;

import com.trip.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByTripId(String tripId);
    Optional<Payment> findBySessionId(String sessionId);
}
