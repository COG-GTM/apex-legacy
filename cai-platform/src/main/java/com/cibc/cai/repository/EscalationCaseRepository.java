package com.cibc.cai.repository;

import com.cibc.cai.entity.EscalationCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationCaseRepository extends JpaRepository<EscalationCase, Long> {
}
