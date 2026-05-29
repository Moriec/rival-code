package com.rivalcode.onlinejudge.service;

import com.rivalcode.contracts.submissions.model.TestCase;
import com.rivalcode.onlinejudge.model.ResolvedTestCase;

public interface TestCaseContentProvider {
    ResolvedTestCase resolve(TestCase testCase);
}
