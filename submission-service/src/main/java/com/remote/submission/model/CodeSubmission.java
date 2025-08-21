package com.remote.submission.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_submissions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class CodeSubmission {
    @Id
    private String id;
    private String sessionId;
    @Lob
    @Column(name = "code_content")
    private String codeContent;
    private String language;
}