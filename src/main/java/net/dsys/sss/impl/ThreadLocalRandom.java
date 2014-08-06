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

import java.util.Random;

import javax.annotation.Nonnull;

/**
 * @author Ricardo Padilha
 */
final class ThreadLocalRandom {

	private static final boolean FAST_RANDOM = true;
	private static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
		@Override
		protected Random initialValue() {
			return new it.unimi.dsi.util.XorShift128PlusRandom();
		}
	};

	private ThreadLocalRandom() {
		super();
	}

	@Nonnull
	public static Random current() {
		if (FAST_RANDOM) {
			return RANDOM.get();
		}
		return java.util.concurrent.ThreadLocalRandom.current();
	}
}
