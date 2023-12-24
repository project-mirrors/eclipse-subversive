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

package org.eclipse.team.svn.core.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.synchronize.variant.ResourceVariant;

/**
 * ResourceVariant comparator
 * 
 * @author Alexander Gurov
 */
public class ResourceVariantComparator implements IResourceVariantComparator {

	public boolean compare(IResource local, IResourceVariant remote) {
		if (local == null && remote == null) {
			return true;
		}
		if (local == null || remote == null) {
			return false;
		}
		ILocalResource resource = SVNRemoteStorage.instance().asLocalResource(local);
		return resource.getRevision() == ((ResourceVariant) remote).getResource().getRevision();
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		if (base == remote) {
			return true;
		}
		if (base == null || remote == null) {
			return false;
		}
		return base.getContentIdentifier().equals(remote.getContentIdentifier());
	}

	public boolean isThreeWay() {
		return true;
	}

}
