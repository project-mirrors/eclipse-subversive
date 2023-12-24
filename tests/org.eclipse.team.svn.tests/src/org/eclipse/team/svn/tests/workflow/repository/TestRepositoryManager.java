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

package org.eclipse.team.svn.tests.workflow.repository;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;

/**
 * This class manages a {@link IRepositoryLocation} for testing purposes.
 * 
 * @author Nicolas Peifer
 */
public interface TestRepositoryManager {

	void createRepository() throws Exception;

	void removeRepository();

	IRepositoryLocation getRepositoryLocation();

}