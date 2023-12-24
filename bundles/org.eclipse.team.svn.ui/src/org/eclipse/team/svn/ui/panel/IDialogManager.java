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
 *    Alexander Gurov - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel;

import org.eclipse.swt.widgets.Shell;

/**
 * Dialog management interface
 * 
 * @author Alexander Gurov
 */
public interface IDialogManager {
    public static final int LEVEL_OK = 0;
    public static final int LEVEL_WARNING = 1;
    public static final int LEVEL_ERROR = 2;
    
    public Shell getShell();
    
    public void setButtonEnabled(int idx, boolean enabled);
    public boolean isButtonEnabled(int idx);
    public void setMessage(int level, String message);
    
    public void forceClose(int buttonId);
    
}
