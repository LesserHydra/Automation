package com.lesserhydra.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents any iterable "collection" of elements that doesn't fit the contract of a true Collection, usually
 * due to differing strategies for adding/removing. 
 * @author Justin Lawen
 *
 * @param <E>
 */
public interface View<E> extends Iterable<E> {
	
	/**
	 * 
	 * @return
	 */
	public int size();
	
	/**
	 * 
	 * @return
	 */
	public default boolean isEmpty() {
		return (size() == 0);
	}
	
	/**
	 * Returns a stream over the elements, in order of priority.
	 * @return
	 */
	public Stream<E> stream();
	
	/**
	 * 
	 * @return
	 */
	public default List<E> toList() {
		return stream().collect(Collectors.toList());
	}
	
}
