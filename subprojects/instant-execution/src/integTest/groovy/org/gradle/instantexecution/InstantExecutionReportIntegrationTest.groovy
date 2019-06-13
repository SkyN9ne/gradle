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

package org.gradle.instantexecution

import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.util.GradleVersion

class InstantExecutionReportIntegrationTest extends AbstractInstantExecutionIntegrationTest {

    def "summarizes unsupported properties"() {
        given:
        buildFile << """
            class SomeBean {
                Gradle gradle
                def nested = new NestedBean()
            }

            class NestedBean {
                Gradle gradle
                Project project
            }

            class SomeTask extends DefaultTask {
                private final bean = new SomeBean()

                SomeTask() {
                    bean.gradle = project.gradle
                    bean.nested.gradle = project.gradle
                    bean.nested.project = project
                }

                @TaskAction
                void run() {
                }
            }

            // ensure there are multiple warnings for the same properties
            task a(type: SomeTask)
            task b(type: SomeTask)
            task c(dependsOn: [a, b])
        """

        when:
        instantRun "c"

        then:
        def reportFile = file(".instant-execution-state/${GradleVersion.current().version}/4ev4b35p4lwnfkkcxm5f3cqb7.html")
        reportFile.isFile()
        outputContains """
            3 instant execution issues found:
              - field 'SomeBean.gradle': there's no serializer for type 'org.gradle.invocation.DefaultGradle'
              - field 'NestedBean.gradle': there's no serializer for type 'org.gradle.invocation.DefaultGradle'
              - field 'NestedBean.project': there's no serializer for type 'org.gradle.api.internal.project.DefaultProject'
            See the complete report at ${clickableUrlFor(reportFile)}
        """.stripIndent()
    }

    private String clickableUrlFor(File file) {
        new ConsoleRenderer().asClickableFileUrl(file)
    }
}
