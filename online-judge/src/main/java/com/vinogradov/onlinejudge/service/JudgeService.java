package com.vinogradov.onlinejudge.service;

import com.vinogradov.contracts.submissions.model.ComputingTask;
import com.vinogradov.contracts.submissionResult.model.JudgeResult;

public interface JudgeService {
    JudgeResult judge(ComputingTask task, int boxId);
}
