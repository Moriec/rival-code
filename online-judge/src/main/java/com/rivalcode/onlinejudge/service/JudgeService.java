package com.rivalcode.onlinejudge.service;

import com.rivalcode.contracts.submissions.model.ComputingTask;
import com.rivalcode.contracts.submissionResult.model.JudgeResult;

public interface JudgeService {
    JudgeResult judge(ComputingTask task, int boxId);
}
