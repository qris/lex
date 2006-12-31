/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class ImmutableMap implements Map {
	private final Map original;
	public ImmutableMap(Map original) {
		this.original = original;
	}
	public Object get(Object key) { return original.get(key); }
	public void putAll(Map newKeys) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This is an immutable map");
	}
	public Object remove(Object key) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This is an immutable map");
	}
	public Object put(Object key, Object value) 
	throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This is an immutable map");
	}
	public void clear() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This is an immutable map");
	}
	public Set keySet()   { return original.keySet(); }
	public Set entrySet() { return original.entrySet(); }
	public int size()     { return original.size(); }
	public Collection values()  { return original.values(); }
	public boolean    isEmpty() { return original.isEmpty(); }
	public boolean containsKey  (Object key) { 
		return original.containsKey(key); 
	}
	public boolean containsValue(Object value) { 
		return original.containsValue(value); 
	}
}