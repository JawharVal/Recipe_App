package com.example.demo.controller;

import com.example.demo.model.GlobalLeaderboardEntry;
import com.example.demo.repositories.GlobalLeaderboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/challenges/global-leaderboard")
public class GlobalLeaderboardController {

    @Autowired
    private GlobalLeaderboardRepository leaderboardRepository;

    @GetMapping
    public ResponseEntity<List<GlobalLeaderboardEntry>> getGlobalLeaderboard() {
        List<GlobalLeaderboardEntry> leaderboard = leaderboardRepository.findAll();
        return ResponseEntity.ok(leaderboard);
    }

    @PostMapping
    public ResponseEntity<GlobalLeaderboardEntry> createLeaderboardEntry(@RequestBody GlobalLeaderboardEntry entry) {
        GlobalLeaderboardEntry createdEntry = leaderboardRepository.save(entry);
        return new ResponseEntity<>(createdEntry, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalLeaderboardEntry> updateLeaderboardEntry(@PathVariable Long id, @RequestBody GlobalLeaderboardEntry updatedEntry) {
        Optional<GlobalLeaderboardEntry> optionalEntry = leaderboardRepository.findById(id);
        if (!optionalEntry.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        GlobalLeaderboardEntry entry = optionalEntry.get();
        entry.setUserEmail(updatedEntry.getUserEmail());
        entry.setUsername(updatedEntry.getUsername());
        entry.setTotalPoints(updatedEntry.getTotalPoints());
        leaderboardRepository.save(entry);
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaderboardEntry(@PathVariable Long id) {
        if (!leaderboardRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        leaderboardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
