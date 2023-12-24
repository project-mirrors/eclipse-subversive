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
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.history.data.RootHistoryCategory;

/**
 * Plain text nodes for History View
 * 
 * @author Alexander Gurov
 */
public class PlainTextLogNode extends AbstractLogNode {
	protected String label;

	public PlainTextLogNode(String label, ILogNode parent) {
		super(parent);
		this.label = label;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(String.class)) {
			return label;
		}
		return null;
	}

	@Override
	public int getType() {
		return ILogNode.TYPE_NONE;
	}

	@Override
	public Object getEntity() {
		return label;
	}

	@Override
	public boolean requiresBoldFont(long currentRevision) {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (label.equals(RootHistoryCategory.PENDING[0])) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/repositories/browser_pending.gif"); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String getLabel(int columnIndex, int labelType, long currentRevision) {
		if (columnIndex == ILogNode.COLUMN_REVISION) {
			return label;
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public ILogNode[] getChildren() {
		return null;
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
		return label.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PlainTextLogNode) {
			return label.equals(((PlainTextLogNode) obj).label);
		}
		return false;
	}

}