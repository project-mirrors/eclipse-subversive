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
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.resource.ILocalResource;

/**
 * Synchronize view state filter interface with predefined filter set
 * 
 * @author Alexander Gurov
 */
public interface ISyncStateFilter extends IStateFilter {
	boolean acceptRemote(IResource resource, String state, int mask);

	boolean acceptGroupNodes();

	public abstract class AbstractSyncStateFilter extends IStateFilter.AbstractStateFilter implements ISyncStateFilter {

	}

	ISyncStateFilter SF_ONREPOSITORY = new AbstractSyncStateFilter() {
		@Override
		public boolean acceptRemote(IResource resource, String state, int mask) {
			return !IStateFilter.SF_NOTEXISTS.accept(resource, state, mask);
		}

		@Override
		public boolean acceptGroupNodes() {
			return true;
		}

		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_ONREPOSITORY.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	ISyncStateFilter SF_OVERRIDE = new AbstractSyncStateFilter() {
		@Override
		public boolean acceptRemote(IResource resource, String state, int mask) {
			return !IStateFilter.SF_NOTEXISTS.accept(resource, state, mask);
		}

		@Override
		public boolean acceptGroupNodes() {
			return true;
		}

		@Override
		protected boolean acceptImpl(ILocalResource local, IResource resource, String state, int mask) {
			return IStateFilter.SF_REVERTABLE.accept(resource, state, mask)
					|| IStateFilter.SF_UNVERSIONED.accept(resource, state, mask);
		}

		@Override
		protected boolean allowsRecursionImpl(ILocalResource local, IResource resource, String state, int mask) {
			return true;
		}
	};

	public static class StateFilterWrapper implements ISyncStateFilter {
		protected IStateFilter local;

		protected IStateFilter remote;

		protected boolean acceptGroupNodes;

		public StateFilterWrapper(IStateFilter filter, boolean acceptGroupNodes) {
			this(filter, null, acceptGroupNodes);
		}

		public StateFilterWrapper(IStateFilter local, IStateFilter remote, boolean acceptGroupNodes) {
			this.local = local;
			this.remote = remote;
			this.acceptGroupNodes = acceptGroupNodes;
		}

		@Override
		public boolean acceptGroupNodes() {
			return acceptGroupNodes;
		}

		@Override
		public boolean acceptRemote(IResource resource, String state, int mask) {
			return remote != null && remote.accept(resource, state, mask);
		}

		@Override
		public boolean accept(IResource resource, String state, int mask) {
			return local != null && local.accept(resource, state, mask);
		}

		@Override
		public boolean accept(ILocalResource resource) {
			return local != null && local.accept(resource);
		}

		@Override
		public boolean allowsRecursion(IResource resource, String state, int mask) {
			return true;
		}

		@Override
		public boolean allowsRecursion(ILocalResource resource) {
			return true;
		}
	}

}
