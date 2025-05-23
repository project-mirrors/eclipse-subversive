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
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.data;

import java.io.ByteArrayInputStream;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.operation.UILoggedOperation;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * Local file revision representation. Wrapper for both IFileState and IFile.
 * 
 * @author Alexei Goncharov
 */
public class SVNLocalFileRevision extends LocalFileRevision implements IEditableContent {
	public SVNLocalFileRevision(IFile file) {
		super(file);
	}

	public SVNLocalFileRevision(IFileState fileState) {
		super(fileState);
	}

	@Override
	public String getComment() {
		if (getFile() != null) {
			return SVNUIMessages.SVNLocalFileRevision_CurrentVersion;
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getContentIdentifier() {
		if (getFile() != null) {
			return ""; //$NON-NLS-1$
		}
		return "[" + DateFormatter.formatDate(getTimestamp()) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean isEditable() {
		return isCurrentState();
	}

	@Override
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		return dest;
	}

	@Override
	public void setContent(byte[] newContent) {
		if (isEditable()) {
			try {
				getFile().setContents(new ByteArrayInputStream(newContent), true, true, new NullProgressMonitor());
			} catch (CoreException ex) {
				UILoggedOperation.reportError(SVNMessages.Operation_GetFileContent_SetContent, ex);
			}
		}
	}

}
