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
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * Local log UI node
 * 
 * @author Alexander Gurov
 */
public class LocalLogNode extends AbstractLogNode {
	protected SVNLocalFileRevision entry;

	public LocalLogNode(SVNLocalFileRevision entry, ILogNode parent) {
		super(parent);
		this.entry = entry;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IFileRevision.class)) {
			return entry;
		}
		return null;
	}

	@Override
	public ILogNode[] getChildren() {
		return null;
	}

	@Override
	public Object getEntity() {
		return entry;
	}

	@Override
	public boolean requiresBoldFont(long currentRevision) {
		return entry.isCurrentState();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/local_rev.gif"); //$NON-NLS-1$
	}

	@Override
	public String getLabel(int columnIndex, int labelType, long currentRevision) {
		switch (columnIndex) {
			case ILogNode.COLUMN_DATE: {
				return DateFormatter.formatDate(entry.getTimestamp());
			}
			case ILogNode.COLUMN_REVISION: {
				if (entry.isCurrentState()) {
					if (currentRevision != SVNRevision.INVALID_REVISION_NUMBER) {
						return BaseMessages.format(SVNUIMessages.LogMessagesComposite_CurrentRevision,
								new String[] { String.valueOf(currentRevision) });
					}
					return "*"; //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}
			case ILogNode.COLUMN_COMMENT: {
				String retVal = entry.getComment();
				if (labelType == ILogNode.LABEL_TRIM) {
					return FileUtility.formatMultilineText(retVal);
				} else if (labelType == ILogNode.LABEL_FLAT) {
					return AbstractLogNode.flattenMultiLineText(retVal, " "); //$NON-NLS-1$
				}
				return retVal;
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public int getType() {
		return ILogNode.TYPE_LOCAL;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public String getAuthor() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public int getChangesCount() {
		return 0;
	}

	@Override
	public String getComment() {
		return entry.getComment();
	}

	@Override
	public long getRevision() {
		return SVNRevision.INVALID_REVISION_NUMBER;
	}

	@Override
	public long getTimeStamp() {
		return entry.getTimestamp();
	}

	@Override
	public int hashCode() {
		return entry.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocalLogNode) {
			return entry.equals(((LocalLogNode) obj).entry);
		}
		return false;
	}

}
