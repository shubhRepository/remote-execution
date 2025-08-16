package com.remote.submission.repository;

import com.remote.submission.model.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, UUID> {
    Optional<CodeSubmission> findById(String id);
}
