/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.ui.discovery;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.team.svn.ui.discovery.messages"; //$NON-NLS-1$
	
	public static String WorkbenchUtil_Browser_Initialization_Failed;
	
	public static String WorkbenchUtil_Open_Location_Title;
	
	public static String WorkbenchUtil_No_URL_Error;
	
	public static String WorkbenchUtil_Invalid_URL_Error;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
