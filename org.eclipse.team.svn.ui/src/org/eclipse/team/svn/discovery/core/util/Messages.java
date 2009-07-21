/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.discovery.core.util;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author David Green
 * @author Igor Burilo
 */
class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.team.svn.discovery.core.util.messages"; //$NON-NLS-1$

	public static String WebUtil_cannotDownload;

	public static String WebUtil_task_retrievingUrl;

	public static String WebUtil_task_verifyingUrl;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
