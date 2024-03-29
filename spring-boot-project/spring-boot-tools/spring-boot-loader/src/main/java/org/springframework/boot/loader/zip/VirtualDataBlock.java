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

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * A virtual {@link DataBlock} build from a collection of other {@link DataBlock}
 * instances.
 *
 * @author Phillip Webb
 */
class VirtualDataBlock implements DataBlock {

	private List<DataBlock> parts;

	private long size;

	/**
	 * Create a new {@link VirtualDataBlock} instance. The {@link #setParts(Collection)}
	 * method must be called before the data block can be used.
	 */
	protected VirtualDataBlock() {
	}

	/**
	 * Create a new {@link VirtualDataBlock} backed by the given parts.
	 * @param parts the parts that make up the virtual data block
	 * @throws IOException in I/O error
	 */
	VirtualDataBlock(Collection<? extends DataBlock> parts) throws IOException {
		setParts(parts);
	}

	/**
	 * Set the parts that make up the virtual data block.
	 * @param parts the data block parts
	 * @throws IOException on I/O error
	 */
	protected void setParts(Collection<? extends DataBlock> parts) throws IOException {
		this.parts = List.copyOf(parts);
		long size = 0;
		for (DataBlock part : parts) {
			size += part.size();
		}
		this.size = size;
	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public int read(ByteBuffer dst, long pos) throws IOException {
		if (pos < 0 || pos >= this.size) {
			return -1;
		}
		long offset = 0;
		int result = 0;
		for (DataBlock part : this.parts) {
			while (pos >= offset && pos < offset + part.size()) {
				int count = part.read(dst, pos - offset);
				result += Math.max(count, 0);
				if (count <= 0 || !dst.hasRemaining()) {
					return result;
				}
				pos += count;
			}
			offset += part.size();
		}
		return result;
	}

}
