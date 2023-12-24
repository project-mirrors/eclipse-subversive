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
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.change.visitors;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.local.change.IActionOperationProcessor;
import org.eclipse.team.svn.core.operation.local.change.IResourceChangeVisitor;
import org.eclipse.team.svn.core.operation.local.change.ResourceChange;

/**
 * Composite visitor
 * 
 * @author Alexander Gurov
 */
public class CompositeVisitor implements IResourceChangeVisitor {
	protected static IResourceChangeVisitor[] EMPTY = {};

	protected IResourceChangeVisitor[] visitors;

	public CompositeVisitor() {
		visitors = CompositeVisitor.EMPTY;
	}

	public void add(IResourceChangeVisitor visitor) {
		LinkedHashSet<IResourceChangeVisitor> visitors = new LinkedHashSet<>(
				Arrays.asList(this.visitors));
		visitors.add(visitor);
		this.visitors = visitors.toArray(new IResourceChangeVisitor[visitors.size()]);
	}

	@Override
	public void postVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		for (int i = 0; i < visitors.length && !monitor.isCanceled(); i++) {
			visitors[i].postVisit(change, processor, monitor);
		}
	}

	@Override
	public void preVisit(ResourceChange change, IActionOperationProcessor processor, IProgressMonitor monitor)
			throws Exception {
		for (int i = 0; i < visitors.length && !monitor.isCanceled(); i++) {
			visitors[i].preVisit(change, processor, monitor);
		}
	}

}
