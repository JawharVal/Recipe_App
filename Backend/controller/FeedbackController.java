
package com.example.demo.controller;

import com.example.demo.dto.FeedbackDTO;
import com.example.demo.model.Feedback;
import com.example.demo.model.FeedbackMessage;
import com.example.demo.model.User;
import com.example.demo.repositories.FeedbackMessageRepository;
import com.example.demo.repositories.FeedbackRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedbackMessageRepository feedbackMessageRepository;

    @Autowired
    private JavaMailSender mailSender;

    private static final String SUPPORT_EMAIL = "jawhar.maatouk1@gmail.com";

    @PostMapping
    public ResponseEntity<String> submitFeedback(@RequestBody FeedbackDTO feedbackDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Feedback feedback = new Feedback(user.getId(), feedbackDTO.getComment());
        feedbackRepository.save(feedback);

        sendFeedbackEmail(user, feedback);

        return new ResponseEntity<>("Feedback submitted successfully", HttpStatus.CREATED);
    }
    @GetMapping("/all")
    public ResponseEntity<List<FeedbackDTO>> getAllFeedbackDTO() {
        List<Feedback> feedbacks = feedbackRepository.findAll();
        List<FeedbackDTO> dtos = feedbacks.stream()
                .map(fb -> new FeedbackDTO(fb.getComment()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<FeedbackDTO>> getAllFeedback() {
        List<Feedback> feedbacks = feedbackRepository.findAll();
        List<FeedbackDTO> dtos = feedbacks.stream()
                .map(fb -> new FeedbackDTO(
                        fb.getId(),
                        fb.getUserId(),
                        fb.getComment(),
                        fb.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private void sendFeedbackEmail(User user, Feedback feedback) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(SUPPORT_EMAIL);
        message.setSubject("New Feedback from " + user.getUsername());
        message.setText("User ID: " + user.getId() + "\n" +
                "Username: " + user.getUsername() + "\n" +
                "Email: " + user.getEmail() + "\n\n" +
                "Feedback:\n" + feedback.getComment() + "\n\n" +
                "Submitted at: " + feedback.getCreatedAt());

        mailSender.send(message);
    }
    @GetMapping("/{feedbackId}/conversation")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getConversation(@PathVariable Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        // Return each message with id, sender, content, createdAt, etc.
        List<Map<String, Object>> messages = feedback.getMessages().stream()
                .sorted(Comparator.comparing(FeedbackMessage::getCreatedAt)) // sort by time
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", msg.getId());
                    map.put("sender", msg.getSender());
                    map.put("content", msg.getContent());
                    map.put("createdAt", msg.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{feedbackId}/conversation")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> postMessage(
            @PathVariable Long feedbackId,
            @RequestBody Map<String, String> body
    ) {
        String sender = body.getOrDefault("sender", "ADMIN"); // or "USER"
        String content = body.getOrDefault("content", "");

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        FeedbackMessage message = new FeedbackMessage(feedback, sender, content);
        feedbackMessageRepository.save(message);

        return ResponseEntity.ok("Message added to conversation");
    }

    @PutMapping("/{id}/response")
    public ResponseEntity<String> respondToFeedback(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String reply = payload.get("reply");
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));


        User user = userRepository.findById(feedback.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Response to your feedback");
        message.setText("Dear " + user.getUsername() + ",\n\n" +
                "Thank you for your feedback:\n" + feedback.getComment() + "\n\n" +
                "Our reply:\n" + reply + "\n\n" +
                "Best regards,\nThe LeGourmand Support Team");

        mailSender.send(message);

        return ResponseEntity.ok("Reply sent successfully");
    }

}
