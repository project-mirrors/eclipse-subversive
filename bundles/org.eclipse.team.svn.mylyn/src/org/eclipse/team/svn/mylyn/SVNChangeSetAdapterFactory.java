/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
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

package org.eclipse.team.svn.mylyn;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.svn.core.mapping.SVNChangeSetResourceMapping;

public class SVNChangeSetAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ActiveChangeSet && adapterType == ResourceMapping.class) {
			ActiveChangeSet cs = (ActiveChangeSet) adaptableObject;
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