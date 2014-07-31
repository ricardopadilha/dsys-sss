/**
 * Copyright 2014 Ricardo Padilha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dsys.sss.api;

import java.nio.ByteBuffer;

/**
 * This is the standard interface of secret sharing providers.
 * 
 * @author Ricardo Padilha
 */
public interface SecretSharing {

	/**
	 * @return the size of the block that is share at a time, in bytes.
	 *         Implementations are allowed to fail if callers provide data that
	 *         is not a multiple of the block size.
	 */
	int getBlockSize();

	/**
	 * @return the size of the output for a given input length
	 */
	int getOutputSize(int length);

	/**
	 * Initializes the algorithm for a given threshold and number of shares.
	 * 
	 * @param threshold
	 *            the minimum number of shares that will be needed to reassemble
	 *            the secret
	 * @param ids
	 *            the id of each share (i.e. the 'x' coordinate)
	 */
	void init(int threshold, int[] ids);

	/**
	 * Regenerate key material.
	 */
	void rekey();

	/**
	 * Splits the content of secret between the shares.
	 * The cardinality of the ids given in {@link #init(int, int[])}
	 * and shares must match.
	 * 
	 * @param src
	 *            the source of the secret data
	 * @param dst
	 *            the content of the shares
	 */
	void split(ByteBuffer src, ByteBuffer[] dst);

	/**
	 * Joins the shares in the secret buffer, using the ids given
	 * in {@link #init(int, int[])} to locate the shares.
	 * The cardinality of shares and ids must match.
	 * 
	 * @param src
	 *            source for the content of the shares
	 * @param dst
	 *            destination for the reassembled secret
	 */
	void join(ByteBuffer[] src, ByteBuffer dst);

}
