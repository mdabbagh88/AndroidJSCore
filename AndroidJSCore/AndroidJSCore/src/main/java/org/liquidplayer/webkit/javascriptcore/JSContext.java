//
// JSContext.java
// AndroidJSCore project
//
// https://github.com/ericwlange/AndroidJSCore/
//
// Created by Eric Lange
//
/*
 Copyright (c) 2014 Eric Lange. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.liquidplayer.webkit.javascriptcore;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import org.liquidplayer.hemroid.JavaScriptCoreGTK;

import java.util.HashMap;
import java.util.Map;


/**
 * Wraps a JavaScriptCore context 
 */
public class JSContext extends JSObject {

	private class JSContextWorker extends Thread {

		private Boolean mReady = false;
        private int mThreadId;

		@Override
		public void run() {
			Looper.prepare();
			workerHandler = new Handler();
            mThreadId = android.os.Process.myTid();
            synchronized (mReady) {
                mReady = true;
            }
			Looper.loop();
		}

		public Handler workerHandler;

		public JSContextWorker() {
			super();
			start();
			Boolean ready = false;
			while (!ready) {
				synchronized (mReady) {
					ready = mReady;
				}
			}
		}

        private class SyncRunnable implements Runnable {
            SyncRunnable(Runnable runnable) {
                mRunnable = runnable;
            }
            private final Runnable mRunnable;
            private Boolean mComplete = false;
            private JSException exception = null;
            @Override
            public void run() {
                try {
                    mRunnable.run();
                } catch (JSException e) {
                    exception = e;
                }
                synchronized (mComplete) {
                    mComplete = true;
                }
            }
            public void block() throws JSException {
                Boolean complete = false;
                while (!complete) {
                    synchronized (mComplete) {
                        complete = mComplete;
                    }
                }
                if (exception != null) throw exception;
            }
        }

        public void sync(final Runnable runnable) throws JSException {
            int currThreadId = android.os.Process.myTid();
            if (currThreadId == mThreadId) {
                runnable.run();
            } else {
                SyncRunnable syncr = new SyncRunnable(runnable);
                workerHandler.post(syncr);
                syncr.block();
            }
        }
        public void async(final Runnable runnable) {
            workerHandler.post(runnable);
        }
	};

    private final JSContextWorker mWorker;

    public void sync(Runnable runnable) {
        mWorker.sync(runnable);
    }
    public void async(Runnable runnable) {
        mWorker.async(runnable);
    }

	/**
	 * Object interface for handling JSExceptions.
	 * @since 2.1
	 */
	public interface IJSExceptionHandler {
		/**
		 * Implement this method to catch JSExceptions
		 * @param exception caught exception
		 * @since 2.1
		 */
        public void handle(JSException exception);
	}

	protected Long ctx;
	private IJSExceptionHandler exceptionHandler;
	
	/**
	 * Creates a new JavaScript context
	 * @since 1.0
	 */
	public JSContext() {
        mWorker = new JSContextWorker();
        context = this;
        sync(new Runnable() {
            @Override public void run() {
                ctx = create();
                valueRef = getGlobalObject(ctx);
                protect(context,valueRef);
            }
        });
	}
	/**
	 * Creates a new JavaScript context in the context group 'inGroup'.
	 * @param inGroup  The context group to create the context in
	 * @since 1.0
	 */
	public JSContext(final JSContextGroup inGroup) {
        mWorker = new JSContextWorker();
        context = this;
        sync(new Runnable() {
            @Override public void run() {
                ctx = createInGroup(inGroup.groupRef());
                valueRef = getGlobalObject(ctx);
                protect(context, valueRef);
            }
        });
	}
	/**
	 * Creates a JavaScript context, and defines the global object with interface 'iface'.  This
	 * object must implement 'iface'.  The methods in 'iface' will be exposed to the JavaScript environment.
	 * @param iface  The interface to expose to JavaScript
	 * @since 1.0
	 * @throws JSException
	 */
	public JSContext(Class<?> iface) throws JSException {
        mWorker = new JSContextWorker();
        context = this;
        sync(new Runnable() {
            @Override public void run() {
                ctx = create();
                valueRef = getGlobalObject(ctx);
            }
        });
        initJSInterface(this, iface, null);
	}
	/**
	 * Creates a JavaScript context in context group 'inGroup', and defines the global object
	 * with interface 'iface'.  This object must implement 'iface'.  The methods in 'iface' will 
	 * be exposed to the JavaScript environment.
	 * @param inGroup  The context group to create the context in
	 * @param iface  The interface to expose to JavaScript
	 * @since 1.0
	 * @throws JSException
	 */
	public JSContext(final JSContextGroup inGroup, Class<?> iface) throws JSException {
        mWorker = new JSContextWorker();
        context = this;
        sync(new Runnable() {
            @Override public void run() {
                ctx = createInGroup(inGroup.groupRef());
                valueRef = getGlobalObject(ctx);
            }
        });
		this.initJSInterface(this, iface, null);
	}
	@Override
	protected void finalize() throws Throwable {
		if (ctx!=null) {
            release(ctx);
            finalizeContext(ctx);
		}
		super.finalize();
	}

	/**
	 * Sets the JS exception handler for this context.  Any thrown JSException in this
	 * context will call the 'handle' method on this object.  The calling function will
	 * return with an undefined value.
	 * @param handler An object that implements 'IJSExceptionHandler'
	 * @since 2.1
	 */
	public void setExceptionHandler(IJSExceptionHandler handler) {
		exceptionHandler = handler;
	}

