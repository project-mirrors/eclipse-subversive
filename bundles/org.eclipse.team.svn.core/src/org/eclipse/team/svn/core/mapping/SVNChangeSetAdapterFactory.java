/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.team.svn.core.mapping;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;

public class SVNChangeSetAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof SVNActiveChangeSet && adapterType == ResourceMapping.class) {
			SVNActiveChangeSet cs = (SVNActiveChangeSet) adaptableObject;
			return new SVNChangeSetResourceMapping(cs);
		}
		if (adaptableObject instanceof SVNIncomingChangeSet && adapterType == ResourceMapping.class) {
			SVNIncomingChangeSet cs = (SVNIncomingChangeSet) adaptableObject;
			return new SVNChangeSetResourceMapping(cs);
		}
		if (adaptableObject instanceof SVNUnassignedChangeSet && adapterType == ResourceMapping.class) {
			SVNUnassignedChangeSet cs = (SVNUnassignedChangeSet) adaptableObject;
			return new SVNChangeSetResourceMapping(cs);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { ResourceMapping.class };
	}

}
