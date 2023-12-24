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

package org.eclipse.team.svn.ui.decorator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamProvider;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.events.IResourceStatesListener;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.team.ui.synchronize.TeamStateDescription;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

/**
 * SVN decorator for ResourceMapping's
 * 
 * @author Igor Burilo
 */
public class SVNLightweightDecorator extends LabelProvider
		implements ILightweightLabelDecorator, IResourceStatesListener {

	// Decorator id as defined in the decorator extension point
	public final static String ID = "org.eclipse.team.svn.ui.decorator.SVNLightweightDecorator"; //$NON-NLS-1$

	protected static final ImageDescriptor OVR_VERSIONED = TeamImages
			.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR);

	protected static final ImageDescriptor OVR_ADDED = TeamImages.getImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR);

	protected static ImageDescriptor OVR_NEW = TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR);

	protected static ImageDescriptor OVR_MODIFIED = TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR);

	protected static ImageDescriptor OVR_CONFLICTED;

	protected static ImageDescriptor OVR_OBSTRUCTED;

	protected static ImageDescriptor OVR_DELETED;

	protected static ImageDescriptor OVR_LOCKED;

	protected static ImageDescriptor OVR_NEEDS_LOCK;

	protected static ImageDescriptor OVR_SWITCHED;

	protected IPropertyChangeListener configurationListener;

	protected boolean indicateConflicted;

	protected boolean indicateModified;

	//protected boolean indicateDeleted;
	protected boolean indicateRemote;

	protected boolean indicateAdded;

	protected boolean indicateNew;

	protected boolean indicateLocked;

	protected boolean indicateNeedsLock;

	protected boolean indicateSwitched;

	protected String outgoingChars;

	protected String addedChars;

	protected String trunkPrefix;

	protected String branchPrefix;

	protected String tagPrefix;

	protected boolean useFonts;

	protected IDecorationFilter filter;

	protected Font ignoredFont;

	protected Font changedFont;

	protected Color ignoredForegroundColor;

	protected Color ignoredBackgroundColor;

	protected Color changedForegroundColor;

	protected Color changedBackgroundColor;

	protected IVariable[] fileFormat;

	protected IVariable[] folderFormat;

	protected IVariable[] projectFormat;

	protected DecoratorVariables decorator;

	protected boolean computeDeep;

	private static final SynchronizationStateTester DEFAULT_TESTER = new SynchronizationStateTester();

	public SVNLightweightDecorator() {
		initStatic();

		configurationListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().startsWith(SVNTeamPreferences.DECORATION_BASE)
						|| event.getProperty().startsWith(SVNTeamPreferences.DATE_FORMAT_BASE)) {
					SVNLightweightDecorator.this.loadConfiguration();
					String decoratorId = this.getClass().getName();
					SVNTeamUIPlugin.instance().getWorkbench().getDecoratorManager().update(decoratorId);
				}
			}
		};

		filter = ExtensionsManager.getInstance().getCurrentDecorationFilter();

		SVNTeamUIPlugin.instance().getPreferenceStore().addPropertyChangeListener(configurationListener);
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(configurationListener);

		SVNRemoteStorage.instance().addResourceStatesListener(ResourceStatesChangedEvent.class, this);

		decorator = new DecoratorVariables(TextVariableSetProvider.instance);
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench()
				.getThemeManager()
				.getCurrentTheme()
				.removePropertyChangeListener(configurationListener);
		SVNTeamUIPlugin.instance().getPreferenceStore().removePropertyChangeListener(configurationListener);

		SVNRemoteStorage.instance().removeResourceStatesListener(ResourceStatesChangedEvent.class, this);

		super.dispose();
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		try {
			// Don't decorate the workspace root or deleted/closed resources
			IResource resource = getResource(element);
			if (resource != null && (resource.getType() == IResource.ROOT || !resource.isAccessible())) {
				return;
			}

			// Get the mapping for the object and ensure it overlaps with SVN projects
			ResourceMapping mapping = Utils.getResourceMapping(element);
			if (mapping == null || !isMappedToSVN(mapping)) {
				return;
			}

			// Get the sync state tester from the context
			IDecorationContext context = decoration.getDecorationContext();
			SynchronizationStateTester tester = SVNLightweightDecorator.DEFAULT_TESTER;
			Object property = context.getProperty(SynchronizationStateTester.PROP_TESTER);
			if (property instanceof SynchronizationStateTester) {
				tester = (SynchronizationStateTester) property;
			}

			// Calculate and apply the decoration
			if (tester.isDecorationEnabled(element) && isSupervised(mapping)) {
				if (fileFormat == null) {
					loadConfiguration();
				}
				if (resource != null) {
					decorateResource(resource, decoration);
				} else {
					decorateModel(element, mapping, decoration, tester);
				}
			}
		} catch (Throwable ex) {
			LoggedOperation.reportError("SVN Decorator", ex);//$NON-NLS-1$
		}
	}

	protected void decorateModel(final Object element, ResourceMapping mapping, IDecoration decoration,
			SynchronizationStateTester tester) throws CoreException {
		//TODO how to limit depth according to "Compute deep outgoing state" properties ? (Subscriber itself already reports the model element's state after a deep calculation)
		final int stateFlags = tester.getState(element,
				IDiff.ADD | IDiff.REMOVE | IDiff.CHANGE | IThreeWayDiff.OUTGOING, new NullProgressMonitor());
		// for model nodes the resulting state is integral of all the states of the included resources
		//	so, it is unreasonable to try to decorate conflicts, additions and removals except as a 'CHANGE' state only
//		if ((stateFlags & IThreeWayDiff.DIRECTION_MASK) == IThreeWayDiff.CONFLICTING && this.indicateConflicted) {
//			if (this.indicateConflicted) {
//				decoration.addOverlay(SVNLightweightDecorator.OVR_CONFLICTED);
//			}
//			else if (this.indicateModified) {
//				decoration.addOverlay(SVNLightweightDecorator.OVR_MODIFIED);
//			}
//			else if (this.indicateRemote) {
//				decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
//			}
//		}
//		else if ((stateFlags & IDiff.ADD) != 0) {
//			//new state also recognized as added, then it should be before added
//			if (this.indicateAdded) {
//				decoration.addOverlay(SVNLightweightDecorator.OVR_ADDED);
//			}
//		}
//		else if ((stateFlags & IDiff.REMOVE) != 0) {
//			decoration.addOverlay(SVNLightweightDecorator.OVR_DELETED);
//		}
//		else
		if ((stateFlags & (IDiff.ADD | IDiff.REMOVE | IDiff.CHANGE)) != 0) {
			if (indicateModified) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_MODIFIED);
			} else if (indicateRemote) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
			}
		} else if (indicateRemote) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
		}

		if (useFonts && (stateFlags & (IDiff.ADD | IDiff.REMOVE | IDiff.CHANGE)) != 0) {
			decoration.setBackgroundColor(changedBackgroundColor);
			decoration.setForegroundColor(changedForegroundColor);
			decoration.setFont(changedFont);
		}

		decorator.decorateText(
				decoration, getFormat(IResource.FOLDER), var -> {
//					if (var.equals(TextVariableSetProvider.VAR_ADDED_FLAG)) {
//						return (stateFlags & IDiff.ADD) != 0 ? SVNLightweightDecorator.this.addedChars : ""; //$NON-NLS-1$
//					}
//					else
					if (var.equals(TextVariableSetProvider.VAR_OUTGOING_FLAG)) {
						return (stateFlags & IThreeWayDiff.OUTGOING) != 0 ? outgoingChars : ""; //$NON-NLS-1$
					}
					String retVal = var.toString();
					return retVal.equals(outgoingChars) ? retVal : "";
				}
		);
		tester.elementDecorated(element, new TeamStateDescription(stateFlags));
	}

	protected void decorateResource(IResource resource, IDecoration decoration) {
		if (!filter.isAcceptable(resource)) {
			return;
		}

		ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
		IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(resource);
		if (IStateFilter.SF_INTERNAL_INVALID.accept(local) || remote == null) {
			return;
		}

		String localStatus = getStatus(local);
		int mask = local.getChangeMask();

		if (!IStateFilter.SF_NOTEXISTS.accept(resource, localStatus, mask)) {
			decorateResourceImpl(remote, local, resource, localStatus, mask, decoration);
		}
	}

	protected void decorateResourceImpl(final IRepositoryResource remote, final ILocalResource local,
			final IResource resource, final String state, final int mask, IDecoration decoration) {
		if (IStateFilter.SF_TREE_CONFLICTING.accept(resource, state, mask) && indicateConflicted) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_CONFLICTED);
		} else if (local.isLocked() && indicateLocked) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_LOCKED);
		} else if (IStateFilter.SF_IGNORED.accept(resource, state, mask)) {
			if (useFonts) {
				decoration.setBackgroundColor(ignoredBackgroundColor);
				decoration.setForegroundColor(ignoredForegroundColor);
				decoration.setFont(ignoredFont);
			}
		} else if (IStateFilter.SF_NEW.accept(resource, state, mask)) {
			if (indicateNew) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_NEW);
			}
		} else if (indicateNeedsLock && IStateFilter.SF_NEEDS_LOCK.accept(resource, state, mask)) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_NEEDS_LOCK);
		} else if (IStateFilter.SF_ADDED.accept(resource, state, mask)) {
			//new state also recognized as added, then it should be before added
			if (indicateAdded) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_ADDED);
			}
		} else if (indicateDeleted(resource) && IStateFilter.SF_DELETED.accept(resource, state, mask)) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_DELETED);
		} else if (IStateFilter.SF_CONFLICTING.accept(resource, state, mask)) {
			if (indicateConflicted) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_CONFLICTED);
			} else if (indicateModified) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_MODIFIED);
			} else if (indicateSwitched && (local.getChangeMask() & ILocalResource.IS_SWITCHED) != 0) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_SWITCHED);
			} else if (indicateRemote) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
			}
		} else if (IStateFilter.SF_MODIFIED.accept(resource, state, mask)) {
			if (indicateModified) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_MODIFIED);
			} else if (indicateSwitched && (local.getChangeMask() & ILocalResource.IS_SWITCHED) != 0) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_SWITCHED);
			} else if (indicateRemote) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
			}
		} else if (IStateFilter.SF_OBSTRUCTED.accept(resource, state, mask)) {
			decoration.addOverlay(SVNLightweightDecorator.OVR_OBSTRUCTED);
		} else if (IStateFilter.SF_VERSIONED.accept(resource, state, mask)) {
			if (indicateSwitched && (local.getChangeMask() & ILocalResource.IS_SWITCHED) != 0) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_SWITCHED);
			} else if (indicateRemote) {
				decoration.addOverlay(SVNLightweightDecorator.OVR_VERSIONED);
			}
		}
		if (useFonts && IStateFilter.SF_ANY_CHANGE.accept(resource, state, mask)) {
			decoration.setBackgroundColor(changedBackgroundColor);
			decoration.setForegroundColor(changedForegroundColor);
			decoration.setFont(changedFont);
		}

		decorator.decorateText(
				decoration, getFormat(resource.getType()), var -> {
					if (var.equals(TextVariableSetProvider.VAR_ADDED_FLAG)) {
						return IStateFilter.SF_ADDED.accept(resource, state, mask) ? addedChars : ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_OUTGOING_FLAG)) {
						return IStateFilter.SF_COMMITABLE.accept(resource, state, mask)
								|| IStateFilter.SF_CONFLICTING.accept(resource, state, mask)
								|| IStateFilter.SF_TREE_CONFLICTING.accept(resource, state, mask)
										? outgoingChars
										: ""; //$NON-NLS-1$
					}

					if (var.equals(TextVariableSetProvider.VAR_REVISION)) {
						return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
								&& !IStateFilter.SF_PREREPLACEDREPLACED.accept(resource, state, mask)
										? String.valueOf(local.getRevision())
										: ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_AUTHOR)) {
						String author = local.getAuthor() == null ? "[no author]" : local.getAuthor(); //$NON-NLS-1$
						return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask) ? author : ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_DATE)) {
						if (!IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							return ""; //$NON-NLS-1$
						}
						long date = local.getLastCommitDate();
						if (date == 0) {
							return SVNMessages.SVNInfo_NoDate;
						}
						return DateFormatter.formatDate(date);
					} else if (var.equals(TextVariableSetProvider.VAR_RESOURCE_URL)) {
						return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
								? SVNUtility.decodeURL(remote.getUrl())
								: ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_SHORT_RESOURCE_URL)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							String shortURL = SVNUtility.decodeURL(remote.getUrl())
									.substring(remote.getRepositoryLocation().getRepositoryRootUrl().length());
							return shortURL.startsWith("/") ? shortURL.substring(1) : shortURL; //$NON-NLS-1$
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_LOCATION_URL)) {
						return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
								? remote.getRepositoryLocation().getUrlAsIs()
								: ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_LOCATION_LABEL)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							IRepositoryLocation location = remote.getRepositoryLocation();
							String label = location.getLabel();
							return label == null || label.length() == 0 ? location.getUrlAsIs() : label;
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_ROOT_PREFIX)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							int kind = ((IRepositoryRoot) remote.getRoot()).getKind();
							return kind == IRepositoryRoot.KIND_TAGS
									? tagPrefix
									: kind == IRepositoryRoot.KIND_BRANCHES
											? branchPrefix
											: kind == IRepositoryRoot.KIND_TRUNK ? trunkPrefix : "";
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_ASCENDANT)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							return SVNUtility.getAscendant(remote);
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_DESCENDANT)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							return SVNUtility.getDescendant(remote);
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_FULLNAME)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							return SVNUtility.getPathUpToRoot(remote);
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_FULLPATH)) {
						if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
							String retVal = SVNUtility.getPathUpToRoot(remote);
							int pos = retVal.lastIndexOf('/');
							if (pos != -1) {
								retVal = retVal.substring(0, pos);
							}
							return retVal;
						}
						return ""; //$NON-NLS-1$
					} else if (var.equals(TextVariableSetProvider.VAR_REMOTE_NAME)) {
						return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask) ? remote.getName() : ""; //$NON-NLS-1$
					}
					if (var.equals(TextVariableSetProvider.VAR_NAME)) {
						return local.getName();
					}
					return var.toString();
				}
		);
	}

	protected boolean indicateDeleted(IResource resource) {
		return resource.getType() != IResource.FILE;
	}

	protected IVariable[] getFormat(int type) {
		if (type == IResource.FOLDER) {
			return folderFormat;
		} else if (type == IResource.PROJECT) {
			return projectFormat;
		}
		return fileFormat;
	}

	protected String getStatus(ILocalResource local) {
		if (computeDeep && local.getResource().getType() != IResource.FILE
				&& local.getStatus() == IStateFilter.ST_NORMAL && FileUtility.checkForResourcesPresenceRecursive(
						new IResource[] { local.getResource() }, IStateFilter.SF_MODIFIED_NOT_IGNORED)) {
			return IStateFilter.ST_MODIFIED;
		}
		return local.getStatus();
	}

	protected boolean isSupervised(ResourceMapping mapping) throws CoreException {
		for (ResourceTraversal traversal : mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null)) {
			for (IResource resource : traversal.getResources()) {
				if (UpdateSubscriber.instance().isSupervised(resource)) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Return whether any of the projects of the mapping are mapped to SVN
	 */
	protected boolean isMappedToSVN(ResourceMapping mapping) {
		for (IProject project : mapping.getProjects()) {
			if (project != null && project.isAccessible()) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project);
				if (provider instanceof SVNTeamProvider) {
					return true;
				}
			}
		}
		return false;
	}

	protected IResource getResource(Object element) {
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}
		return Utils.getResource(element);
	}

	protected synchronized void initStatic() {
		SVNLightweightDecorator.OVR_NEW = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/new_resource.gif"); //$NON-NLS-1$
		// we cannot use pencil icon for modified resources due to CVS uses it in order to show watch/edit states
		//SVNLightweightDecorator.OVR_MODIFIED = SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/modified_resource.gif");
		SVNLightweightDecorator.OVR_CONFLICTED = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/conflicted_unresolved.gif"); //$NON-NLS-1$
		SVNLightweightDecorator.OVR_OBSTRUCTED = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/obstructed.gif"); //$NON-NLS-1$
		SVNLightweightDecorator.OVR_DELETED = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/deleted.gif"); //$NON-NLS-1$
		SVNLightweightDecorator.OVR_LOCKED = SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/lock.gif"); //$NON-NLS-1$
		SVNLightweightDecorator.OVR_NEEDS_LOCK = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/needs_lock.gif"); //$NON-NLS-1$
		SVNLightweightDecorator.OVR_SWITCHED = SVNTeamUIPlugin.instance()
				.getImageDescriptor("icons/overlays/switched.gif"); //$NON-NLS-1$
	}

	protected void loadConfiguration() {
		IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();

		indicateConflicted = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_CONFLICTED_NAME);
		indicateModified = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_MODIFIED_NAME);
		indicateRemote = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_REMOTE_NAME);
		indicateAdded = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_ADDED_NAME);
		indicateNew = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEW_NAME);
		indicateLocked = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_LOCKED_NAME);
		indicateNeedsLock = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_NAME);
		indicateSwitched = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME);

		outgoingChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_OUTGOING_NAME);
		addedChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_ADDED_NAME);

		trunkPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TRUNK_PREFIX_NAME);
		branchPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_BRANCH_PREFIX_NAME);
		tagPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TAG_PREFIX_NAME);
		useFonts = SVNTeamPreferences.getDecorationBoolean(store,
				SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_NAME);
		UIMonitorUtility.getDisplay().syncExec(() -> {
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			//SWT.COLOR_TRANSPARENT does not seem to be working when set using plugin.xml definitions
			Color sample = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
			Color c;
			ignoredFont = current.getFontRegistry()
					.get(SVNTeamPreferences.fullDecorationName(SVNTeamPreferences.NAME_OF_IGNORED_FONT));
			changedFont = current.getFontRegistry()
					.get(SVNTeamPreferences.fullDecorationName(SVNTeamPreferences.NAME_OF_OUTGOING_FONT));
			ignoredForegroundColor = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_IGNORED_FOREGROUND_COLOR));
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_IGNORED_BACKGROUND_COLOR));
			ignoredBackgroundColor = c == null || c.equals(sample) ? null : c;
			changedForegroundColor = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_OUTGOING_FOREGROUND_COLOR));
			c = current.getColorRegistry()
					.get(SVNTeamPreferences
							.fullDecorationName(SVNTeamPreferences.NAME_OF_OUTGOING_BACKGROUND_COLOR));
			changedBackgroundColor = c == null || c.equals(sample) ? null : c;
		});

		computeDeep = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_COMPUTE_DEEP_NAME);

		String formatLine = SVNTeamPreferences.getDecorationString(store,
				SVNTeamPreferences.DECORATION_FORMAT_FOLDER_NAME);
		folderFormat = decorator.parseFormatLine(formatLine);

		formatLine = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FILE_NAME);
		fileFormat = decorator.parseFormatLine(formatLine);

		formatLine = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_PROJECT_NAME);
		projectFormat = decorator.parseFormatLine(formatLine);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.svn.core.resource.events.IResourceStatesListener#resourcesStateChanged(org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent)
	 */
	@Override
	public void resourcesStateChanged(ResourceStatesChangedEvent event) {
		fireLabelProviderChanged(new LabelProviderChangedEvent(this, event.getResourcesRecursivelly()));
	}
}
