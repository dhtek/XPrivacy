package biz.bokhorst.xprivacy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.saurik.substrate.MS;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class XPrivacy implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private static boolean mCydia = false;
	private static String mSecret = null;
	private static List<String> mListHookError = new ArrayList<String>();

	// http://developer.android.com/reference/android/Manifest.permission.html

	// Xposed
	public void initZygote(StartupParam startupParam) throws Throwable {
		Util.log(null, Log.WARN, String.format("Load %s", startupParam.modulePath));

		// Check for LBE security master
		if (Util.hasLBE()) {
			Util.log(null, Log.ERROR, "LBE installed");
			return;
		}

		init();
	}

	// Xposed
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		// Check for LBE security master
		if (Util.hasLBE())
			return;

		// Log load
		Util.log(null, Log.INFO, String.format("Load package=%s uid=%d", lpparam.packageName, Process.myUid()));

		handleLoadPackage(lpparam.packageName, lpparam.classLoader, mSecret);
	}

	// Cydia
	public static void initialize() {
		mCydia = true;
		init();

		// Self
		MS.hookClassLoad(Util.class.getName(), new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> clazz) {
				hookAll(XUtilHook.getInstances(), clazz.getClassLoader(), mSecret);
			}
		});

		// TODO: Cydia: Build.SERIAL
		// TODO: Cydia: android.provider.Settings.Secure

		// Advertising Id
		MS.hookClassLoad("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info", new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> clazz) {
				hookAll(XAdvertisingIdClientInfo.getInstances(), clazz.getClassLoader(), mSecret);
			}
		});

		// User activity
		MS.hookClassLoad("com.google.android.gms.location.ActivityRecognitionClient", new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> clazz) {
				hookAll(XActivityRecognitionClient.getInstances(), clazz.getClassLoader(), mSecret);
			}
		});

		// Google auth
		MS.hookClassLoad("com.google.android.gms.auth.GoogleAuthUtil", new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> clazz) {
				hookAll(XGoogleAuthUtil.getInstances(), clazz.getClassLoader(), mSecret);
			}
		});

		// Location client
		MS.hookClassLoad("com.google.android.gms.location.LocationClient", new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> clazz) {
				hookAll(XLocationClient.getInstances(), clazz.getClassLoader(), mSecret);
			}
		});
	}

	public static void init() {
		// Generate secret
		mSecret = Long.toHexString(new Random().nextLong());

		// System server
		try {
			// frameworks/base/services/java/com/android/server/SystemServer.java
			Class<?> cSystemServer = Class.forName("com.android.server.SystemServer");
			Method mMain = cSystemServer.getDeclaredMethod("main", String[].class);
			if (mCydia)
				MS.hookMethod(cSystemServer, mMain, new MS.MethodAlteration<Object, Void>() {
					@Override
					public Void invoked(Object thiz, Object... args) throws Throwable {
						PrivacyService.register(mListHookError, mSecret);
						return invoke(thiz, args);
					}
				});
			else
				XposedBridge.hookMethod(mMain, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						PrivacyService.register(mListHookError, mSecret);
					}
				});
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}

		// Activity manager service
		hookAll(XActivityManagerService.getInstances(), mSecret);

		// App widget manager
		hookAll(XAppWidgetManager.getInstances(), mSecret);

		// Application
		hookAll(XApplication.getInstances(), mSecret);

		// Audio record
		hookAll(XAudioRecord.getInstances(), mSecret);

		// Binder device
		hookAll(XBinder.getInstances(), mSecret);

		// Bluetooth adapater
		hookAll(XBluetoothAdapter.getInstances(), mSecret);

		// Bluetooth device
		hookAll(XBluetoothDevice.getInstances(), mSecret);

		// Camera
		hookAll(XCamera.getInstances(), mSecret);

		// Content resolver
		hookAll(XContentResolver.getInstances(), mSecret);

		// Context wrapper
		hookAll(XContextImpl.getInstances(), mSecret);

		// Environment
		hookAll(XEnvironment.getInstances(), mSecret);

		// InetAddress
		hookAll(XInetAddress.getInstances(), mSecret);

		// InputDevice
		hookAll(XInputDevice.getInstances(), mSecret);

		// IO bridge
		hookAll(XIoBridge.getInstances(), mSecret);

		// Media recorder
		hookAll(XMediaRecorder.getInstances(), mSecret);

		// Network info
		hookAll(XNetworkInfo.getInstances(), mSecret);

		// Network interface
		hookAll(XNetworkInterface.getInstances(), mSecret);

		// NFC adapter
		hookAll(XNfcAdapter.getInstances(), mSecret);

		// Package manager service
		hookAll(XProcess.getInstances(), mSecret);

		// Process builder
		hookAll(XProcessBuilder.getInstances(), mSecret);

		// Resources
		hookAll(XResources.getInstances(), mSecret);

		// Runtime
		hookAll(XRuntime.getInstances(), mSecret);

		// Settings secure
		if (!mCydia)
			hookAll(XSettingsSecure.getInstances(), mSecret);

		// SIP manager
		hookAll(XSipManager.getInstances(), mSecret);

		// SMS manager
		hookAll(XSmsManager.getInstances(), mSecret);

		// System properties
		hookAll(XSystemProperties.getInstances(), mSecret);

		// Web view
		hookAll(XWebView.getInstances(), mSecret);

		// Intent receive
		hookAll(XActivityThread.getInstances(), mSecret);

		// Intent send
		hookAll(XActivity.getInstances(), mSecret);
	}

	public static void handleLoadPackage(String packageName, ClassLoader classLoader, String secret) {
		// Skip hooking self
		String self = XPrivacy.class.getPackage().getName();
		if (packageName.equals(self)) {
			hookAll(XUtilHook.getInstances(), classLoader, secret, true);
			return;
		}

		// Build SERIAL
		if (PrivacyManager.getRestriction(null, Process.myUid(), PrivacyManager.cIdentification, "SERIAL", secret))
			try {
				Field serial = Build.class.getField("SERIAL");
				serial.setAccessible(true);
				serial.set(null, PrivacyManager.getDefacedProp(Process.myUid(), "SERIAL"));
			} catch (Throwable ex) {
				Util.bug(null, ex);
			}

		// Advertising Id
		try {
			Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info", false, classLoader);
			hookAll(XAdvertisingIdClientInfo.getInstances(), classLoader, secret, true);
		} catch (Throwable ignored) {
		}

		// User activity
		try {
			Class.forName("com.google.android.gms.location.ActivityRecognitionClient", false, classLoader);
			hookAll(XActivityRecognitionClient.getInstances(), classLoader, secret, true);
		} catch (Throwable ignored) {
		}

		// Google auth
		try {
			Class.forName("com.google.android.gms.auth.GoogleAuthUtil", false, classLoader);
			hookAll(XGoogleAuthUtil.getInstances(), classLoader, secret, true);
		} catch (Throwable ignored) {
		}

		// Location client
		try {
			Class.forName("com.google.android.gms.location.LocationClient", false, classLoader);
			hookAll(XLocationClient.getInstances(), classLoader, secret, true);
		} catch (Throwable ignored) {
		}
	}

	public static void handleGetSystemService(String name, String className, String secret) {
		Util.log(null, Log.INFO, "getSystemService " + name + "=" + className + " uid=" + Binder.getCallingUid());

		if (name.equals(Context.ACCOUNT_SERVICE))
			hookAll(XAccountManager.getInstances(className), secret, true);
		else if (name.equals(Context.ACTIVITY_SERVICE))
			hookAll(XActivityManager.getInstances(className), secret, true);
		else if (name.equals(Context.CLIPBOARD_SERVICE))
			hookAll(XClipboardManager.getInstances(className), secret, true);
		else if (name.equals(Context.CONNECTIVITY_SERVICE))
			hookAll(XConnectivityManager.getInstances(className), secret, true);
		else if (name.equals(Context.LOCATION_SERVICE))
			hookAll(XLocationManager.getInstances(className), secret, true);
		else if (name.equals("PackageManager"))
			hookAll(XPackageManager.getInstances(className), secret, true);
		else if (name.equals(Context.SENSOR_SERVICE))
			hookAll(XSensorManager.getInstances(className), secret, true);
		else if (name.equals(Context.TELEPHONY_SERVICE))
			hookAll(XTelephonyManager.getInstances(className), secret, true);
		else if (name.equals(Context.WINDOW_SERVICE))
			hookAll(XWindowManager.getInstances(className), secret, true);
		else if (name.equals(Context.WIFI_SERVICE))
			hookAll(XWifiManager.getInstances(className), secret, true);
	}

	public static void hookAll(List<XHook> listHook, String secret) {
		for (XHook hook : listHook)
			hook(hook, null, secret, false);
	}

	public static void hookAll(List<XHook> listHook, String secret, boolean filter) {
		for (XHook hook : listHook)
			hook(hook, null, secret, filter);
	}

	public static void hookAll(List<XHook> listHook, ClassLoader classLoader, String secret) {
		for (XHook hook : listHook)
			hook(hook, classLoader, secret, false);
	}

	public static void hookAll(List<XHook> listHook, ClassLoader classLoader, String secret, boolean filter) {
		for (XHook hook : listHook)
			hook(hook, classLoader, secret, filter);
	}

	private static void hook(final XHook hook, ClassLoader classLoader, String secret, boolean filter) {
		// Check SDK version
		Hook md = null;
		String message = null;
		if (hook.getRestrictionName() == null) {
			if (hook.getSdk() == 0)
				message = "No SDK specified for " + hook;
		} else {
			md = PrivacyManager.getHook(hook.getRestrictionName(), hook.getSpecifier());
			if (md == null)
				message = "Hook not found " + hook;
			else if (hook.getSdk() != 0)
				message = "SDK not expected for " + hook;
		}
		if (message != null) {
			mListHookError.add(message);
			Util.log(hook, Log.ERROR, message);
		}

		if (secret == null)
			Util.log(hook, Log.ERROR, "Secret missing for " + hook);

		int sdk = 0;
		if (hook.getRestrictionName() == null)
			sdk = hook.getSdk();
		else if (md != null)
			sdk = md.getSdk();

		if (Build.VERSION.SDK_INT < sdk)
			return;

		// Provide secret
		hook.setSecret(secret);

		try {
			// Create hook method
			XC_MethodHook methodHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (Process.myUid() <= 0)
							return;
						XParam xparam = XParam.fromXposed(param);
						hook.before(xparam);
						if (xparam.hasResult())
							param.setResult(xparam.getResult());
						if (xparam.hasThrowable())
							param.setThrowable(xparam.getThrowable());
						param.setObjectExtra("xextra", xparam.getExtras());
					} catch (Throwable ex) {
						Util.bug(null, ex);
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!param.hasThrowable())
						try {
							if (Process.myUid() <= 0)
								return;
							XParam xparam = XParam.fromXposed(param);
							xparam.setExtras(param.getObjectExtra("xextra"));
							hook.after(xparam);
							if (xparam.hasResult())
								param.setResult(xparam.getResult());
							if (xparam.hasThrowable())
								param.setThrowable(xparam.getThrowable());
						} catch (Throwable ex) {
							Util.bug(null, ex);
						}
				}
			};

			// Find class
			Class<?> hookClass = null;
			try {
				if (mCydia)
					hookClass = Class.forName(hook.getClassName(), false, classLoader);
				else
					hookClass = findClass(hook.getClassName(), classLoader);
			} catch (Throwable ex) {
				message = String.format("Class not found for %s", hook);
				mListHookError.add(message);
				Util.log(hook, hook.isOptional() ? Log.WARN : Log.ERROR, message);
			}

			// Get members
			List<Member> listMember = new ArrayList<Member>();
			Class<?> clazz = hookClass;
			while (clazz != null) {
				if (hook.getMethodName() == null) {
					for (Constructor<?> constructor : clazz.getDeclaredConstructors())
						if (Modifier.isPublic(constructor.getModifiers()) ? hook.isVisible() : !hook.isVisible())
							listMember.add(constructor);
					break;
				} else {
					for (Method method : clazz.getDeclaredMethods())
						if (method.getName().equals(hook.getMethodName())
								&& !Modifier.isAbstract(method.getModifiers())
								&& (Modifier.isPublic(method.getModifiers()) ? hook.isVisible() : !hook.isVisible()))
							listMember.add(method);
				}
				clazz = clazz.getSuperclass();
			}

			// Hook members
			for (final Member member : listMember)
				try {
					if (Modifier.isAbstract(member.getModifiers()))
						Util.log(hook, Log.ERROR, String.format("Abstract: %s", member));
					else if (!filter || !Modifier.isNative(member.getModifiers()))
						if (mCydia)
							if (member instanceof Method)
								MS.hookMethod(member.getDeclaringClass(), (Method) member, new MethodAlterationEx(hook,
										member));
							else
								MS.hookMethod(member.getDeclaringClass(), (Constructor<?>) member,
										new MethodAlterationEx(hook, member));
						else
							XposedBridge.hookMethod(member, methodHook);
				} catch (NoSuchFieldError ex) {
					Util.log(hook, Log.WARN, ex.toString());
				} catch (Throwable ex) {
					mListHookError.add(ex.toString());
					Util.bug(hook, ex);
				}

			// Check if members found
			if (listMember.isEmpty() && !hook.getClassName().startsWith("com.google.android.gms")) {
				message = String.format("Method not found for %s", hook);
				if (!hook.isOptional())
					mListHookError.add(message);
				Util.log(hook, hook.isOptional() ? Log.WARN : Log.ERROR, message);
			}
		} catch (Throwable ex) {
			mListHookError.add(ex.toString());
			Util.bug(hook, ex);
		}
	}

	public static class MethodAlterationEx extends MS.MethodAlteration<Object, Object> {
		private XHook mHook;
		private Member mMember;

		public MethodAlterationEx(XHook hook, Member member) {
			mHook = hook;
			mMember = member;
		}

		@Override
		public Object invoked(Object thiz, Object... args) throws Throwable {
			if (Process.myUid() <= 0)
				return invoke(thiz, args);

			XParam xparam = XParam.fromCydia(mMember, thiz, args);
			mHook.before(xparam);

			if (!xparam.hasResult() || xparam.hasThrowable()) {
				try {
					Object result = invoke(thiz, args);
					xparam.setResult(result);
				} catch (Throwable ex) {
					xparam.setThrowable(ex);
				}

				mHook.after(xparam);
			}

			if (xparam.hasThrowable())
				throw xparam.getThrowable();
			return xparam.getResult();
		}
	}
}
