package com.example.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSimulationRepository extends JpaRepository<SavedSimulation, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<SavedSimulation> findByNameIgnoreCase(String name);
}
