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
 *    Polarion Software
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.utility;

import java.util.Random;

public class StringId {

	public static final char ID_SEPARATOR = '-';

	private final static char[] FIRST_CHAR = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

	private final static char[] LETTERS_DIGITS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9' };

	public static String generateRandom(String prefix, int length) {
		if (prefix == null) {
			return generateRandom(length);
		}
		return prefix + ID_SEPARATOR + generateRandom(length);
	}

	public static String generateRandom(int length) {
		if (length == 0) {
			return new String();
		}

		Random random = new Random();
		StringBuilder str = new StringBuilder();

		str.append(FIRST_CHAR[random.nextInt(FIRST_CHAR.length)]);

		for (int i = 1; i < length; i++) {
			str.append(LETTERS_DIGITS[random.nextInt(LETTERS_DIGITS.length)]);
		}
		return str.toString();
	}

//	public static boolean isStringId(String strId) {
//		int pos = strId.lastIndexOf(ID_SEPARATOR);
//		if (pos != -1) {
//			strId = strId.substring(pos + 1);
//		}
//		for (int i = 0; i < strId.length(); i++) {
//			char ch = strId.charAt(i);
//			if (!Character.isDigit(ch) && !Character.isUpperCase(ch)) {
//				return false;
//			}
//		}
//		return true;
//	}

}
