package com.cai.platform.repository;

import com.cai.platform.domain.EscalationCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationCaseRepository extends JpaRepository<EscalationCase, Long> {
}
