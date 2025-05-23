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

package org.eclipse.team.svn.ui.history.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.history.data.HistoryCategory;
import org.eclipse.team.svn.ui.history.data.RootHistoryCategory;
import org.eclipse.team.svn.ui.history.data.SVNLocalFileRevision;

/**
 * History View category UI node
 * 
 * @author Alexander Gurov
 */
public class CategoryLogNode extends AbstractLogNode {
	protected HistoryCategory category;

	public CategoryLogNode(RootHistoryCategory category) {
		this(category, null);
	}

	public CategoryLogNode(HistoryCategory category, ILogNode parent) {
		super(parent);
		this.category = category;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(HistoryCategory.class)) {
			return category;
		}
		return null;
	}

	@Override
	public ILogNode[] getChildren() {
		Object[] entries = category.getEntries();
		ILogNode[] children = new ILogNode[entries.length];
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] instanceof SVNLogEntry) {
				children[i] = new SVNLogNode((SVNLogEntry) entries[i], this);
			} else if (entries[i] instanceof SVNLocalFileRevision) {
				children[i] = new LocalLogNode((SVNLocalFileRevision) entries[i], this);
			} else if (entries[i] instanceof HistoryCategory) {
				children[i] = new CategoryLogNode((HistoryCategory) entries[i], this);
			} else if (entries[i] instanceof String) {
				children[i] = new PlainTextLogNode((String) entries[i], this);
			}
		}
		return children;
	}

	@Override
	public Object getEntity() {
		return category;
	}

	@Override
	public boolean requiresBoldFont(long currentRevision) {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/group_by_date.gif"); //$NON-NLS-1$
	}

	@Override
	public String getLabel(int columnIndex, int labelType, long currentRevision) {
		if (columnIndex == ILogNode.COLUMN_REVISION) {
			return category.getName();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public int getType() {
		return ILogNode.TYPE_CATEGORY;
	}

	@Override
	public boolean hasChildren() {
		return true;
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
		return ""; //$NON-NLS-1$
	}

	@Override
	public long getRevision() {
		return SVNRevision.INVALID_REVISION_NUMBER;
	}

	@Override
	public long getTimeStamp() {
		return 0;
	}

	@Override
	public int hashCode() {
		return category.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CategoryLogNode) {
			return category.equals(((CategoryLogNode) obj).category);
		}
		return false;
	}

}
