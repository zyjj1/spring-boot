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

package org.springframework.boot.testsupport.testcontainers;

import org.testcontainers.containers.GenericContainer;

/**
 * A {@link GenericContainer} for ActiveMQ.
 *
 * @author Stephane Nicoll
 */
public class ActiveMQContainer extends GenericContainer<ActiveMQContainer> {

	private static final int DEFAULT_PORT = 61616;

	public ActiveMQContainer() {
		super(DockerImageNames.activeMq());
		addExposedPorts(DEFAULT_PORT);
	}

	/**
	 * Return the broker URL to use.
	 * @return the broker url of the ActiveMQ instance
	 */
	public String getBrokerUrl() {
		return String.format("tcp://" + getHost() + ":" + getMappedPort(DEFAULT_PORT));
	}

}
