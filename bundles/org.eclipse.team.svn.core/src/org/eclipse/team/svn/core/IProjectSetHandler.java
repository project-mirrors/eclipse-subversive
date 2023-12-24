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
 *    Michael (msa) - Eclipse-SourceReferences support
 *    Alexander Fedorov (ArSysOp) - ongoing suppport
 *******************************************************************************/

package org.eclipse.team.svn.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.operation.CompositeOperation;

/**
 * As we can support several project set providers, this is an interface to interact with them
 * 
 * @author Igor Burilo
 */
public interface IProjectSetHandler {

	String asReference(IProject project) throws TeamException;

	String asReference(String resourceUrl, String projectName);

	String getProjectNameForReference(String fullReference);

	IProject configureCheckoutOperation(CompositeOperation op, IProject project, String fullReference)
			throws TeamException;

	boolean accept(String referenceString);

}
