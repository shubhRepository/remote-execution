package com.remote.submission.controller;

import com.remote.submission.model.CodeSubmission;
import com.remote.submission.service.CodeSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submit")
public class SubmissionController {

    private final CodeSubmissionService codeService;

    @Autowired
    public SubmissionController(CodeSubmissionService codeService) {
        this.codeService = codeService;
    }

    @PostMapping("/code")
    public ResponseEntity<CodeSubmission> submitCode(@RequestBody CodeSubmission codeSubmission) {
        CodeSubmission codeSubmissionResult = codeService.handleCodeSubmission(codeSubmission);
        return ResponseEntity.ok(codeSubmissionResult);
    }
}
