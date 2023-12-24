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
 *    Alexander Fedorov (ArSysOp) - ongoing suppport
 *******************************************************************************/

package org.eclipse.team.svn.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Action;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Operation;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Reason;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.connector.SVNProperty.BuiltIn;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Resource state filter interface and most useful implementations
 * 
 * @author Alexander Gurov
 */
public interface IStateFilter {

	String ST_INTERNAL_INVALID = "InternalInvalid"; //$NON-NLS-1$

	String ST_NOTEXISTS = null;

	String ST_IGNORED = "Ignored"; //$NON-NLS-1$

	String ST_NEW = "New"; //$NON-NLS-1$

	String ST_ADDED = "Added"; //$NON-NLS-1$

	String ST_NORMAL = "Normal"; //$NON-NLS-1$

	String ST_MODIFIED = "Modified"; //$NON-NLS-1$

	String ST_CONFLICTING = "Conflicting"; //$NON-NLS-1$

	String ST_DELETED = "Deleted"; //$NON-NLS-1$

	String ST_MISSING = "Missing"; //$NON-NLS-1$

	String ST_OBSTRUCTED = "Obstructed"; //$NON-NLS-1$

	String ST_PREREPLACED = "Prereplaced"; //$NON-NLS-1$

	String ST_REPLACED = "Replaced"; //$NON-NLS-1$

	String ST_LINKED = "Linked"; //$NON-NLS-1$

	boolean accept(ILocalResource resource);

	boolean accept(IResource resource, String state, int mask);

	boolean allowsRecursion(ILocalResource resource);

	boolean allowsRecursion(IResource resource, String state, int mask);

	public abstract class AbstractStateFilter implements IStateFilter {
		@Override
		public boolean accept(ILocalResource resource) {
			return resource.getStatus() != IStateFilter.ST_INTERNAL_INVALID
					&& acceptImpl(resource, resource.getResource(), resource.getStatus(), resource.getChangeMask());
		}

		@Override
		public boolean accept(IResource resource, String state, int mask) {
			return state != IStateFilter.ST_INTERNAL_INVALID && acceptImpl(null, resource, state, mask);
		}

		@Override
		public boolean allowsRecursion(ILocalResource resource) {
			return resource.getStatus() != IStateFilter.ST_INTERNAL_INVALID && allowsRecursionImpl(null,
					resource.getResource(), resource.getStatus(), resource.getChangeMask());
		}

		@Override
		public boolean allowsRecursion(IResource resource, String state, int mask) {
			return state != IStateFilter.ST_INTERNAL_INVALID && allowsRecursionImpl(null, resource, state, mask);
		}

		protected abstract boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask);

