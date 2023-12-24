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

package org.eclipse.team.svn.ui.dialog;

import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;

/**
 * Node Kind changed notification dialog
 * 
 * @author Alexander Gurov
 */
public class NotifyNodeKindChangedDialog extends MessageDialog {

	public NotifyNodeKindChangedDialog(Shell parentShell, IResource[] resources) {
		super(parentShell, SVNUIMessages.NotifyNodeKindChangedDialog_Title, null,
				BaseMessages.format(SVNUIMessages.NotifyNodeKindChangedDialog_Message,
						new String[] { NotifyNodeKindChangedDialog.enumerateParents(resources) }),
				MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0);
	}

	protected static String enumerateParents(IResource[] resources) {
		HashSet<IContainer> parents = new HashSet<>();
		for (IResource element : resources) {
			parents.add(element.getParent());
		}
		resources = parents.toArray(new IResource[parents.size()]);
		FileUtility.reorder(resources, true);
		String retVal = ""; //$NON-NLS-1$
		for (IResource element : resources) {
			retVal += "'" + element.getFullPath().toString() + "'\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return retVal;
	}

}
