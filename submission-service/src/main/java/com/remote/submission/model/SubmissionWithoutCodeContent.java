package com.remote.submission.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionWithoutCodeContent {
    private String sessionId;
    private String language;
}
