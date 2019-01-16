package fr.mad.ImageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8Runnable;
import com.eclipsesource.v8.utils.V8Thread;

import io.alicorn.v8.V8JavaAdapter;

public class ScriptManager {
	
	public final static List<Object> objects = new ArrayList<>();
	public final static List<Class<?>> classes = new ArrayList<>();
	
	public V8Thread runScript(String script) {
		return new V8Thread(new V8Runnable() {
			
			public void run(V8 runtime) {
				loadAppObject(runtime);
				runtime.executeVoidScript(script);
			}
		});
	}
	
	protected void loadAppObject(V8 runtime) {
		for (Class<?> clazz : classes) {
			V8JavaAdapter.injectClass(clazz, runtime);
		}
		for (Object obj : objects) {
			String name = obj.getClass().getName();
			V8JavaAdapter.injectObject(name, obj.getClass(), runtime);
		}
	}
	
	private JavaCallback jc(BiFunction<V8Object, V8Array, Object> cb) {
		return new JavaCallback() {
			
			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				return cb.apply(receiver, parameters);
			}
		};
	}
}
