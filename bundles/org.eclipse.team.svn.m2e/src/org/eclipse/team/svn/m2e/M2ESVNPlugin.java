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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.m2e;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Alexander Gurov
 */
public class M2ESVNPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.polarion.eclipse.team.svn.m2eclipse";

	private static M2ESVNPlugin instance;
	
	public M2ESVNPlugin() {
		super();
		M2ESVNPlugin.instance = this;
	}
	
	public static M2ESVNPlugin instance() {
    	return M2ESVNPlugin.instance;
	}

    public String getResource(String key) {
        return FileUtility.getResource(Platform.getResourceBundle(this.getBundle()), key);
    }
    
    public String getResource(String key, Object []args) {
        String message = this.getResource(key);
        return MessageFormat.format(message, args);
    }
    
}
