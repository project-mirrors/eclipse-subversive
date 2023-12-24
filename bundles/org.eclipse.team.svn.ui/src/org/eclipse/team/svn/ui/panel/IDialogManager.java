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
	int LEVEL_OK = 0;

	int LEVEL_WARNING = 1;

	int LEVEL_ERROR = 2;

	Shell getShell();

	void setButtonEnabled(int idx, boolean enabled);

	boolean isButtonEnabled(int idx);

	void setMessage(int level, String message);

	void forceClose(int buttonId);

}