		protected abstract boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state,
				int mask);

		protected ILocalResource takeLocal(ILocalResource local, IResource resource) {
			return local != null ? local : SVNRemoteStorage.instance().asLocalResource(resource);
		}
	}

	public abstract class CompositeStateFilter implements IStateFilter {
		protected IStateFilter[] filters;

		public CompositeStateFilter(IStateFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean accept(ILocalResource resource) {
			for (IStateFilter filter : filters) {
				if (haveQuickDecision(filter.accept(resource))) {
					return getQuickDecision();
				}
			}
			return getFinalDecision();
		}

		@Override
		public boolean accept(IResource resource, String state, int mask) {
			for (IStateFilter filter : filters) {
				if (haveQuickDecision(filter.accept(resource, state, mask))) {
					return getQuickDecision();
				}
			}
			return getFinalDecision();
		}

		@Override
		public boolean allowsRecursion(ILocalResource resource) {
			for (IStateFilter filter : filters) {
				if (haveQuickDecision(filter.allowsRecursion(resource))) {
					return getQuickDecision();
				}
			}
			return getFinalDecision();
		}

		@Override
		public boolean allowsRecursion(IResource resource, String state, int mask) {
			for (IStateFilter filter : filters) {
				if (haveQuickDecision(filter.allowsRecursion(resource, state, mask))) {
					return getQuickDecision();
				}
			}
			return getFinalDecision();
		}

		protected abstract boolean getFinalDecision();

		protected abstract boolean getQuickDecision();

		protected abstract boolean haveQuickDecision(boolean current);
	}

	public static class OrStateFilter extends CompositeStateFilter {
		public OrStateFilter(IStateFilter[] filters) {
			super(filters);
		}

		@Override
		protected boolean getFinalDecision() {
			return false;
		}

		@Override
		protected boolean getQuickDecision() {
			return true;
		}

		@Override
		protected boolean haveQuickDecision(boolean current) {
			return current;
		}

	}

	public static class AndStateFilter extends CompositeStateFilter {
		public AndStateFilter(IStateFilter[] filters) {
			super(filters);
		}

		@Override
		protected boolean getFinalDecision() {
			return true;
		}

		@Override
		protected boolean haveQuickDecision(boolean current) {
			return !current;
		}

		@Override
		protected boolean getQuickDecision() {
			return false;
		}

	}

	public static abstract class AbstractTreeConflictingStateFilter extends AbstractStateFilter {
		/*
		 * Note: as we're trying to retrieve local resource from remote storage (if it is null) then we must not call
		 * particular filters in order to avoid stack overflow (e.g. SF_UNVERSIONED, it's called during calculating of local resource)
		 */
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			local = takeLocal(local, resource);
			if (local.hasTreeConflict()) {
				SVNConflictDescriptor treeConflict = local.getTreeConflictDescriptor();
				return acceptTreeConflict(treeConflict, local);
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask);
		}

		protected abstract boolean acceptTreeConflict(SVNConflictDescriptor treeConflict, ILocalResource local);
	}

	/**
	 * Check if resource has a tree conflict and if it has then detect if resource exists on repository
	 * 
	 * It is created as a separate class (not as other internal filter classes) in order to allow easily extend it
	 */
	public static class TreeConflictingRepositoryExistStateFilter extends AbstractTreeConflictingStateFilter {
		@Override
		protected boolean acceptTreeConflict(SVNConflictDescriptor treeConflict, ILocalResource resource) {
			/*
			 * For update operation resource exists on repository if action isn't 'Delete'
			 * 
			 * For switch or merge operations we can't exactly detect if resource exists remotely.
			 * Probably, we could determine it be exploring sync info's (AbstractSVNSyncInfo) remote resource variant,
			 * but such solution isn't applicable here (also I found following why we can't use it: while calculating
			 * sync info some filters are called(e.g. SF_ONREPOSITORY) and we get stack overflow).
			 * So we consider that resource exists remotely if conflict descriptor reason is 'modified'
			 * 
			 * TODO Probably, we can add more specific conditions for merge and switch operations here
			 * 		Take into account IResourceChange ?
			 */
			if (treeConflict.operation == Operation.UPDATE || treeConflict.operation == Operation.SWITCHED) {
				/*
				 * 1. Action 'Delete'
				 * 2. Not (Action 'Add' and reason 'Add')
				 */
				return treeConflict.action != Action.DELETE
						&& !(treeConflict.action == Action.ADD && treeConflict.reason == Reason.ADDED);
			} else if (treeConflict.operation == Operation.MERGE) {
				return treeConflict.action != Action.DELETE && treeConflict.reason == Reason.MODIFIED;
			}
			return false;
		}
	}

	IStateFilter SF_TREE_CONFLICTING_REPOSITORY_EXIST = new TreeConflictingRepositoryExistStateFilter();

	IStateFilter SF_INTERNAL_INVALID = new IStateFilter() {
		@Override
		public boolean accept(ILocalResource resource) {
			return resource.getStatus() == IStateFilter.ST_INTERNAL_INVALID;
		}

		@Override
		public boolean accept(IResource resource, String state, int mask) {
			return state == IStateFilter.ST_INTERNAL_INVALID;
		}

		@Override
		public boolean allowsRecursion(ILocalResource resource) {
			return false;
		}

		@Override
		public boolean allowsRecursion(IResource resource, String state, int mask) {
			return false;
		}
	};

	IStateFilter SF_LOCKED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (mask & ILocalResource.IS_LOCKED) != 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_READY_TO_LOCK = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return resource instanceof IFile && (mask & ILocalResource.IS_LOCKED) == 0
					&& IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_SWITCHED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (mask & ILocalResource.IS_SWITCHED) != 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_UNVERSIONED_EXTERNAL = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			//return state == IStateFilter.ST_IGNORED && (mask & ILocalResource.IS_UNVERSIONED_EXTERNAL) != 0;
			return state == IStateFilter.ST_IGNORED && (mask & ILocalResource.IS_SVN_EXTERNALS) != 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask);
		}
	};

	IStateFilter SF_LINKED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_LINKED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_ALL = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_NOTEXISTS = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_NOTEXISTS || state == IStateFilter.ST_LINKED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_OBSTRUCTED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_OBSTRUCTED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_REPLACED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_REPLACED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_PREREPLACED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_PREREPLACEDREPLACED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_REPLACED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_IGNORED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_IGNORED
					|| IStateFilter.SF_UNVERSIONED.accept(resource, state, mask) && SVNUtility.isIgnored(resource);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_IGNORED_NOT_FORBIDDEN = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_IGNORED.accept(resource, state, mask) && (mask & ILocalResource.IS_FORBIDDEN) == 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	/**
	 * @deprecated due to mixed semantics
	 */
	@Deprecated
	IStateFilter SF_IGNORED_BUT_NOT_EXTERNAL = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_IGNORED.accept(resource, state, mask)
					&& (mask & ILocalResource.IS_UNVERSIONED_EXTERNAL) == 0;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_UNVERSIONED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_NEW
					|| state == IStateFilter.ST_IGNORED || state == IStateFilter.ST_NOTEXISTS;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_VERSIONED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			//at first check tree conflict
			local = takeLocal(local, resource);
			if (local.hasTreeConflict()) {
				return new TreeConflictingRepositoryExistStateFilter() {
					@Override
					protected boolean acceptTreeConflict(SVNConflictDescriptor treeConflict, ILocalResource resource) {
						return super.acceptTreeConflict(treeConflict, resource) || Reason.ADDED == treeConflict.reason;
					}
				}.accept(local);
			}
			return state == IStateFilter.ST_REPLACED || state == IStateFilter.ST_PREREPLACED
					|| state == IStateFilter.ST_ADDED || state == IStateFilter.ST_NORMAL
					|| state == IStateFilter.ST_MODIFIED || state == IStateFilter.ST_CONFLICTING
					|| state == IStateFilter.ST_DELETED || state == IStateFilter.ST_MISSING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return this.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_NOTONREPOSITORY = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			//at first check tree conflict
			local = takeLocal(local, resource);
			if (local.hasTreeConflict()) {
				return !IStateFilter.SF_TREE_CONFLICTING_REPOSITORY_EXIST.accept(local);
			}
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_NEW
					|| state == IStateFilter.ST_IGNORED || state == IStateFilter.ST_NOTEXISTS
					|| state == IStateFilter.ST_ADDED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_ONREPOSITORY = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			//at first check tree conflict
			local = takeLocal(local, resource);
			if (local.hasTreeConflict()) {
				return IStateFilter.SF_TREE_CONFLICTING_REPOSITORY_EXIST.accept(local);
			}
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_REPLACED
					|| state == IStateFilter.ST_NORMAL || state == IStateFilter.ST_MODIFIED
					|| state == IStateFilter.ST_CONFLICTING || state == IStateFilter.ST_DELETED
					|| state == IStateFilter.ST_MISSING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_NEW = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_NEW)
					&& !IStateFilter.SF_IGNORED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (!IStateFilter.SF_IGNORED.accept(resource, state, mask)
					|| (mask & ILocalResource.IS_SVN_EXTERNALS) != 0) && state != IStateFilter.ST_OBSTRUCTED
					&& state != IStateFilter.ST_LINKED;
		}
	};

	IStateFilter SF_ADDED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_REPLACED
					|| state == IStateFilter.ST_NEW || state == IStateFilter.ST_ADDED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_NOTMODIFIED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_NORMAL || state == IStateFilter.ST_NOTEXISTS
					|| state == IStateFilter.ST_LINKED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	IStateFilter SF_MODIFIED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_MODIFIED || state == IStateFilter.ST_CONFLICTING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_CONFLICTING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_CONFLICTING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_CONTENT_CONFLICTING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			local = takeLocal(local, resource);
			return local.getTextStatus() == IStateFilter.ST_CONFLICTING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_PROPERTIES_CONFLICTING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			local = takeLocal(local, resource);
			return local.getPropStatus() == IStateFilter.ST_CONFLICTING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_DATA_CONFLICTING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_CONFLICTING && !takeLocal(local, resource).hasTreeConflict();
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_TREE_CONFLICTING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return takeLocal(local, resource).hasTreeConflict();
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_DELETED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_REPLACED
					|| state == IStateFilter.ST_DELETED || state == IStateFilter.ST_MISSING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_MISSING = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_MISSING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_COMMITABLE = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_REPLACED || state == IStateFilter.ST_ADDED
					|| state == IStateFilter.ST_MODIFIED || state == IStateFilter.ST_DELETED
					|| state == IStateFilter.ST_MISSING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_REVERTABLE = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return state == IStateFilter.ST_PREREPLACED || state == IStateFilter.ST_CONFLICTING
					|| state == IStateFilter.ST_REPLACED || state == IStateFilter.ST_ADDED
					|| state == IStateFilter.ST_MODIFIED || state == IStateFilter.ST_DELETED
					|| state == IStateFilter.ST_MISSING
					|| IStateFilter.SF_TREE_CONFLICTING.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_ANY_CHANGE = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return !IStateFilter.SF_IGNORED.accept(resource, state, mask) && state != IStateFilter.ST_NORMAL
					&& state != IStateFilter.ST_OBSTRUCTED && state != IStateFilter.ST_LINKED;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (!IStateFilter.SF_IGNORED.accept(resource, state, mask)
					|| (mask & ILocalResource.IS_SVN_EXTERNALS) != 0) && state != IStateFilter.ST_OBSTRUCTED
					&& state != IStateFilter.ST_LINKED;
		}
	};

	IStateFilter SF_EXCLUDE_DELETED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			if (IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)) {
				return state != IStateFilter.ST_DELETED && state != IStateFilter.ST_MISSING;
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return this.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_NEEDS_LOCK = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, final IResource resource, String state, int mask) {
			if (!(resource instanceof IFile) || IStateFilter.SF_UNVERSIONED.accept(resource, state, mask)
					|| !resource.isAccessible()) {
				return false;
			}
			final SVNProperty[][] propData = new SVNProperty[1][];
			IActionOperation op = new AbstractActionOperation("Operation_CheckProperty", SVNMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
					ISVNConnector proxy = location.acquireSVNProxy();
					try {
						propData[0] = SVNUtility.properties(proxy,
								new SVNEntryRevisionReference(FileUtility.getWorkingCopyPath(resource), null,
										SVNRevision.BASE),
								ISVNConnector.Options.NONE, new SVNProgressMonitor(this, monitor, null));
					} finally {
						location.releaseSVNProxy(proxy);
					}
				}
			};
			ProgressMonitorUtility.doTaskExternalDefault(op, new NullProgressMonitor());
			boolean needsLock = false;
			if (propData[0] != null) {
				for (int i = 0; i < propData[0].length; i++) {
					if (propData[0][i].name.equals(BuiltIn.NEEDS_LOCK)) {
						needsLock = true;
						break;
					}
				}
				return needsLock;
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_MODIFIED_NOT_IGNORED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return !IStateFilter.SF_IGNORED.accept(resource, state, mask)
					&& !IStateFilter.SF_NOTMODIFIED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return (!IStateFilter.SF_IGNORED.accept(resource, state, mask)
					|| (mask & ILocalResource.IS_SVN_EXTERNALS) != 0) && state != IStateFilter.ST_OBSTRUCTED
					&& state != IStateFilter.ST_LINKED;
		}
	};

	IStateFilter SF_EXCLUDE_PREREPLACED_AND_DELETED = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			if (IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					&& !IStateFilter.SF_PREREPLACED.accept(resource, state, mask)) {
				return state != IStateFilter.ST_DELETED && state != IStateFilter.ST_MISSING;
			}
			return false;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_EXCLUDE_PREREPLACED_AND_DELETED_FILES = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return resource instanceof IFile
					&& IStateFilter.SF_EXCLUDE_PREREPLACED_AND_DELETED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_EXCLUDE_DELETED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_VERSIONED_FOLDERS = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return resource instanceof IContainer && IStateFilter.SF_VERSIONED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_VERSIONED_FILES = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return resource instanceof IFile && IStateFilter.SF_VERSIONED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_VERSIONED.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

	IStateFilter SF_HAS_PROPERTIES_CHANGES = new AbstractStateFilter() {
		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			local = takeLocal(local, resource);
			return local.getPropStatus() == IStateFilter.ST_MODIFIED
					|| local.getPropStatus() == IStateFilter.ST_CONFLICTING;
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED_EXTERNAL.accept(resource, state, mask);
		}
	};

}
