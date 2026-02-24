package com.example.heartbit.repository;

import com.example.heartbit.domain.Bots;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotsRepository extends JpaRepository<Bots, Long> {
}
