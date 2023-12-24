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

package org.eclipse.team.svn.core.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.TeamException;

/**
 * Remote status cache
 * 
 * @author Igor Burilo
 */
public interface IRemoteStatusCache {

	public interface ICacheVisitor {
		public void visit(IPath path, byte[] data);
	}

	boolean containsData() throws TeamException;

	IResource[] members(IResource resource) throws TeamException;

	IResource[] allMembers(IResource resource) throws TeamException;

	void traverse(IResource[] resources, int depth, ICacheVisitor visitor) throws TeamException;

	byte[] getBytes(IResource resource) throws TeamException;

	boolean setBytes(IResource resource, byte[] bytes) throws TeamException;

	boolean flushBytes(IResource resource, int depth) throws TeamException;

	void clearAll() throws TeamException;

	void dispose();
}
