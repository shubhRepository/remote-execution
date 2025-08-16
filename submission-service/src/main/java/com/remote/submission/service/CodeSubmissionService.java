package com.remote.submission.service;

import com.remote.submission.config.Constants;
import com.remote.submission.model.CodeSubmission;
import com.remote.submission.repository.CodeSubmissionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class CodeSubmissionService {

    private final CodeSubmissionRepository codeSubmissionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public CodeSubmissionService(CodeSubmissionRepository codeSubmissionRepository, RabbitTemplate rabbitTemplate) {
        this.codeSubmissionRepository = codeSubmissionRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public CodeSubmission handleCodeSubmission(CodeSubmission codeSubmission) {
        codeSubmission.setId(UUID.randomUUID().toString());
        if (codeSubmission.getSessionId() == null) {
            codeSubmission.setSessionId(UUID.randomUUID().toString());
        }
        codeSubmission.setCodeContent(Base64.getEncoder().encodeToString(codeSubmission.getCodeContent().getBytes()));
        codeSubmissionRepository.save(codeSubmission);
        rabbitTemplate.convertAndSend(Constants.FILE_EXECUTION_EXCHANGE, Constants.FILE_EXECUTION_ROUTING_KEY,
                codeSubmission);
        return codeSubmission;
    }
}
