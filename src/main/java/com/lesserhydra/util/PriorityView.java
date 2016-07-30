package com.lesserhydra.util;

/**
 * Represents a "collection" of elements ordered by priority.
 * @author Justin Lawen
 *
 * @param <P> Priority type
 * @param <E> Element type
 */
public interface PriorityView<P,E> extends View<E> {
	
	/**
	 * Adds an element with the given priority.
	 * @param priority
	 * @param element
	 */
	public void add(P priority, E element);
	
	/**
	 * Removes an element.
	 * @param element
	 * @return
	 */
	public boolean remove(E element);
	
}
