/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

public class BaseMessages extends NLS {

	public static String format(String translatedString, Object arg) {
		return BaseMessages.format(translatedString, new Object[] { arg });
	}

	public static String format(String translatedString, Object[] args) {
		return NLS.bind(translatedString, args);
	}

	/**
	 * Gets a resource string by field name. This is useful when the field name is constructed ad hoc.
	 * 
	 * @param key
	 * @return
	 */
	public static String getString(String key, Class clazz) {
		if ((key == null) || (clazz == null)) {
			return null;
		}

		String str = BaseMessages.getRawString(key, clazz);
		if (str == null) {
			//System.err.println("Unknown key: " + key + ", class: " + clazz.getName());
			str = key;
		}
		return str;
	}

	protected static boolean hasString(String key, Class clazz) {
		return BaseMessages.getRawString(key, clazz) != null;
	}

	protected static String getRawString(String key, Class clazz) {
		String res = null;

		/*
		 * if key contains not valid characters for java identifier
		 * then replace not valid characters to underscore
		 */
		if (key.indexOf("-") != -1) { //$NON-NLS-1$
			key = key.replace('-', '_'); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try {
			Field field = clazz.getDeclaredField(key);
			res = (String) field.get(null);
		} catch (Exception e) {
			res = null;
		}
		return res;
	}

	/**
	 * Convenience method used for error messages, where for message we also add its error identifier
	 * 
	 * @param key
	 * @return
	 */
	public static String getErrorString(String key, Class clazz) {
		String retVal = BaseMessages.getString(key, clazz);
		if (BaseMessages.hasString(key, clazz)) {
			if (key.startsWith("Error_")) { //$NON-NLS-1$
				String idKey = key + "_Id"; //$NON-NLS-1$
				if (BaseMessages.hasString(idKey, clazz)) {
					retVal = BaseMessages.getString(idKey, clazz) + ": " + retVal; //$NON-NLS-1$
				}
			}
		}
		return retVal;
	}

	public static String formatErrorString(String key, Object[] args, Class clazz) {
		String retVal = BaseMessages.getErrorString(key, clazz);
		return BaseMessages.format(retVal, args);
	}
}
