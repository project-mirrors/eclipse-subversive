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

package org.eclipse.team.svn.ui.history;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;

/**
 * Content provider for the affected paths tree
 *
 * @author Sergiy Logvin
 */
public class AffectedPathsContentProvider implements ITreeContentProvider {
	protected AffectedPathsNode root;

	public void initialize(SVNChangedPathData[] affectedPaths, Collection<String> relatedPathPrefixes,
			Collection<String> relatedParents, long currentRevision) {
		root = new AffectedPathsNode(SVNUIMessages.AffectedPathsContentProvider_RootName, null, null);
		if (affectedPaths == null) {
			return;
		}

		for (SVNChangedPathData row : affectedPaths) {
			processPath(row, relatedPathPrefixes, relatedParents);
		}

		for (AffectedPathsNode node : root.getChildren()) {
			doCompress(node);
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		AffectedPathsNode node = (AffectedPathsNode) element;
		return node.hasChildren();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		AffectedPathsNode parentNode = (AffectedPathsNode) parentElement;
		return parentNode.getChildren().toArray(new AffectedPathsNode[parentNode.getChildren().size()]);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object getParent(Object element) {
		return ((AffectedPathsNode) element).getParent();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[] { root };
	}

	public AffectedPathsNode getRoot() {
		return root;
	}

	protected void processPath(SVNChangedPathData affectedPath, Collection<String> relatedPathPrefixes,
			Collection<String> relatedParents) {
		String fullResourcePath = affectedPath.getFullResourcePath();
		if (!isRelatedPath(fullResourcePath, relatedPathPrefixes)
				&& !isRelatedParent(fullResourcePath, relatedParents)) {
			return;
		}
		StringTokenizer st = new StringTokenizer(fullResourcePath, "/"); //$NON-NLS-1$
		AffectedPathsNode node = null;
		AffectedPathsNode parent = root;
		// also handle changes for repository root
		AffectedPathsNode nextToLast = root;
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			node = findByName(parent, name);
			if (node == null) {
				node = new AffectedPathsNode(name, parent,
						name.equals(affectedPath.resourceName) ? affectedPath.action : null);
				parent.addChild(node);
			} else if (!st.hasMoreTokens()) {
				node.setStatus(affectedPath.action);
			}
			nextToLast = parent;
			parent = node;
		}
		nextToLast.addData(affectedPath);
		if (node != null && (node.getChildren() == null || node.getChildren().size() == 0)
				&& (node.getPathData() == null || node.getPathData().length == 0)) {
			nextToLast.removeChild(node);
		}
	}

	protected AffectedPathsNode findByName(AffectedPathsNode parent, String name) {
		for (AffectedPathsNode node : parent.getChildren()) {
			if (node.getName().equals(name)) {
				return node;
			}
		}
		return null;
	}

	protected boolean isRelatedParent(String fullPath, Collection<String> relatedParents) {
		if (relatedParents == null || relatedParents.contains(fullPath)) {
			return true;
		}
		return false;
	}

	protected boolean isRelatedPath(String fullPath, Collection<String> relatedPathPrefixes) {
		if (relatedPathPrefixes == null) {
			return true;
		}
		for (String prefix : relatedPathPrefixes) {
			if (fullPath.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	protected void doCompress(AffectedPathsNode node) {
		List<AffectedPathsNode> children = node.getChildren();
		if (children.size() > 1) {
			for (AffectedPathsNode tNode : children) {
				doCompress(tNode);
			}
		} else if (children.size() == 1) {
			AffectedPathsNode child = children.get(0);

			if (node.getData().length > 0) {
				doCompress(child);
			} else {
				node.addCompressedNameSegment(child.getName());

				List<AffectedPathsNode> lowerChildren = child.getChildren();
				for (AffectedPathsNode tNode : lowerChildren) {
					tNode.setParent(node);
				}
				node.setChildren(lowerChildren);

				for (SVNChangedPathData data : child.getData()) {
					node.addData(data);
				}

				doCompress(node);
			}
		}
	}

}
