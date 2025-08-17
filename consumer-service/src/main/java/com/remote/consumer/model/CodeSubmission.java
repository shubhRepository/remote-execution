package com.remote.consumer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
    private String codeContent;
    private String language;
    private int userId;
}