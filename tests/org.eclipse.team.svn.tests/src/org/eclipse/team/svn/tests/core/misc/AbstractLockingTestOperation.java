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

package org.eclipse.team.svn.tests.core.misc;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;

/**
 * @author Alexander Gurov
 */
public abstract class AbstractLockingTestOperation extends AbstractActionOperation {
	public AbstractLockingTestOperation(String operationName) {
		super(operationName, SVNMessages.class);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return MultiRule.combine(TestUtil.getFirstProject(), TestUtil.getSecondProject());
	}

}
