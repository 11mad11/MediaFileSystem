package fr.mad.ImageUtil;

import java.util.Map;

public class MapObjectHelper {
	
	private Map<String, ?> map;
	
	public MapObjectHelper(Map<String, ?> map) {
		this.map = map;
	}
	
	public long get(String key, long def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.longValue();
	}
	
	public int get(String key, int def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.intValue();
	}
	
	public short get(String key, short def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.shortValue();
	}
	
	public byte get(String key, byte def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.byteValue();
	}
	
	public double get(String key, double def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.doubleValue();
	}
	
	public float get(String key, float def) {
		Number v = get(key);
		if (v == null)
			return def;
		return v.floatValue();
	}
	
	public String get(String key, String def) {
		Object v = get(key);
		if (v == null)
			return def;
		return v.toString();
	}
	
	public <T, K extends T> T get(String key, K def) {
		T v = get(key);
		if (v == null)
			return def;
		return v;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) map.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getNotNull(String key) {
		T v = (T) map.get(key);
		if (v == null)
			throw new NullPointerException("Null value for : " + key + " is not permitted");
		return v;
	}
}
