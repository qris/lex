/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ImmutableMap implements Map
{
	private final Map m_OriginalMap;
    
	public ImmutableMap(Map original)
    {
		m_OriginalMap = new HashMap(original);
	}
    
	public Object get(Object key)
    {
        return m_OriginalMap.get(key);
    }
    
	public void putAll(Map newKeys) throws UnsupportedOperationException
    {
		throw new UnsupportedOperationException("This is an immutable map");
	}
    
	public Object remove(Object key) throws UnsupportedOperationException
    {
		throw new UnsupportedOperationException("This is an immutable map");
	}
    
	public Object put(Object key, Object value) 
	throws UnsupportedOperationException
    {
		throw new UnsupportedOperationException("This is an immutable map");
	}
    
	public void clear() throws UnsupportedOperationException
    {
		throw new UnsupportedOperationException("This is an immutable map");
	}
    
	public Set keySet()   { return m_OriginalMap.keySet(); }
	public Set entrySet() { return m_OriginalMap.entrySet(); }
	public int size()     { return m_OriginalMap.size(); }
	public Collection values()  { return m_OriginalMap.values(); }
	public boolean    isEmpty() { return m_OriginalMap.isEmpty(); }
	public boolean containsKey  (Object key)
    { 
		return m_OriginalMap.containsKey(key); 
	}
	public boolean containsValue(Object value)
    { 
		return m_OriginalMap.containsValue(value); 
	}
}