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

package org.eclipse.team.svn.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.mapping.DiffTreeChangesSection.ITraversalFactory;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.IMemento;

public class ChangeSetCompareAdapter extends SynchronizationCompareAdapter implements ITraversalFactory {

	@Override
	public void save(ResourceMapping[] mappings, IMemento memento) {
		// Don't save
	}

	@Override
	public ResourceMapping[] restore(IMemento memento) {
		// Don't restore
		return new ResourceMapping[0];
	}

	@Override
	public ResourceTraversal[] getTraversals(ISynchronizationScope scope) {
		return scope.getTraversals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
	}

	@Override
	public String getName(ResourceMapping mapping) {
		Object modelObject = mapping.getModelObject();
		if (modelObject instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) modelObject;
			return changeSet.getName();
		}
		return super.getName(mapping);
	}
}
