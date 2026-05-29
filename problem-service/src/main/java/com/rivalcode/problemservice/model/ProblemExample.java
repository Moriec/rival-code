package com.rivalcode.problemservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "problem_examples", schema = "problem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemExample {

    @Id
    @Column(name = "example_id")
    private UUID exampleId;

    @Column(name = "problem_version_id", nullable = false)
    private UUID problemVersionId;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @PrePersist
    void prePersist() {
        if (exampleId == null) exampleId = UUID.randomUUID();
    }
}