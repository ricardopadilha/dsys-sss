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

package net.dsys.sss.test;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import net.dsys.sss.api.SecretSharing;
import net.dsys.sss.impl.SecretSharings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ricardo Padilha
 */
public final class SSSByteTest {

	private SecretSharing sss;

	public SSSByteTest() {
		super();
	}

	@Before
	public void setUp() throws Exception {
		sss = SecretSharings.getDefault();
	}

	@After
	public void tearDown() throws Exception {
		sss = null;
	}

	private void testSplitJoin(final ByteBuffer src, final int threshold, final int[] ids) {
		final int len = sss.getOutputSize(src.remaining());
		final int cnt = ids.length;
		final ByteBuffer[] dst = new ByteBuffer[cnt];
		for (int i = 0; i < cnt; i++) {
			dst[i] = ByteBuffer.allocate(len);
		}
		final ByteBuffer end = ByteBuffer.allocate(src.remaining());

		sss.init(threshold, ids);
		sss.split(src, dst);

		src.flip();
		for (final ByteBuffer bb : dst) {
			bb.flip();
		}

		sss.join(dst, end);

		end.flip();
		assertEquals(src, end);
	}

	@Test
	public void testSplitJoin() {
		for (int t = 1; t < 256; t++) {
			final int[] ids = createIds(t);
			for (int i = 0; i < 256; i++) {
				final ByteBuffer src = ByteBuffer.allocate(1);
				src.put((byte) i).flip();
				testSplitJoin(src, t, ids);
			}
		}
	}

	@Test
	public void testLength() {
		final int reps = 0x7FFF;
		final int t = 2;
		final int[] ids = {1, 2};
		for (int i = 0; i < reps; i++) {
			final ByteBuffer src = ByteBuffer.allocate(i);
			fillRandom(src);
			testSplitJoin(src, t, ids);
		}
	}

	private static void fillRandom(final ByteBuffer src) {
		final Random rnd = ThreadLocalRandom.current();
		while (src.remaining() > 0) {
			src.put((byte) rnd.nextInt());
		}
		src.flip();
	}

	private static int[] createIds(final int t) {
		final int[] ids = new int[t];
		for (int i = 0; i < t; i++) {
			ids[i] = i + 1;
		}
		return ids;
	}

}
