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
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.repository.browser;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNLock;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveNode;
import org.eclipse.team.svn.ui.repository.model.RepositoryFictiveWorkingDirectory;
import org.eclipse.team.svn.ui.repository.model.RepositoryPending;
import org.eclipse.team.svn.ui.repository.model.RepositoryResource;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * Repository browser label provider
 *
 * @author Sergiy Logvin
 */
public class RepositoryBrowserLabelProvider implements ITableLabelProvider {
	protected Map<ImageDescriptor, Image> images;

	protected static String hasProps;

	protected static String noAuthor;

	protected static String noDate;

	public RepositoryBrowserLabelProvider(RepositoryBrowserTableViewer tableViewer) {
		images = new HashMap<>();
		RepositoryBrowserLabelProvider.noAuthor = SVNMessages.SVNInfo_NoAuthor;
		RepositoryBrowserLabelProvider.noDate = SVNMessages.SVNInfo_NoDate;
		RepositoryBrowserLabelProvider.hasProps = SVNUIMessages.RepositoriesView_Browser_HasProps;
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == RepositoryBrowserTableViewer.COLUMN_NAME) {
			ImageDescriptor iDescr = null;
			if (element instanceof RepositoryResource) {
				iDescr = ((RepositoryResource) element).getImageDescriptor(null);
			} else if (element instanceof RepositoryFictiveNode) {
				iDescr = ((RepositoryFictiveNode) element).getImageDescriptor(null);
			}
			if (iDescr != null) {
				Image img = images.get(iDescr);
				if (img == null) {
					images.put(iDescr, img = iDescr.createImage());
				}
				return img;
			}
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof RepositoryResource) {
			return getColumnTextForElement(element, columnIndex);
		} else if (element instanceof RepositoryFictiveNode) {
			if (columnIndex == RepositoryBrowserTableViewer.COLUMN_NAME) {
				return ((RepositoryFictiveNode) element).getLabel(null);
			}
			if (element instanceof RepositoryFictiveWorkingDirectory) {
				return getColumnTextForElement(
						((RepositoryFictiveWorkingDirectory) element).getAssociatedDirectory(), columnIndex);
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
		for (Image img : images.values()) {
			img.dispose();
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	protected String getColumnTextForElement(Object element, int columnIndex) {
		if (element instanceof RepositoryResource) {
			IRepositoryResource resource = ((RepositoryResource) element).getRepositoryResource();
			IRepositoryResource.Information resourceInfo = resource.getInfo();
			if (columnIndex == RepositoryBrowserTableViewer.COLUMN_NAME) {
				return ((RepositoryResource) element).getLabel();
			} else if (columnIndex == RepositoryBrowserTableViewer.COLUMN_REVISION) {
				String revision = ""; //$NON-NLS-1$
				try {
					if (resource.isInfoCached()) {
						revision = String.valueOf(((RepositoryResource) element).getRevision());
					} else {
						revision = SVNUIMessages.getString(RepositoryPending.PENDING);
					}
				} catch (Exception ex) {
					LoggedOperation.reportError(SVNUIMessages.Error_GetColumnText, ex);
				}
				return revision;
			} else if (resourceInfo != null) {
				if (columnIndex == RepositoryBrowserTableViewer.COLUMN_LAST_CHANGE_DATE) {
					return resourceInfo.lastChangedDate != 0
							? DateFormatter.formatDate(resourceInfo.lastChangedDate)
							: RepositoryBrowserLabelProvider.noDate;
				} else if (columnIndex == RepositoryBrowserTableViewer.COLUMN_LAST_CHANGE_AUTHOR) {
					String author = resourceInfo.lastAuthor;
					return author != null ? author : RepositoryBrowserLabelProvider.noAuthor;
				} else if (columnIndex == RepositoryBrowserTableViewer.COLUMN_LOCK_OWNER) {
					SVNLock lock = resourceInfo.lock;
					String lockOwner = lock == null ? "" : lock.owner; //$NON-NLS-1$
					return lockOwner;
				} else if (columnIndex == RepositoryBrowserTableViewer.COLUMN_SIZE) {
					long size = resourceInfo.fileSize;
					return resource instanceof IRepositoryFile ? String.valueOf(size) : ""; //$NON-NLS-1$
				} else if (columnIndex == RepositoryBrowserTableViewer.COLUMN_HAS_PROPS) {
					boolean hasProps = resourceInfo.hasProperties;
					return hasProps ? RepositoryBrowserLabelProvider.hasProps : ""; //$NON-NLS-1$
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

}
