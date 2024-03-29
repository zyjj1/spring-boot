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

package org.springframework.boot.ssl;

/**
 * Interface that can be used to register an {@link SslBundle} for a given name.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public interface SslBundleRegistry {

	/**
	 * Register a named {@link SslBundle}.
	 * @param name the bundle name
	 * @param bundle the bundle
	 */
	void registerBundle(String name, SslBundle bundle);

	/**
	 * Updates an {@link SslBundle}.
	 * @param name the bundle name
	 * @param updatedBundle the updated bundle
	 * @throws NoSuchSslBundleException if the bundle cannot be found
	 * @since 3.2.0
	 */
	void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException;

}
