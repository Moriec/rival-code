package com.rivalcode.contracts.submissions.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    private TestCaseFileRef inputFile;
    private TestCaseFileRef expectedOutputFile;
}
