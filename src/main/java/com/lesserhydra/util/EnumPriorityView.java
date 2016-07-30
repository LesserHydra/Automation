package com.lesserhydra.util;

/**
 * A PriorityView implementation that uses enumeration values as priorities.
 * This view is sorted by declaration order of the priority enumeration.
 * @author Justin Lawen
 *
 * @param <P> Priority enumeration
 * @param <E> Element type
 */
public abstract class EnumPriorityView<P extends Enum<P>, E> implements PriorityView<P, E> {
	
	/*private final P[] types;
	
	private int size;
	private Node<E> head;
	private Node<E> tail;
	private Map<P, Node<E>> heads;
	private Map<P, Node<E>> tails;
	
	
	public EnumPriorityView(Class<P> priorityType) {
		this.types = priorityType.getEnumConstants();
		this.tails = new EnumMap<>(priorityType);
		
		this.head = new Node<>(null, null, null);
		this.tail = new Node<>(head, null, null);
		head.next = tail;
		for (P type : types) {
			tails.put(type, head);
		}
	}
	
	@Override
	public void add(P priority, E element) {
		Objects.requireNonNull(priority);
		Objects.requireNonNull(element);
		
		size += 1;
		findTail(priority, element);
	}
	
	//Returns last node of given priority
	private Node<E> findTail(P p) {
		Node<E> found = tails.get(p);
		if (found.item != null) return found;
		
		//TODO: First
		//TODO: Last
		
		//Middle
		Node<E> prevNode = found.prev;
		Node<E> nextNode = found.next;
		P prevP = types[p.ordinal()-1];
		P nextP = types[p.ordinal()+1];
		
		//Previous node is previous type
		boolean prevGood = tails.get(prevP) == prevNode;
		//Next node is next type
		boolean nextGood = heads.get(nextP) == nextNode;
		
		
	}
	
	private Node<E> insertNewBefore(Node<E> next) {
		Node<E> node = new Node<>(next.prev, null, next);
		next.prev = node;
		if (node.prev != null) node.prev.next = node;
		if (head == next) head = node;
		return node;
	}
	
	private Node<E> insertNewAfter(Node<E> prev) {
		Node<E> node = new Node<>(prev, null, prev.next);
		prev.next = node;
		if (node.next != null) node.next.prev = node;
		if (tail == prev) tail = node;
		return node;
	}
	
	private Node<E> findHead(P p) {
		int i = p.ordinal();
		Node<E> head = heads.get(p);
		if (head.item != null) return head;
		
		//Update previous nodes
		updatePrevious(p, head);
		//Update following nodes
		updateFollowing(p, head);
	}
	
	private void deleteNode(Node<E> node) {
		node.item = null;
		if (node.prev == null) return;
		node.prev.next = node.next;
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
