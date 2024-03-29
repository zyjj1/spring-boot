/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.util.Iterator;
import java.util.zip.ZipEntry;

/**
 * Provides information about the jar layers.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @see ExtractCommand
 * @see ListCommand
 */
interface Layers extends Iterable<String> {

	/**
	 * Return the jar layers in the order that they should be added (starting with the
	 * least frequently changed layer).
	 */
	@Override
	Iterator<String> iterator();

	/**
	 * Return the layer that a given entry is in.
	 * @param entry the entry to check
	 * @return the layer that the entry is in
	 */
	default String getLayer(ZipEntry entry) {
		return getLayer(entry.getName());
	}

	/**
	 * Return the layer that the entry with the given name is in.
	 * @param entryName the name of the entry to check
	 * @return the layer that the entry is in
	 */
	String getLayer(String entryName);

	/**
	 * Return the name of the application layer.
	 * @return the name of the application layer
	 */
	String getApplicationLayerName();

	/**
	 * Return a {@link Layers} instance for the currently running application.
	 * @param context the command context
	 * @return a new layers instance
	 * @throws LayersNotEnabledException if layers are not enabled
	 */
	static Layers get(Context context) {
		IndexedLayers indexedLayers = IndexedLayers.get(context);
		if (indexedLayers == null) {
			throw new LayersNotEnabledException();
		}
		return indexedLayers;
	}

	final class LayersNotEnabledException extends RuntimeException {

		LayersNotEnabledException() {
			super("Layers not enabled: Failed to load layer index file");
		}

	}

}
