package com.lesserhydra.util;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigUtil {
	
	/**
	 * Creates a typed list containing all elements of a typeless list that instantiate the given class.
	 * Note that this method silently skips over any elements that do not mach the given class.
	 * @param list Typeless list
	 * @param clazz Class of elements
	 * @return Typed copy of list
	 */
	public static <T> List<T> castList(List<?> list, Class<T> clazz) {
		return list.stream()
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.collect(Collectors.toList());
	}
	
}
