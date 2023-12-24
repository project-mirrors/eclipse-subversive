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
 *    Alexander Gurov - Initial API and implementation
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.repository.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.svn.core.connector.SVNLock;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResource.Information;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.decorator.DecoratorVariables;
import org.eclipse.team.svn.ui.decorator.IVariable;
import org.eclipse.team.svn.ui.decorator.IVariableContentProvider;
import org.eclipse.team.svn.ui.decorator.UserVariable;
import org.eclipse.team.svn.ui.operation.GetRemoteResourceRevisionOperation;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.repository.RepositoriesView;
import org.eclipse.team.svn.ui.repository.RepositoryTreeViewer;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.themes.ITheme;

/**
 * Abstract repository element representation
 * 
 * @author Alexander Gurov
 */
public abstract class RepositoryResource implements IWorkbenchAdapter, IWorkbenchAdapter2, IAdaptable,
		IResourceTreeNode, IToolTipProvider, IVariableContentProvider {
	public static RGB NOT_RELATED_NODES_FOREGROUND;

	public static RGB NOT_RELATED_NODES_BACKGROUND;

	public static Font NOT_RELATED_NODES_FONT;

	public static RGB STRUCTURE_DEFINED_NODES_FOREGROUND;

	public static RGB STRUCTURE_DEFINED_NODES_BACKGROUND;

	public static Font STRUCTURE_DEFINED_NODES_FONT;

	protected static Map<ImageDescriptor, OverlayedImageDescriptor> images = new HashMap<>();

	protected static ImageDescriptor lockDescriptor;

	protected static ImageDescriptor externalsDescriptor;

	protected GetRemoteResourceRevisionOperation revisionOp;

	protected IRepositoryResource resource;

	protected RepositoryResource parent;

	protected RepositoryTreeViewer repositoryTree;

	protected Boolean relatesToLocation;

	protected IPropertyChangeListener configurationListener;

	protected DecoratorVariables toolTipDecorator;

	protected String label;

	protected boolean externals;

	public RepositoryResource(RepositoryResource parent, IRepositoryResource resource) {
		this.parent = parent;
		this.resource = resource;
		if (RepositoryResource.NOT_RELATED_NODES_FOREGROUND == null) {
			initializeFontsAndColors();
		}
		toolTipDecorator = new DecoratorVariables(ToolTipVariableSetProvider.instance);
	}

	public boolean isExternals() {
		return externals ? true : parent == null ? false : parent.isExternals();
	}

	public void setExternals(boolean externals) {
		this.externals = externals;
	}

	public RepositoryResource getParent() {
		return parent;
	}

	public boolean isRelatesToLocation() {
		if (relatesToLocation == null) {
			relatesToLocation = resource.getSelectedRevision().getKind() == Kind.HEAD
					&& SVNUtility.createPathForSVNUrl(resource.getRepositoryLocation().getUrl())
							.isPrefixOf(SVNUtility.createPathForSVNUrl(resource.getUrl()));
		}
		return relatesToLocation;
	}

	@Override
	public void setViewer(RepositoryTreeViewer repositoryTree) {
		this.repositoryTree = repositoryTree;
	}

	@Override
	public IRepositoryResource getRepositoryResource() {
		return resource;
	}

	@Override
	public Object getData() {
		return resource;
	}

	@Override
	public void refresh() {
		resource.refresh();
		revisionOp = null;
	}

	@Override
	public RGB getBackground(Object element) {
		return isRelatesToLocation() ? null : RepositoryResource.NOT_RELATED_NODES_BACKGROUND;
	}

	@Override
	public RGB getForeground(Object element) {
		return isRelatesToLocation() ? null : RepositoryResource.NOT_RELATED_NODES_FOREGROUND;
	}

	@Override
	public FontData getFont(Object element) {
		return isRelatesToLocation() ? null : RepositoryResource.NOT_RELATED_NODES_FONT.getFontData()[0];
	}

	@Override
	public String getLabel(Object o) {
		String retVal = this.getLabel() + " "; //$NON-NLS-1$
		try {
			retVal += getRevision();
		} catch (Exception e) {
			LoggedOperation.reportError(SVNUIMessages.Error_FormatLabel, e);
			retVal += SVNUIMessages.getString(RepositoryError.ERROR_MSG);
		}
		return retVal;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		ImageDescriptor originalDescriptor = getImageDescriptorImpl();
		Information info = resource.getInfo();
		if (info != null && info.lock != null) {
			if (RepositoryResource.lockDescriptor == null) {
				RepositoryResource.lockDescriptor = SVNTeamUIPlugin.instance()
						.getImageDescriptor("icons/overlays/lock.gif"); //$NON-NLS-1$
			}
			return RepositoryResource.decorateImage(originalDescriptor, RepositoryResource.lockDescriptor);
		} else if (isExternals() && SVNTeamPreferences.getDecorationBoolean(
				SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME)) {
			if (RepositoryResource.externalsDescriptor == null) {
				RepositoryResource.externalsDescriptor = SVNTeamUIPlugin.instance()
						.getImageDescriptor("icons/overlays/switched.gif"); //$NON-NLS-1$
			}
			return RepositoryResource.decorateImage(originalDescriptor, RepositoryResource.externalsDescriptor);
		}
		return originalDescriptor;
	}

	protected static ImageDescriptor decorateImage(ImageDescriptor originalDescriptor,
			ImageDescriptor decorationDescriptor) {
		synchronized (RepositoryResource.images) {
			OverlayedImageDescriptor imgDescr = RepositoryResource.images.get(originalDescriptor);
			if (imgDescr == null) {
				Image image = originalDescriptor.createImage();
				CompareUI.disposeOnShutdown(image);
				RepositoryResource.images.put(originalDescriptor,
						imgDescr = new OverlayedImageDescriptor(image, decorationDescriptor, new Point(16, 16),
								OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.BOTTOM));
			}
			return imgDescr;
		}
	}

	@Override
	public Object getParent(Object o) {
		return resource.getParent();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IWorkbenchAdapter.class) || adapter.equals(IWorkbenchAdapter2.class)) {
			return this;
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RepositoryResource) {
			return ((RepositoryResource) obj).resource.equals(resource);
		}
		return super.equals(obj);
	}

	@Override
	public String getToolTipMessage(String formatString) {
		ToolTipMessage tooltipMessage = new ToolTipMessage();
		IVariable[] format = toolTipDecorator.parseFormatLine(formatString);
		toolTipDecorator.decorateText(tooltipMessage, format, this);
		return tooltipMessage.getMessage();
	}

	@Override
	public String getValue(IVariable var) {
		IRepositoryResource resource = getRepositoryResource();
		Information info = resource.getInfo();
		SVNLock lock = info == null ? null : info.lock;
		if (var.equals(ToolTipVariableSetProvider.VAR_NAME)) {
			return RepositoryResource.this.formatToolTipLine(var, resource.getName());
		} else if (var.equals(ToolTipVariableSetProvider.VAR_URL)) {
			return RepositoryResource.this.formatToolTipLine(var, resource.getUrl());
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LAST_CHANGE_DATE)) {
			if (info != null && info.lastChangedDate != 0) {
				return RepositoryResource.this.formatToolTipLine(var, DateFormatter.formatDate(info.lastChangedDate));
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LAST_AUTHOR)) {
			if (info != null && info.lastAuthor != null && !info.lastAuthor.equals("")) { //$NON-NLS-1$
				return RepositoryResource.this.formatToolTipLine(var, info.lastAuthor);
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_SIZE)) {
			if (info != null) {
				return RepositoryResource.this.formatToolTipLine(var, String.valueOf(info.fileSize));
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LOCK_OWNER)) {
			if (lock != null && lock.owner != null) {
				return RepositoryResource.this.formatToolTipLine(var, lock.owner);
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LOCK_CREATION_DATE)) {
			if (lock != null && lock.creationDate != 0) {
				return RepositoryResource.this.formatToolTipLine(var, DateFormatter.formatDate(lock.creationDate));
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LOCK_EXPIRATION_DATE)) {
			if (lock != null && lock.expirationDate != 0) {
				return RepositoryResource.this.formatToolTipLine(var, DateFormatter.formatDate(lock.expirationDate));
			}
		} else if (var.equals(ToolTipVariableSetProvider.VAR_LOCK_COMMENT)) {
			if (lock != null && lock.comment != null && lock.comment.length() != 0) {
				return RepositoryResource.this.formatToolTipLine(var, lock.comment);
			}
		}
		if (var instanceof UserVariable) {
			return var.toString();
		}
		return ""; //$NON-NLS-1$
	}

	protected String formatToolTipLine(IVariable var, String value) {
		return var.getDescription() + " " + value + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void initializeFontsAndColors() {
		configurationListener = event -> {
			if (event.getProperty().startsWith(SVNTeamPreferences.DECORATION_BASE)) {
				RepositoryResource.loadConfiguration();
				RepositoriesView repositoriesView = RepositoriesView.instance();
				if (repositoriesView != null) {
					repositoriesView.getRepositoryTree().refresh();
				}
			}
		};
		RepositoryResource.loadConfiguration();
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(configurationListener);
	}

	protected static void loadConfiguration() {
		UIMonitorUtility.getDisplay().syncExec(() -> {
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			//SWT.COLOR_TRANSPARENT does not seem to be working when set using plugin.xml definitions
			Color sample = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
			Color c;
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_NOT_RELATED_NODES_FOREGROUND_COLOR));
			RepositoryResource.NOT_RELATED_NODES_FOREGROUND = c == null ? null : c.getRGB();
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_NOT_RELATED_NODES_BACKGROUND_COLOR));
			RepositoryResource.NOT_RELATED_NODES_BACKGROUND = c == null || c.equals(sample) ? null : c.getRGB();
			RepositoryResource.NOT_RELATED_NODES_FONT = current.getFontRegistry()
					.get(SVNTeamPreferences.fullDecorationName(SVNTeamPreferences.NAME_OF_NOT_RELATED_NODES_FONT));
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_STRUCTURE_NODES_FOREGROUND_COLOR));
			RepositoryResource.STRUCTURE_DEFINED_NODES_FOREGROUND = c == null ? null : c.getRGB();
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_STRUCTURE_NODES_BACKGROUND_COLOR));
			RepositoryResource.STRUCTURE_DEFINED_NODES_BACKGROUND = c == null || c.equals(sample)
					? null
					: c.getRGB();
			RepositoryResource.STRUCTURE_DEFINED_NODES_FONT = current.getFontRegistry()
					.get(SVNTeamPreferences.fullDecorationName(SVNTeamPreferences.NAME_OF_STRUCTURE_NODES_FONT));
		});
	}

	public String getLabel() {
		return label == null ? resource.getName() : label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getRevision() throws Exception {
		if (revisionOp != null) {
			return revisionOp.getRevision() == SVNRevision.INVALID_REVISION_NUMBER
					? SVNUIMessages.getString(revisionOp.getExecutionState() == IActionOperation.ERROR
							? RepositoryError.ERROR_MSG
							: RepositoryPending.PENDING)
					: String.valueOf(revisionOp.getRevision());
		}

		if (resource.isInfoCached()) {
			return String.valueOf(resource.getRevision());
		}

		revisionOp = new GetRemoteResourceRevisionOperation(resource);
		CompositeOperation op = new CompositeOperation(revisionOp.getId(), revisionOp.getMessagesClass());
		op.add(revisionOp);
		op.add(getRefreshOperation(getViewer()));

		UIMonitorUtility.doTaskScheduled(op, new DefaultOperationWrapperFactory() {
			@Override
			public IActionOperation getLogged(IActionOperation operation) {
				return new LoggedOperation(operation);
			}
		});

		return SVNUIMessages.getString(RepositoryPending.PENDING);
	}

	protected RepositoryTreeViewer getViewer() {
		return repositoryTree;
	}

	protected RefreshOperation getRefreshOperation(RepositoryTreeViewer viewer) {
		return new RefreshOperation(viewer);
	}

	protected abstract ImageDescriptor getImageDescriptorImpl();

	protected class RefreshOperation extends AbstractActionOperation {
		protected RepositoryTreeViewer viewer;

		public RefreshOperation(RepositoryTreeViewer viewer) {
			super("Operation_RefreshView", SVNUIMessages.class); //$NON-NLS-1$
			this.viewer = viewer;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			// TODO rework this using cancellation manager in order to make it thread-safe...
			if (viewer != null && !viewer.getControl().isDisposed()) {
				viewer.refresh(RepositoryResource.this, null, true);
			}
		}
	}

}
