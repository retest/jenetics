/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
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
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.ext.util;

import static java.lang.String.format;

import java.util.Comparator;

/**
 * Represents a 2-dimensional point.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
public final class Point2 implements MOV<Point2> {
	private final double _x;
	private final double _y;

	private Point2(final double x, final double y) {
		_x = x;
		_y = y;
	}

	/**
	 * Return the <em>x</em> value of the point.
	 *
	 * @return the <em>x</em> value of the point
	 */
	public double x() {
		return _x;
	}

	/**
	 * Return the <em>y</em> value of the point.
	 *
	 * @return the <em>y</em> value of the point
	 */
	public double y() {
		return _y;
	}

	@Override
	public Point2 value() {
		return this;
	}

	@Override
	public int size() {
		return 2;
	}

	@Override
	public ElementComparator<Point2> comparator() {
		return (u, v, i) -> {
			if (i < 0 || i > 1) {
				throw new IndexOutOfBoundsException(format(
					"Index out of bounds [0, 2): %d", i
				));
			}

			return i == 0
				? Double.compare(u._x, v._x)
				: Double.compare(u._y, v._y);
		};
	}

	@Override
	public Comparator<Point2> dominance() {
		return (u, v) -> {
			boolean adom = false;
			boolean bdom = false;

			int cmp = Double.compare(u._x, v._x);
			if (cmp > 0) {
				adom = true;
			} else if (cmp < 0) {
				bdom = true;
			}

			cmp = Double.compare(u._y, v._y);
			if (cmp > 0) {
				adom = true;
				if (bdom) {
					return 0;
				}
			} else if (cmp < 0) {
				bdom = true;
				if (adom) {
					return 0;
				}
			}

			if (adom == bdom) {
				return 0;
			} else if (adom) {
				return 1;
			} else {
				return -1;
			}
		};
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash += 31*Double.hashCode(_x) + 37;
		hash += 31*Double.hashCode(_y) + 37;
		return hash;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Point2 &&
			Double.compare(((Point2)obj)._x, _x) == 0 &&
			Double.compare(((Point2)obj)._y, _y) == 0;
	}

	@Override
	public String toString() {
		return format("[%f, %f]", _x, _y);
	}

	/**
	 * Create a new 2-dimensional point from the given values.
	 *
	 * @param x the x value
	 * @param y the y value
	 * @return a new 2-dimensional point from the given values
	 */
	public static Point2 of(final double x, final double y) {
		return new Point2(x, y);
	}

}
