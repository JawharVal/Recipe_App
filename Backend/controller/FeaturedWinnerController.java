package com.example.demo.controller;

import com.example.demo.model.FeaturedWinner;
import com.example.demo.repositories.FeaturedWinnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/challenges/featured-winners")
public class FeaturedWinnerController {

    @Autowired
    private FeaturedWinnerRepository featuredWinnerRepository;

    @GetMapping
    public ResponseEntity<List<FeaturedWinner>> getAllFeaturedWinners() {
        List<FeaturedWinner> winners = featuredWinnerRepository.findAll();
        return ResponseEntity.ok(winners);
    }

    @PostMapping
    public ResponseEntity<FeaturedWinner> createFeaturedWinner(@RequestBody FeaturedWinner winner) {
        FeaturedWinner createdWinner = featuredWinnerRepository.save(winner);
        return new ResponseEntity<>(createdWinner, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeaturedWinner> updateFeaturedWinner(@PathVariable Long id, @RequestBody FeaturedWinner updatedWinner) {
        Optional<FeaturedWinner> optionalWinner = featuredWinnerRepository.findById(id);
        if (!optionalWinner.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        FeaturedWinner winner = optionalWinner.get();
        winner.setUserEmail(updatedWinner.getUserEmail());
        winner.setUsername(updatedWinner.getUsername());
        winner.setTotalPoints(updatedWinner.getTotalPoints());
        featuredWinnerRepository.save(winner);
        return ResponseEntity.ok(winner);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeaturedWinner(@PathVariable Long id) {
        if (!featuredWinnerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        featuredWinnerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
