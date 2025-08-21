package com.remote.submission.controller;

import com.remote.submission.model.CodeSubmission;
import com.remote.submission.model.SubmissionWithoutCodeContent;
import com.remote.submission.service.CodeSubmissionService;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PostMapping("/execute/raw-code")
    public ResponseEntity<CodeSubmission> submitCode(@RequestBody CodeSubmission codeSubmission) {
        CodeSubmission codeSubmissionResult = codeService.handleCodeSubmission(codeSubmission);
        return ResponseEntity.ok(codeSubmissionResult);
    }

    @PostMapping(value = "/execute/file-code", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CodeSubmission> submitFile(@RequestPart("metadata") String metadata,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        CodeSubmission codeSubmissionResult = codeService.handleFileSubmission(metadata, file);
        return ResponseEntity.ok(codeSubmissionResult);
    }
}