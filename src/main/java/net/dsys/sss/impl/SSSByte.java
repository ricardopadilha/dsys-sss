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

package net.dsys.sss.impl;

import java.nio.ByteBuffer;
import java.util.Random;

import net.dsys.sss.api.SecretSharing;

/**
 * Secret sharing implementation based on bytes.
 * <p>
 * Warning: this class is not thread-safe. In other words, do not
 * {@link #init(int, int)} and {@link #split(ByteBuffer, int[], ByteBuffer[])}
 * or {@link #join(int[], ByteBuffer[], ByteBuffer)} using the same instance on
 * different threads simultaneously without some kind of synchronization.
 * 
 * @author Ricardo Padilha
 */
final class SSSByte implements SecretSharing {

	private static final boolean CACHED = true;
	private static final int MASK = 0xFF;

	private static final int[] MUL;
	private static final int[] INV;

	static {
		if (CACHED) {
			MUL = new int[1 << 16];
		} else {
			MUL = null;
		}
		INV = new int[1 << 8];
		for (int a = 0; a < 256; ++a) {
			for (int b = 0; b < 256; ++b) {
				final int c = gfMul(a, b);
				if (c == 1) {
					INV[a] = b;
				}
				if (CACHED) {
					MUL[(a << 8) + b] = c;
				}
			}
		}
	}

	private int threshold;
	private int[] ids;
	private int[] polynomial;
	private int[] lagrange;

	SSSByte() {
		this.threshold = -1;
	}

	private static int gfMul(final int a, final int b) {
		int result = 0;
		int x = a;
		int y = b;
		for (int i = 0; i < 8; ++i) {
			if ((y & 1) != 0) {
				result ^= x;
			}
			final boolean carry = (x & 0x80) != 0;
			x <<= 1;
			x &= MASK;
			if (carry) {
				x ^= 0x1b;
			}
			y >>= 1;
		}
		return result & MASK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBlockSize() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getOutputSize(final int length) {
		return length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final int threshold, final int[] ids) {
		if (threshold < 1) {
			throw new IllegalArgumentException("threshold < 1");
		}
		validateIds(ids);
		this.threshold = threshold;
		this.ids = ids.clone();
		this.polynomial = createPolynomial(threshold);
		this.lagrange = computeLagrange(this.ids);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rekey() {
		checkState();
		this.polynomial = createPolynomial(threshold);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void split(final ByteBuffer src, final ByteBuffer[] dst) {
		checkState();
		validateBuffers(ids, dst);
		final int count = dst.length;
		final int degree = polynomial.length;
		final int[] shares = new int[dst.length];
		while (src.remaining() > 0) {
			polynomial[degree - 1] = src.get() & MASK;
			split(polynomial, ids, shares);
			for (int j = 0; j < count; j++) {
				dst[j].put((byte) shares[j]);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void join(final ByteBuffer[] src, final ByteBuffer dst) {
		checkState();
		validateBuffers(ids, src);
		final int count = src.length;
		final int[] bs = new int[src.length];
		while (src[0].remaining() > 0) {
			for (int j = 0; j < count; j++) {
				bs[j] = src[j].get() & MASK;
			}
			final byte join = (byte) join(lagrange, bs);
			dst.put(join);
		}
	}

	private void checkState() {
		if (threshold < 0) {
			throw new IllegalArgumentException("secret sharing not initialized");
		}
	}

	private static void validateIds(final int[] ids) {
		if (ids == null) {
			throw new IllegalArgumentException("ids == null");
		}
		if (ids.length == 0) {
			throw new IllegalArgumentException("ids.length == 0");
		}
		final int k = ids.length;
		for (int i = 0; i < k; i++) {
			if (ids[i] > 255) {
				throw new IllegalArgumentException("ids must be lower than 255: " + ids[i]);
			}
			ids[i] &= MASK;
		}
	}

	private static int[] createPolynomial(final int threshold) {
		final Random rnd = ThreadLocalRandom.current();
		final int[] result = new int[threshold];
		for (int i = 0; i < threshold; i++) {
			result[i] = (rnd.nextInt(255) + 1) & MASK;
		}
		return result;
	}

	/**
	 * This method assumes that all integer parameters have been masked with 0xFF
	 */
	private static int[] computeLagrange(final int[] ids) {
		final int length = ids.length;
		final int[] result = new int[length];
		for (int i = 0; i < length; ++i) {
			int p = 1;
			int q = 1;
			for (int j = 0; j < length; ++j) {
				if (i == j) {
					continue;
				}
				if (CACHED) {
					p = MUL[(p << 8) + ids[j]];
					q = MUL[(q << 8) + (ids[j] ^ ids[i])];
				} else {
					p = gfMul(p, ids[j]);
					q = gfMul(q, ids[j] ^ ids[i]);
				}
			}
			if (CACHED) {
				result[i] = MUL[(p << 8) + INV[q]];
			} else {
				result[i] = gfMul(p, INV[q]);
			}
		}
		return result;
	}

	/**
	 * This method assumes that all integer parameters have been masked with 0xFF
	 */
	private static void split(final int[] polynomial, final int[] ids, final int[] shares) {
		final int degree = polynomial.length;
		final int k = shares.length;
		for (int i = 0; i < k; ++i) {
			final int x = ids[i];
			int r = 0;
			for (int j = 0; j < degree; ++j) {
				if (CACHED) {
					r = MUL[(r << 8) + x] ^ polynomial[j];
				} else {
					r = gfMul(r, x) ^ polynomial[j];
				}
			}
			shares[i] = r & MASK;
		}
	}

	/**
	 * This method assumes that all integer parameters have been masked with 0xFF
	 */
	private static int join(final int[] lagrange, final int[] shares) {
		final int n = lagrange.length;
		int r = 0;
		for (int i = 0; i < n; ++i) {
			if (CACHED) {
				r ^= MUL[(shares[i] << 8) + lagrange[i]];
			} else {
				r ^= gfMul(shares[i], lagrange[i]);
			}
		}
		return r & MASK;
	}

	private static void validateBuffers(final int[] ids, final ByteBuffer[] buffers) {
		if (buffers == null) {
			throw new IllegalArgumentException("dst == null");
		}
		if (buffers.length == 0) {
			throw new IllegalArgumentException("dst.length == 0");
		}
		if (ids.length != buffers.length) {
			throw new IllegalArgumentException("ids.length != dst.length");
		}
		final int k = ids.length;
		final int length = buffers[0].remaining();
		for (int i = 1; i < k; i++) {
			if (buffers[i].remaining() != length) {
				throw new IllegalArgumentException("buffers are not the same length");
			}
		}
	}
}
