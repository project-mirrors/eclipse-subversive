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

package org.eclipse.team.svn.tests;

import org.eclipse.team.svn.tests.core.AbstractOperationTest;
import org.eclipse.team.svn.tests.core.RepositoryLocationsManagementTest;
import org.eclipse.team.svn.tests.core.StateFilterTest;
import org.eclipse.team.svn.tests.ui.DecoratorVariablesTest;
import org.eclipse.team.svn.tests.ui.JavaViewMenuEnablementTest;
import org.eclipse.team.svn.tests.ui.RepositoryViewMenuEnablementTest;
import org.eclipse.team.svn.tests.ui.SVNTeamMoveDeleteHookTest;
import org.eclipse.team.svn.tests.ui.UIMonitorUtilityTest;
import org.eclipse.team.svn.tests.workflow.ParameterizedWorkflowTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Default test suite factory
 * 
 * @author Alexander Gurov
 * @author Nicolas Peifer
 */
@RunWith(Suite.class)
@SuiteClasses({ AbstractOperationTest.class, DecoratorVariablesTest.class, JavaViewMenuEnablementTest.class,
		ParameterizedWorkflowTest.class, RepositoryLocationsManagementTest.class,
		RepositoryViewMenuEnablementTest.class, StateFilterTest.class, SVNTeamMoveDeleteHookTest.class,
		UIMonitorUtilityTest.class })
public class AllTests {
	// no implementation needed
}
