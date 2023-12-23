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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.local.change.visitors.RemoveNonVersionedVisitor;
import org.eclipse.team.svn.core.resource.IResourceProvider;

/**
 * Remove all non-versioned resources in subtree. Should be used in "Override And Update" action.
 * Legacy code, use new ResourcesTraversalOperation(resources, new RemoveNonVersionedVisitor(true), IResource.DEPTH_INFINITE) instead.
 * 
 * @author Alexander Gurov
 */
public class RemoveNonVersionedResourcesOperation extends ResourcesTraversalOperation {
    public RemoveNonVersionedResourcesOperation(IResource[] resources, boolean addedAlso) {
        super("Operation_RemoveNonSVN", SVNMessages.class, resources, new RemoveNonVersionedVisitor(addedAlso), IResource.DEPTH_INFINITE);
    }

    public RemoveNonVersionedResourcesOperation(IResourceProvider provider, boolean addedAlso) {
        super("Operation_RemoveNonSVN", SVNMessages.class, provider, new RemoveNonVersionedVisitor(addedAlso), IResource.DEPTH_INFINITE);
    }

}
