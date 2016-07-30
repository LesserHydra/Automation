package com.lesserhydra.util;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

public class SetBuilder<T> {
	
	private final Set<T> set;
	private boolean done = false;
	
	public static <T> SetBuilder<T> init(Supplier<Set<T>> setSupplier) {
		return new SetBuilder<>(setSupplier.get());
	}
	
	public SetBuilder<T> add(T value) {
		if (done) throw new IllegalStateException("Builder has already been used.");
		set.add(value);
		return this;
	}
	
	public Set<T> build() {
		if (done) throw new IllegalStateException("Builder has already been used.");
		done = true;
		return set;
	}
	
	public Set<T> buildImmutable() {
		if (done) throw new IllegalStateException("Builder has already been used.");
		done = true;
		return Collections.unmodifiableSet(set);
	}
	
	private SetBuilder(Set<T> set) {
		this.set = set;
	}
}
