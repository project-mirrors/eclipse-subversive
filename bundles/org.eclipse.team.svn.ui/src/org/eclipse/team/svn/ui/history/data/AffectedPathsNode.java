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
 *    Sergiy Logvin - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.svn.core.connector.SVNLogPath;

/**
 * Node for the tree of the affected paths
 *
 * @author Sergiy Logvin
 */
public class AffectedPathsNode {
	protected String name;

	protected String compressedName;

	protected List<AffectedPathsNode> children;

	protected AffectedPathsNode parent;

	protected ArrayList<SVNChangedPathData> data;

	protected SVNLogPath.ChangeType status;

	public AffectedPathsNode(String name, AffectedPathsNode parent, SVNLogPath.ChangeType status) {
		this.name = compressedName = name;
		this.parent = parent;
		data = new ArrayList<>();
		children = new ArrayList<>();
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public String getCompressedName() {
		return compressedName;
	}

	public void addCompressedNameSegment(String compressedName) {
		this.compressedName += "/" + compressedName; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return compressedName;
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	public AffectedPathsNode getParent() {
		return parent;
	}

	public List<AffectedPathsNode> getChildren() {
		return children;
	}

	public boolean addChild(AffectedPathsNode child) {
		if (children.contains(child)) {
			return false;
		}
		return children.add(child);
	}

	public boolean removeChild(AffectedPathsNode child) {
		if (children.contains(child)) {
			return children.remove(child);
		}
		return false;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof AffectedPathsNode) {
			AffectedPathsNode node2 = (AffectedPathsNode) arg0;
			if (parent == null) {
				return node2.parent == null;
			}
			if (parent.equals(node2.parent) && name.equals(node2.name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int h = 17;
		h += 31 * (parent != null ? parent.hashCode() : 0);
		h += 31 * name.hashCode();
		return h;
	}

	public SVNChangedPathData[] getData() {
		return data.toArray(new SVNChangedPathData[data.size()]);
	}

	protected List<SVNChangedPathData> getPathDataImpl(List<SVNChangedPathData> result) {
		result.addAll(data);
		for (AffectedPathsNode node : children) {
			node.getPathDataImpl(result);
		}
		return result;
	}

	public void addData(SVNChangedPathData data) {
		if (!this.data.contains(data)) {
			this.data.add(data);
		}
	}

	public SVNChangedPathData[] getPathData() {
		List<SVNChangedPathData> tmp = getPathDataImpl(new ArrayList<>());
		return tmp.toArray(new SVNChangedPathData[tmp.size()]);
	}

	public void setParent(AffectedPathsNode parent) {
		this.parent = parent;
	}

	public void setChildren(List<AffectedPathsNode> children) {
		if (children != null) {
			this.children = children;
		} else {
			this.children.clear();
		}
	}

	public String getFullPath() {
		return parent != null ? parent.getFullPath() + "/" + compressedName : ""; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public SVNLogPath.ChangeType getStatus() {
		return status;
	}

	public void setStatus(SVNLogPath.ChangeType status) {
		this.status = status;
	}

}
