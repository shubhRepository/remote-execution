package com.remote.submission.controller;

import com.remote.submission.model.CodeSubmission;
import com.remote.submission.service.CodeSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final CodeSubmissionService codeService;

    @Autowired
    public ExecutionController(CodeSubmissionService codeService) {
        this.codeService = codeService;
    }

    @GetMapping("/session")
    public ResponseEntity<String> getSessionId() {
        return ResponseEntity.ok(codeService.generateSessionId());
    }

    @PostMapping("/execute/code")
    public ResponseEntity<CodeSubmission> submitCode(@RequestBody CodeSubmission codeSubmission) {
        CodeSubmission codeSubmissionResult = codeService.handleCodeSubmission(codeSubmission);
        return ResponseEntity.ok(codeSubmissionResult);
    }
}
