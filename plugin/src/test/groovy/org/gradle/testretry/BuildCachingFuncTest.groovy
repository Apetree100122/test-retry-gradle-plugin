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
package org.gradle.testretry

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class BuildCachingFuncTest extends AbstractPluginFuncTest {

    @Unroll
    def "test task is still cacheable (gradle version #gradleVersion)"() {
        given:
        successfulTest()
        buildFile << """
            test.retry.maxRetries = 1
        """

        when:
        gradleRunner(gradleVersion)
            .withArguments("--build-cache", "test")
            .build()

        def result = gradleRunner(gradleVersion)
            .withArguments("--build-cache", "clean", "test")
            .build()

        then:
        result.task(":test").outcome == TaskOutcome.FROM_CACHE

        where:
        gradleVersion << GRADLE_VERSIONS_UNDER_TEST
    }

    @Unroll
    def "maxRetries and maxFailures are not treated as inputs (gradle version #gradleVersion)"() {
        given:
        successfulTest()
        buildFile << """
            test.retry.maxRetries = 1
        """

        when:
        gradleRunner(gradleVersion)
            .withArguments("--build-cache", "test")
            .build()

        buildFile << """
            test.retry {
                maxRetries = 2
                maxFailures = 2
            }
        """

        def result = gradleRunner(gradleVersion)
            .withArguments("--build-cache", "clean", "test")
            .build()

        then:
        result.task(":test").outcome == TaskOutcome.FROM_CACHE

        where:
        gradleVersion << GRADLE_VERSIONS_UNDER_TEST
    }

    @Unroll
    def "failOnPassedAfterRetry is input (gradle version #gradleVersion)"() {
        given:
        flakyTest()
        buildFile << """
            test.retry.maxRetries = 1
        """

        when:
        gradleRunner(gradleVersion)
            .withArguments("--build-cache", "test")
            .build()

        buildFile << """
            test.retry {
                failOnPassedAfterRetry = true
            }
        """

        def result = gradleRunner(gradleVersion)
            .withArguments("--build-cache", "clean", "test")
            .buildAndFail()

        then:
        result.task(":test").outcome == TaskOutcome.FAILED

        where:
        gradleVersion << GRADLE_VERSIONS_UNDER_TEST
    }

    @Unroll
    def "removing plugin invalidates cached result (gradle version #gradleVersion)"() {
        given:
        flakyTest()
        buildFile << """
            test.retry.maxRetries = 1
        """

        when:
        gradleRunner(gradleVersion)
            .withArguments("--build-cache", "test")
            .build()

        buildFile.text = baseBuildScript() - "id 'org.gradle.test-retry'"

        def result = gradleRunner(gradleVersion)
            .withArguments("--build-cache", "clean", "test")
            .buildAndFail()

        then:
        result.task(":test").outcome == TaskOutcome.FAILED

        where:
        gradleVersion << GRADLE_VERSIONS_UNDER_TEST
    }

}
