package com.lesserhydra.automation;


public interface Module {
	
	default void init() {};

	default void deinit() {};
	
}