	/**
	 * Clears a previously set exception handler.
	 * @since 2.1
	 */
	public void clearExceptionHandler() {
		exceptionHandler = null;
	}

	/**
	 * If an exception handler is set, calls the exception handler, otherwise throws
	 * the JSException.
	 * @param exception The JSException to be thrown
	 * @since 1.0
	 */
	public void throwJSException(JSException exception) throws JSException {
		if (exceptionHandler == null) {
			throw exception;
		} else {
            // Before handling this exception, disable the exception handler.  If a JSException
            // is thrown in the handler, then it would recurse and blow the stack.  This way an
            // actual exception will get thrown.  If successfully handled, then turn it back on.
            IJSExceptionHandler temp = exceptionHandler;
            exceptionHandler = null;
			temp.handle(exception);
            exceptionHandler = temp;
		}
	}
	
	/**
	 * Gets the context group to which this context belongs.
	 * @return  The context group to which this context belongs
	 */
	public JSContextGroup getGroup() {
		Long g = getGroup(ctx);
		if (g==null || g==0) return null;
		return new JSContextGroup(g);
	}
	
	/**
	 * Gets the JavaScriptCore context reference
	 * @return  the JavaScriptCore context reference
	 */
	public Long ctxRef() {
		return ctx;
	}

    private class JNIReturnClass implements Runnable {
        @Override
        public void run() {}
        JNIReturnObject jni;
    }

    /**
	 * Executes a the JavaScript code in 'script' in this context
	 * @param script  The code to execute
	 * @param thiz  The 'this' object
	 * @param sourceURL  The URI of the source file, only used for reporting in stack trace (optional)
	 * @param startingLineNumber  The beginning line number, only used for reporting in stack trace (optional)
	 * @return  The return value returned by 'script'
	 * @since 1.0
	 * @throws JSException
	 */
	public JSValue evaluateScript(final String script, final JSObject thiz,
			final String sourceURL, final int startingLineNumber) throws JSException {

        JNIReturnClass runnable = new JNIReturnClass() {
            @Override public void run() {
                jni = evaluateScript(ctx, new JSString(script).stringRef(),
                        (thiz == null) ? 0L : thiz.valueRef(), (sourceURL == null) ? 0L : new JSString(sourceURL).stringRef(),
                        startingLineNumber);
            }
        };
        sync(runnable);

		if (runnable.jni.exception!=0) {
			throwJSException(new JSException(new JSValue(runnable.jni.exception, context)));
			return new JSValue(this);
		}
		return new JSValue(runnable.jni.reference,this);
	}
	
	/**
	 * Executes a the JavaScript code in 'script' in this context
	 * @param script  The code to execute
	 * @param thiz  The 'this' object
	 * @return  The return value returned by 'script'
	 * @since 1.0
	 * @throws JSException
	 */
	public JSValue evaluateScript(String script, JSObject thiz) throws JSException {
		return evaluateScript(script,thiz,null,0);
	}
	/**
	 * Executes a the JavaScript code in 'script' in this context
	 * @param script  The code to execute
	 * @return  The return value returned by 'script'
	 * @since 1.0
	 * @throws JSException
	 */
	public JSValue evaluateScript(String script) throws JSException {
		return evaluateScript(script,null,null,0);
	}
	
	private Map<Long,JSObject> objects = new HashMap<Long,JSObject>();

	/**
	 * Keeps a reference to an object in this context.  This is used so that only one
	 * Java object instance wrapping a JavaScript object is maintained at any time.  This way,
	 * local variables in the Java object will stay wrapped around all returns of the same
	 * instance.  This is handled by JSObject, and should not need to be called by clients.
	 * @param obj  The object with which to associate with this context
	 * @since 1.0
	 */
	public synchronized  void persistObject(JSObject obj) {
		objects.put(obj.valueRef(), obj);
	}
	/**
	 * Removes a reference to an object in this context.  Should only be used from the 'finalize'
	 * object method.  This is handled by JSObject, and should not need to be called by clients.
	 * @param obj the JSObject to dereference
	 * @since 1.0
	 */
	public synchronized void finalizeObject(JSObject obj) {
        objects.remove(obj.valueRef());
	}
	/**
	 * Reuses a stored reference to a JavaScript object if it exists, otherwise, it creates the
	 * reference.
	 * @param objRef the JavaScriptCore object reference
	 * @since 1.0
	 * @return The JSObject representing the reference
	 */
	public synchronized JSObject getObjectFromRef(long objRef) {
		JSObject obj = objects.get(objRef);
		if (obj==null) {
			obj = new JSObject(objRef, this);
		}
		return obj;
	}
	/**
	 * Forces JavaScript garbage collection on this context
	 * @since 1.0
	 */
	public void garbageCollect()
    {
        async(new Runnable() {
            @Override
            public void run() {
                garbageCollect(ctx);
            }
        });
	}
	
	protected static native void staticInit();
    protected native long create();
	protected native long createInGroup(long group);
	protected native long retain(long ctx);
	protected native long release(long ctx);
	protected native long getGroup(long ctx);
	protected native long getGlobalObject(long ctx);
	protected native JNIReturnObject evaluateScript(long ctx, long script, long thisObject, long sourceURL, int startingLineNumber);
	protected native JNIReturnObject checkScriptSyntax(long ctx, long script, long sourceURL, int startingLineNumber);
	protected native void garbageCollect(long ctx);
    protected native void finalizeContext(long ctx);
	
	static {
		new JavaScriptCoreGTK(null);
		System.loadLibrary("android-js-core");
		staticInit();
	}
}
