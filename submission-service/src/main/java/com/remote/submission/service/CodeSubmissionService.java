package com.remote.submission.service;

import com.remote.submission.model.CodeSubmission;
import com.remote.submission.repository.CodeSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class CodeSubmissionService {

    private final CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    public CodeSubmissionService(CodeSubmissionRepository codeSubmissionRepository) {
        this.codeSubmissionRepository = codeSubmissionRepository;
    }

    public CodeSubmission handleCodeSubmission(CodeSubmission codeSubmission) {
        codeSubmission.setId(UUID.randomUUID().toString());
        if (codeSubmission.getSessionId() == null) {
            codeSubmission.setSessionId(UUID.randomUUID().toString());
        }
        codeSubmission.setCodeContent(Base64.getEncoder().encodeToString(codeSubmission.getCodeContent().getBytes()));
        codeSubmissionRepository.save(codeSubmission);
        return codeSubmission;
    }
}
