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

import static java.util.Objects.requireNonNull;
import static io.jenetics.internal.util.SerialIO.readIntArray;
import static io.jenetics.internal.util.SerialIO.readObjectArray;
import static io.jenetics.internal.util.SerialIO.writeIntArray;
import static io.jenetics.internal.util.SerialIO.writeObjectArray;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.jenetics.util.ISeq;

/**
 * Default implementation of the {@link FlatTree} interface. Beside the
 * flattened and dense layout it is also an <em>immutable</em> implementation of
 * the {@link Tree} interface. It can only be created from an existing tree.
 *
 * <pre>{@code
 * final Tree<String, ?> immutable = FlatTreeNode.of(TreeNode.parse(...));
 * }</pre>
 *
 * @implNote
 * This class is immutable and thread-safe.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version 5.0
 * @since 3.9
 */
public final class FlatTreeNode<T>
	implements
		FlatTree<T, FlatTreeNode<T>>,
		Serializable
{
	private static final long serialVersionUID = 3L;

	private final int _index;
	private final Object[] _elements;
	private final int[] _childOffsets;
	private final int[] _childCounts;

	private FlatTreeNode(
		final int index,
		final Object[] elements,
		final int[] childOffsets,
		final int[] childCounts
	) {
		_index = index;
		_elements = requireNonNull(elements);
		_childOffsets = requireNonNull(childOffsets);
		_childCounts = requireNonNull(childCounts);
	}

	/**
	 * Returns the root of the tree that contains this node. The root is the
	 * ancestor with no parent. This implementation have a runtime complexity
	 * of O(1).
	 *
	 * @return the root of the tree that contains this node
	 */
	@Override
	public FlatTreeNode<T> getRoot() {
		return nodeAt(0);
	}

	@Override
	public boolean isRoot() {
		return _index == 0;
	}

	private FlatTreeNode<T> nodeAt(final int index) {
		return new FlatTreeNode<T>(
			index,
			_elements,
			_childOffsets,
			_childCounts
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getValue() {
		return (T) _elements[_index];
	}

	@Override
	public Optional<FlatTreeNode<T>> getParent() {
		int index = -1;
		for (int i = _index; --i >= 0 && index == -1;) {
			if (isParent(i)) {
				index = i;
			}
		}

		return index != -1
			? Optional.of(nodeAt(index))
			: Optional.empty();
	}

	private boolean isParent(final int index) {
		return _childCounts[index] > 0 &&
			_childOffsets[index] <= _index &&
			_childOffsets[index] + _childCounts[index] > _index;
	}

	@Override
	public FlatTreeNode<T> childAt(final int index) {
		if (index < 0 || index >= childCount()) {
			throw new IndexOutOfBoundsException(Integer.toString(index));
		}

		return nodeAt(childOffset() + index);
	}

	@Override
	public int childCount() {
		return _childCounts[_index];
	}

	/**
	 * Return the index of the first child node in the underlying node array.
	 * {@code -1} is returned if {@code this} node is a leaf.
	 *
	 * @return Return the index of the first child node in the underlying node
	 *         array, or {@code -1} if {@code this} node is a leaf
	 */
	@Override
	public int childOffset() {
		return _childOffsets[_index];
	}

	@Override
	public ISeq<FlatTreeNode<T>> flattenedNodes() {
		return stream().collect(ISeq.toISeq());
	}

	/**
	 * Return a stream of all nodes of the whole underlying tree. This method
	 * call is equivalent to
	 * <pre>{@code
	 * final Stream<FlatTreeNode<T>> nodes = getRoot().breadthFirstStream();
	 * }</pre>
	 *
	 * @return a stream of all nodes of the whole underlying tree
	 */
	public Stream<FlatTreeNode<T>> stream() {
		return IntStream.range(0, _elements.length).mapToObj(this::nodeAt);
	}

	/**
	 * Return a sequence of all <em>mapped</em> nodes of the whole underlying
	 * tree. This is a convenient method for
	 * <pre>{@code
	 * final ISeq<B> seq = stream()
	 *     .map(mapper)
	 *     .collect(ISeq.toISeq())
	 * }</pre>
	 *
	 * @param mapper the mapper function
	 * @param <B> the mapped type
	 * @return a sequence of all <em>mapped</em> nodes
	 */
	public <B> ISeq<B> map(final Function<FlatTreeNode<T>, ? extends B> mapper) {
		return stream()
			.map(mapper)
			.collect(ISeq.toISeq());
	}

	@Override
	public boolean identical(final Tree<?, ?> other) {
		return other == this ||
			other instanceof FlatTreeNode &&
			((FlatTreeNode)other)._index == _index &&
			((FlatTreeNode)other)._elements == _elements;
	}

	@Override
	public int hashCode() {
		return Tree.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this ||
			obj instanceof FlatTreeNode &&
			(equals((FlatTreeNode<?>)obj) || Tree.equals((Tree<?, ?>)obj, this));
	}

	private boolean equals(final FlatTreeNode<?> tree) {
		return tree._index == _index &&
			Arrays.equals(tree._elements, _elements) &&
			Arrays.equals(tree._childCounts, _childCounts) &&
			Arrays.equals(tree._childOffsets, _childOffsets);
	}

	@Override
	public String toString() {
		return toParenthesesString();
	}

	@Override
	public int size() {
		return countChildren( _index) + 1;
	}

	private int countChildren(final int index) {
		int count = _childCounts[index];
		for (int i = 0; i < _childCounts[index]; ++i) {
			count += countChildren(_childOffsets[index] + i);
		}
		return count;
	}

	/**
	 * Create a new, immutable {@code FlatTreeNode} from the given {@code tree}.
	 *
	 * @param tree the source tree
	 * @param <V> the tree value types
	 * @return a new {@code FlatTreeNode} from the given {@code tree}
	 * @throws NullPointerException if the given {@code tree} is {@code null}
	 */
	public static <V> FlatTreeNode<V> of(final Tree<? extends V, ?> tree) {
		requireNonNull(tree);

		final int size = tree.size();
		assert size >= 1;

		final Object[] elements = new Object[size];
		final int[] childOffsets = new int[size];
		final int[] childCounts = new int[size];

		int childOffset = 1;
		int index = 0;

		final Iterator<? extends Tree<?, ?>> it = tree.breadthFirstIterator();
		while (it.hasNext()) {
			final Tree<?, ?> node = it.next();

			elements[index] = node.getValue();
			childCounts[index] = node.childCount();
			childOffsets[index] = node.isLeaf() ? -1 : childOffset;

			childOffset += node.childCount();
			++index;
		}

		return new FlatTreeNode<>(
			0,
			elements,
			childOffsets,
			childCounts
		);
	}

	/**
	 * Parses a (parentheses) tree string, created with
	 * {@link Tree#toParenthesesString()}. The tree string might look like this:
	 * <pre>
	 *  mul(div(cos(1.0),cos(π)),sin(mul(1.0,z)))
	 * </pre>
	 *
	 * @see Tree#toParenthesesString(Function)
	 * @see Tree#toParenthesesString()
	 * @see TreeNode#parse(String)
	 *
	 * @since 5.0
	 *
	 * @param tree the parentheses tree string
	 * @return the parsed tree
	 * @throws NullPointerException if the given {@code tree} string is
	 *         {@code null}
	 * @throws IllegalArgumentException if the given tree string could not be
	 *         parsed
	 */
	public static FlatTreeNode<String> parse(final String tree) {
		return of(TreeParser.parse(tree, Function.identity()));
	}

	/**
	 * Parses a (parentheses) tree string, created with
	 * {@link Tree#toParenthesesString()}. The tree string might look like this
	 * <pre>
	 *  0(1(4,5),2(6),3(7(10,11),8,9))
	 * </pre>
	 * and can be parsed to an integer tree with the following code:
	 * <pre>{@code
	 * final Tree<Integer, ?> tree = FlatTreeNode.parse(
	 *     "0(1(4,5),2(6),3(7(10,11),8,9))",
	 *     Integer::parseInt
	 * );
	 * }</pre>
	 *
	 * @see Tree#toParenthesesString(Function)
	 * @see Tree#toParenthesesString()
	 * @see TreeNode#parse(String, Function)
	 *
	 * @since 5.0
	 *
	 * @param <B> the tree node value type
	 * @param tree the parentheses tree string
	 * @param mapper the mapper which converts the serialized string value to
	 *        the desired type
	 * @return the parsed tree object
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if the given parentheses tree string
	 *         doesn't represent a valid tree
	 */
	public static <B> FlatTreeNode<B> parse(
		final String tree,
		final Function<? super String, ? extends B> mapper
	) {
		return of(TreeParser.parse(tree, mapper));
	}


	/* *************************************************************************
	 *  Java object serialization
	 * ************************************************************************/

	private Object writeReplace() {
		return new Serial(Serial.FLAT_TREE_NODE, this);
	}

	private void readObject(final ObjectInputStream stream)
		throws InvalidObjectException
	{
		throw new InvalidObjectException("Serialization proxy required.");
	}


	void write(final ObjectOutput out) throws IOException {
		final FlatTreeNode<T> node = _index == 0 ? this : of(this);

		writeObjectArray(node._elements, out);
		writeIntArray(node._childOffsets, out);
		writeIntArray(node._childCounts, out);
	}

	@SuppressWarnings("rawtypes")
	static FlatTreeNode read(final ObjectInput in)
		throws IOException, ClassNotFoundException
	{
		return new FlatTreeNode(
			0,
			readObjectArray(in),
			readIntArray(in),
			readIntArray(in)
		);
	}

}
