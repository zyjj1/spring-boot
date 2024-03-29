/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.lifecycle.TestContainersParallelStartupIntegrationTests.ContainerConfig;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for parallel startup.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContainerConfig.class)
@TestPropertySource(properties = "spring.testcontainers.beans.startup=parallel")
@DirtiesContext
@DisabledIfDockerUnavailable
@ExtendWith(OutputCaptureExtension.class)
public class TestContainersParallelStartupIntegrationTests {

	@Test
	void startsInParallel(CapturedOutput out) {
		assertThat(out).contains("-lifecycle-0").contains("-lifecycle-1").contains("-lifecycle-2");
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfig {

		@Bean
		static PostgreSQLContainer<?> container1() {
			return new PostgreSQLContainer<>(DockerImageNames.postgresql());
		}

		@Bean
		static PostgreSQLContainer<?> container2() {
			return new PostgreSQLContainer<>(DockerImageNames.postgresql());
		}

		@Bean
		static PostgreSQLContainer<?> container3() {
			return new PostgreSQLContainer<>(DockerImageNames.postgresql());
		}

	}

}
