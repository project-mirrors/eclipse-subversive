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
 *    Alessandro Nistico - [patch] Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.tests.core;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNConnectorCancelException;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IConsoleStream;
import org.junit.Test;

/**
 * Tests for the abstract class.
 * 
 * @author Alessandro Nistico
 */
public class AbstractOperationTest {

	@Test
	public void testSetOperationName() {
		AbstractActionOperation op = new AbstractActionOperation("old name", SVNMessages.class) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
			}

			@Override
			public ISchedulingRule getSchedulingRule() {
				return null;
			}
		};
		assertEquals("old name", op.getOperationName());
		op.setOperationName("new name");
		assertEquals("new name", op.getOperationName());
	}

	@Test
	public void testGetExecutionState() {
		// test the failure of an execution
		AbstractActionOperation op = new AbstractActionOperation("name", SVNMessages.class) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				throw new Exception();
			}

			@Override
			public ISchedulingRule getSchedulingRule() {
				return null;
			}
		};
		op.run(new NullProgressMonitor());
		assertEquals(IActionOperation.ERROR, op.getExecutionState());
	}

	@Test
	public void testRun() {
		// test the cancellation of an execution
		AbstractActionOperation op = new AbstractActionOperation("name", SVNMessages.class) {
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				monitor.setCanceled(true);
				reportStatus(new IStatus() {
					@Override
					public IStatus[] getChildren() {
						return null;
					}

					@Override
					public int getCode() {
						return 0;
					}

					@Override
					public Throwable getException() {
						return new SVNConnectorCancelException("Cancelled");
					}

					@Override
					public String getMessage() {
						return null;
					}

					@Override
					public String getPlugin() {
						return null;
					}

					@Override
					public int getSeverity() {
						return IStatus.CANCEL;
					}

					@Override
					public boolean isMultiStatus() {
						return false;
					}

					@Override
					public boolean isOK() {
						return false;
					}

					@Override
					public boolean matches(int severityMask) {
						return false;
					}
				});
			}

			@Override
			public ISchedulingRule getSchedulingRule() {
				return null;
			}
		};

		op.setConsoleStream(new IConsoleStream() {
			@Override
			public void doComplexWrite(Runnable runnable) {
			}

			@Override
			public void markCancelled() {
			}

			@Override
			public void markEnd() {
			}

			@Override
			public void markStart(String data) {
			}

			@Override
			public void write(int severity, String data) {
			}
		});
		op.run(new NullProgressMonitor());
	}

	@Test
	public void testGetShortErrorMessage() {
		String expected = "SVN: 'op name' operation finished with error: Throwable";
		class MockAbstractActionOperation extends AbstractActionOperation {
			public MockAbstractActionOperation() {
				super("op name", SVNMessages.class);
			}

			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
			}

			@Override
			public ISchedulingRule getSchedulingRule() {
				return null;
			}

			@Override
			public String getShortErrorMessage(Throwable t) {
				return super.getShortErrorMessage(t);
			}
		}
		MockAbstractActionOperation op = new MockAbstractActionOperation();
		assertEquals(expected, op.getShortErrorMessage(new Throwable("Throwable")));
	}
}
