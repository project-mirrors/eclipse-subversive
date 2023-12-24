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

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;

/**
 * Affected paths label provider
 *
 * @author Sergiy Logvin
 */
public class AffectedPathsLabelProvider extends LabelProvider {
	protected static Image folderIcon;

	protected static Image overlayedFolderIcon;

	protected static Image rootIcon;

	protected static Image rootAdditionIcon;

	protected static Image overlayedRootIcon;

	protected static Image addedFolderIcon;

	protected static Image modifiedFolderIcon;

	protected static Image deletedFolderIcon;

	protected static Image replacedFolderIcon;

	protected long currentRevision;

	public AffectedPathsLabelProvider() {
		synchronized (AffectedPathsLabelProvider.class) {
			if (AffectedPathsLabelProvider.folderIcon == null) {
				SVNTeamUIPlugin instance = SVNTeamUIPlugin.instance();
				AffectedPathsLabelProvider.folderIcon = instance
						.getImageDescriptor("icons/views/history/affected_folder.gif") //$NON-NLS-1$
						.createImage();
				AffectedPathsLabelProvider.overlayedFolderIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/empty.gif"), //$NON-NLS-1$
						new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V))
								.createImage();
				AffectedPathsLabelProvider.rootIcon = instance.getImageDescriptor("icons/objects/repository-root.gif") //$NON-NLS-1$
						.createImage();
				AffectedPathsLabelProvider.rootAdditionIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.rootIcon, instance.getImageDescriptor("icons/overlays/addition.gif"), //$NON-NLS-1$
						new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V))
								.createImage();
				AffectedPathsLabelProvider.overlayedRootIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.rootIcon, instance.getImageDescriptor("icons/overlays/empty.gif"), //$NON-NLS-1$
						new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V))
								.createImage();
				AffectedPathsLabelProvider.addedFolderIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.folderIcon,
						instance.getImageDescriptor("icons/overlays/addition.gif"), new Point(22, 16), //$NON-NLS-1$
						OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage();
				AffectedPathsLabelProvider.modifiedFolderIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/change.gif"), //$NON-NLS-1$
						new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V))
								.createImage();
				AffectedPathsLabelProvider.deletedFolderIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.folderIcon,
						instance.getImageDescriptor("icons/overlays/deletion.gif"), new Point(22, 16), //$NON-NLS-1$
						OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage();
				AffectedPathsLabelProvider.replacedFolderIcon = (new OverlayedImageDescriptor(
						AffectedPathsLabelProvider.folderIcon,
						instance.getImageDescriptor("icons/overlays/replacement.gif"), new Point(22, 16), //$NON-NLS-1$
						OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage();
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.folderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.overlayedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.rootIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.rootAdditionIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.overlayedRootIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.addedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.modifiedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.deletedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.replacedFolderIcon);
			}
		}
	}

	public void setCurrentRevision(long currentRevision) {
		this.currentRevision = currentRevision;
	}

	public Image getImage(Object element) {
		if (((AffectedPathsNode) element).getParent() == null) {
			return this.currentRevision == 0
					? AffectedPathsLabelProvider.rootAdditionIcon
					: AffectedPathsLabelProvider.overlayedRootIcon;
		}
		if (((AffectedPathsNode) element).getStatus() != null) {
			switch (((AffectedPathsNode) element).getStatus()) {
				case ADDED: {
					return AffectedPathsLabelProvider.addedFolderIcon;
				}
				case MODIFIED: {
					return AffectedPathsLabelProvider.modifiedFolderIcon;
				}
				case DELETED: {
					return AffectedPathsLabelProvider.deletedFolderIcon;
				}
				case REPLACED: {
					return AffectedPathsLabelProvider.replacedFolderIcon;
				}
			}
		}
		return AffectedPathsLabelProvider.overlayedFolderIcon;
	}

}
