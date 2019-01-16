package fr.mad.ImageUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.util.Map;

public class Crypto {
	public static void removeCryptographyRestrictions() {
		if (!isRestrictedCryptography()) {
			Security.setProperty("crypto.policy", "unlimited");
			return;
		}
		try {
			/*
			 * Do the following, but with reflection to bypass access checks:
			 *
			 * JceSecurity.isRestricted = false;
			 * JceSecurity.defaultPolicy.perms.clear();
			 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
			 */
			final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
			final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
			final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
			
			final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
			isRestrictedField.setAccessible(true);
			final Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
			isRestrictedField.set(null, false);
			
			final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
			defaultPolicyField.setAccessible(true);
			final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
			
			final Field perms = cryptoPermissions.getDeclaredField("perms");
			perms.setAccessible(true);
			((Map<?, ?>) perms.get(defaultPolicy)).clear();
			
			final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
			instance.setAccessible(true);
			defaultPolicy.add((Permission) instance.get(null));
		} catch (final Exception e) {
			throw new Error(e);
		}
	}
	
	private static boolean isRestrictedCryptography() {
		final String name = System.getProperty("java.runtime.name");
		final String ver = System.getProperty("java.version");
		boolean needHack = name != null && name.equals("Java(TM) SE Runtime Environment") && ver != null && (ver.startsWith("1.7") || ver.startsWith("1.8"));
		return needHack;
	}
}
