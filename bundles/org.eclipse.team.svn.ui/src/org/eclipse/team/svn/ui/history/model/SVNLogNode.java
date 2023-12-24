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
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * SVN log entry UI node
 * 
 * @author Alexander Gurov
 */
public class SVNLogNode extends AbstractLogNode {
	protected SVNLogEntry entry;

	public SVNLogNode(SVNLogEntry entry, ILogNode parent) {
		super(parent);
		this.entry = entry;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(SVNLogEntry.class)) {
			return entry;
		}
		return null;
	}

	@Override
	public ILogNode[] getChildren() {
		SVNLogEntry[] entries = entry.getChildren();
		ILogNode[] children = new ILogNode[entries.length];
		for (int i = 0; i < entries.length; i++) {
			children[i] = new SVNLogNode(entries[i], this);
		}
		return children;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return SVNTeamUIPlugin.instance()
				.getImageDescriptor(parent instanceof SVNLogNode
						? "icons/objects/repository-gray.gif" //$NON-NLS-1$
						: "icons/objects/repository.gif"); //$NON-NLS-1$
	}

	@Override
	public boolean requiresBoldFont(long currentRevision) {
		return currentRevision != SVNRevision.INVALID_REVISION_NUMBER && entry.revision == currentRevision;
	}

	@Override
	public String getLabel(int columnIndex, int labelType, long currentRevision) {
		switch (columnIndex) {
			case ILogNode.COLUMN_REVISION: {
				String retVal = String.valueOf(entry.revision);
				if (currentRevision == entry.revision) {
					retVal = "*" + retVal; //$NON-NLS-1$
				}
				return retVal;
			}
			case ILogNode.COLUMN_DATE: {
				return entry.date == 0 ? SVNMessages.SVNInfo_NoDate : DateFormatter.formatDate(entry.date);
			}
			case ILogNode.COLUMN_CHANGES: {
				return String.valueOf(entry.changedPaths != null ? entry.changedPaths.length : 0);
			}
			case ILogNode.COLUMN_AUTHOR: {
				return entry.author == null || entry.author.length() == 0 ? SVNMessages.SVNInfo_NoAuthor : entry.author;
			}
			case ILogNode.COLUMN_COMMENT: {
				String retVal = entry.message;
				if (retVal == null || retVal.length() == 0) {
					return SVNMessages.SVNInfo_NoComment;
				}
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
	public Object getEntity() {
		return entry;
	}

	@Override
	public int getType() {
		return ILogNode.TYPE_SVN;
	}

	@Override
	public boolean hasChildren() {
		return entry.hasChildren();
	}

	@Override
	public String getAuthor() {
		return entry.author;
	}

	@Override
	public int getChangesCount() {
		return entry.changedPaths == null ? 0 : entry.changedPaths.length;
	}

	@Override
	public String getComment() {
		return entry.message;
	}

	@Override
	public long getRevision() {
		return entry.revision;
	}

	@Override
	public long getTimeStamp() {
		return entry.date;
	}

	@Override
	public int hashCode() {
		return (int) entry.revision;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SVNLogNode) {
			return entry.revision == ((SVNLogNode) obj).entry.revision;
		}
		return false;
	}

}
