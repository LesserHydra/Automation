package com.lesserhydra.util;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A PriorityView implementation that uses enumeration values as priorities.
 * This view is sorted by declaration order of the priority enumeration.
 * @author Justin Lawen
 *
 * @param <P> Priority enumeration
 * @param <E> Element type
 */
//TODO: Certainly needs a rewrite when I'm feeling more up to it.
public class EnumMapPriorityView<P extends Enum<P>, E> implements PriorityView<P, E> {
	
	private final P[] priorityTypes;
	
	//private Head<P, E> head;
	private Map<P, LinkedList<E>> priorities;
	
	
	public EnumMapPriorityView(Class<P> priorityType) {
		this.priorities = new EnumMap<>(priorityType);
		this.priorityTypes = priorityType.getEnumConstants();
		
		/*Head<P, E> lastHead = new Head<>(priorityTypes[0]);
		for (int i = 1; i < priorityTypes.length; i++) {
			P type = priorityTypes[i];
			Head<P, E> newHead = new Head<>(type);
			priorityHeads.put(type, newHead);
			lastHead.next = newHead;
			lastHead = newHead;
		}*/
	}
	
	@Override
	public void add(P priority, E element) {
		getPriorityList(priority).add(element);
	}

	@Override
	public boolean remove(E element) {
		for (int i = 0; i < priorityTypes.length; i++) {
			if (remove(priorityTypes[i], element)) return true;
		}
		return false;
	}
	
	private boolean remove(P priority, E element) {
		LinkedList<E> list = priorities.get(priority);
		return (list == null ? false : list.remove(list));
	}
	
	@Override
	public int size() {
		int result = 0;
		for (int i = 0; i < priorityTypes.length; i++) {
			result += size(priorityTypes[i]);
		}
		return result;
	}

	private int size(P priority) {
		LinkedList<E> list = priorities.get(priority);
		return (list == null ? 0 : list.size());
	}
	
	@Override
	public Iterator<E> iterator() {
		return new PIterator();
	}

	@Override
	public Stream<E> stream() {
		return Arrays.stream(priorityTypes)
				.flatMap(this::stream);
	}

	private Stream<E> stream(P priority) {
		LinkedList<E> list = priorities.get(priority);
		return (list != null ? list.stream() : Stream.empty());
	}
	
	private LinkedList<E> getPriorityList(P priority) {
		LinkedList<E> result = priorities.get(priority);
		if (result != null) return result;
		result = new LinkedList<>();
		priorities.put(priority, result);
		return result;
	}
	
	private class PIterator implements Iterator<E> {
		
		private int i;
		private Iterator<E> it;
		
		PIterator() {
			i = -1;
			it = getNextIt();
		}
		
		@Override
		public boolean hasNext() {
			return (i < priorityTypes.length && it.hasNext());
		}
		
		@Override
		public E next() {
			E result = it.next();
			if (!it.hasNext()) it = getNextIt();
			return result;
		}
		
		private Iterator<E> getNextIt() {
			i++;
			while (i < priorityTypes.length) {
				LinkedList<E> nextList = priorities.get(priorityTypes[i]);
				if (nextList != null) return nextList.iterator();
				i++;
			}
			return null;
		}
		
	}
	
	/*private static class Head<P, E> extends Node<E> {
		final P priority;
		
		Head(P priority) {
			super(null, null, null);
			this.priority = priority;
		}
	}
	
	private static class Node<E> {
		E item;
		Node<E> next;
		Node<E> prev;
		
		Node(Node<E> prev, E element, Node<E> next) {
			this.item = element;
			this.next = next;
			this.prev = prev;
		}
	}*/
	
}
