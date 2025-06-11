package com.example.demo.repositories;

import com.example.demo.model.FeedbackMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    List<FeedbackMessage> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);
}
