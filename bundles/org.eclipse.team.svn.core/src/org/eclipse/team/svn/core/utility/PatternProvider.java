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
 *    Gabor Liptak - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.utility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pattern cache, and replacement method instead of String.replaceAll, wich is horribly slow.
 * 
 * @author Gabor Liptak
 */
public final class PatternProvider {
	private static int MAX_CACHE_SIZE = 100;

	private static LinkedHashMap<String, Pattern> patterns = new LinkedHashMap<>() {
		private static final long serialVersionUID = 2921759287651173337L;

		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > PatternProvider.MAX_CACHE_SIZE;
		}
	};

	public static String replaceAll(String strSource, String strPattern, String strReplacement) {
		return PatternProvider.getPattern(strPattern).matcher(strSource).replaceAll(strReplacement);
	}

	public static synchronized Pattern getPattern(String strPattern) {
		Pattern patternReturn = PatternProvider.patterns.get(strPattern);

		//if two threads would need the same new pattern in the same time, only one will compile it
		if (patternReturn == null) {
			patternReturn = Pattern.compile(strPattern);
			PatternProvider.patterns.put(strPattern, patternReturn);
		}
		return patternReturn;
	}

	private PatternProvider() {
	}
}
