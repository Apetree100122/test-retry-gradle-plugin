/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.testretry.internal;

import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec;
import org.gradle.api.internal.tasks.testing.TestExecuter;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testretry.TestRetryTaskExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.gradle.testretry.internal.DefaultTestRetryTaskExtension.DEFAULT_FAIL_ON_PASSED_AFTER_RETRY;
import static org.gradle.testretry.internal.DefaultTestRetryTaskExtension.DEFAULT_MAX_FAILURES;
import static org.gradle.testretry.internal.DefaultTestRetryTaskExtension.DEFAULT_MAX_RETRIES;

public class RetryTestExecuter implements TestExecuter<JvmTestExecutionSpec> {

    private final TestRetryTaskExtension extension;
    private final TestExecuter<JvmTestExecutionSpec> delegate;
    private final Test testTask;
    private final RetryTestFrameworkGenerator retryTestFrameworkGenerator;

    public RetryTestExecuter(
        Test task,
        TestRetryTaskExtension extension,
        TestExecuter<JvmTestExecutionSpec> delegate,
        RetryTestFrameworkGenerator retryTestFrameworkGenerator
    ) {
        this.extension = extension;
        this.delegate = delegate;
        this.testTask = task;
        this.retryTestFrameworkGenerator = retryTestFrameworkGenerator;
    }

    @Override
    public void execute(JvmTestExecutionSpec spec, TestResultProcessor testResultProcessor) {
        // Can't rely on plugin defaults as feature is unavailable on Gradle 5.0
        @SuppressWarnings("UnstableApiUsage") int maxRetries = extension.getMaxRetries().getOrElse(DEFAULT_MAX_RETRIES);
        @SuppressWarnings("UnstableApiUsage") int maxFailures = extension.getMaxFailures().getOrElse(DEFAULT_MAX_FAILURES);
        @SuppressWarnings("UnstableApiUsage") boolean failOnPassedAfterRetry = extension.getFailOnPassedAfterRetry().getOrElse(DEFAULT_FAIL_ON_PASSED_AFTER_RETRY);

        if (maxRetries <= 0) {
            delegate.execute(spec, testResultProcessor);
            return;
        }

        RetryTestResultProcessor retryTestResultProcessor = new RetryTestResultProcessor(testResultProcessor, maxFailures);

        int retryCount = 0;
        JvmTestExecutionSpec testExecutionSpec = spec;

        while (true) {
            delegate.execute(testExecutionSpec, retryTestResultProcessor);
            RetryTestResultProcessor.RoundResult result = retryTestResultProcessor.getResult();

            if (!result.nonRetriedTests.isEmpty()) {
                failWithNonRetriedTests(result);
                return;
            }

            if (result.failedTests.isEmpty()) {
                if (retryCount > 0 && !failOnPassedAfterRetry) {
                    testTask.setIgnoreFailures(true);
                }
                break;
            } else if (result.lastRound) {
                break;
            } else {
                testExecutionSpec = createRetryJvmExecutionSpec(spec, testTask, result.failedTests);
                retryTestResultProcessor.reset(++retryCount == maxRetries);
            }
        }
    }

    private static void failWithNonRetriedTests(RetryTestResultProcessor.RoundResult result) {
        throw new IllegalStateException("org.gradle.test-retry was unable to retry the following test methods, which is unexpected. Please file a bug report at https://github.com/gradle/test-retry-gradle-plugin/issues" +
            result.nonRetriedTests.stream()
                .map(retry -> "   " + retry.getClassName() + "#" + retry.getName())
                .collect(Collectors.joining("\n", "\n", "\n")));
    }

    private JvmTestExecutionSpec createRetryJvmExecutionSpec(JvmTestExecutionSpec spec, Test testTask, List<TestName> retries) {
        return new JvmTestExecutionSpec(
            retryTestFrameworkGenerator.createRetryingTestFramework(spec, testTask, retries),
            spec.getClasspath(),
            spec.getCandidateClassFiles(),
            spec.isScanForTestClasses(),
            spec.getTestClassesDirs(),
            spec.getPath(),
            spec.getIdentityPath(),
            spec.getForkEvery(),
            spec.getJavaForkOptions(),
            spec.getMaxParallelForks(),
            spec.getPreviousFailedTestClasses()
        );
    }

    @Override
    public void stopNow() {
        delegate.stopNow();
    }
}
