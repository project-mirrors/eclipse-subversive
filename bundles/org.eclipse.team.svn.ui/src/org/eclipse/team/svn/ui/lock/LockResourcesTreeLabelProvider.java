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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.lock;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * Tree label provider for lock resources
 * 
 * @author Igor Burilo
 */
public class LockResourcesTreeLabelProvider extends LabelProvider {

	protected static Image rootIcon;
	protected static Image folderIcon;
	
	public LockResourcesTreeLabelProvider() {
		synchronized (LockResourcesTreeLabelProvider.class) {
			if (LockResourcesTreeLabelProvider.folderIcon == null) {
				SVNTeamUIPlugin instance = SVNTeamUIPlugin.instance();				
				LockResourcesTreeLabelProvider.folderIcon = instance.getImageDescriptor("icons/views/history/affected_folder.gif").createImage(); //$NON-NLS-1$
				LockResourcesTreeLabelProvider.rootIcon = instance.getImageDescriptor("icons/objects/repository-root.gif").createImage(); //$NON-NLS-1$
				CompareUI.disposeOnShutdown(LockResourcesTreeLabelProvider.folderIcon);
				CompareUI.disposeOnShutdown(LockResourcesTreeLabelProvider.rootIcon);
			}
		}
	}
	
	public Image getImage(Object element) {
		LockResource node = (LockResource) element;
		if (node.isRoot()) {
			return LockResourcesTreeLabelProvider.rootIcon;
		} else {
			return LockResourcesTreeLabelProvider.folderIcon;
		}		
	}

	public String getText(Object element) {
		LockResource node = (LockResource) element; 
		return node.getName();
	}
}
