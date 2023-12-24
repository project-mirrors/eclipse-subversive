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

package org.eclipse.team.svn.core.utility;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.team.svn.core.SVNMessages;

/**
 * This subprogress monitor allow us to get current progress state
 * 
 * @author Alexander Gurov
 */
public class SubProgressMonitorWithInfo extends ProgressMonitorWrapper {
	protected double currentProgress;

	protected double unknownProgress;

	protected long lastTime;

	protected double parentTicks;

	protected double scale;

	protected int totalWork;

	protected String subTaskOp;

	public SubProgressMonitorWithInfo(IProgressMonitor monitor, double parentTicks) {
		super(monitor);
		this.parentTicks = parentTicks;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		lastTime = 0;
		currentProgress = 0;
		unknownProgress = 0;
		this.totalWork = totalWork > 0 ? totalWork : ProgressMonitorUtility.TOTAL_WORK;
		scale = parentTicks / this.totalWork;
	}

	@Override
	public void done() {
		subTaskOp = null;
		internalWorked(totalWork - currentProgress);
	}

	@Override
	public void internalWorked(double work) {
		if (currentProgress + work > totalWork) {
			work = totalWork - currentProgress;
		}
		if (currentProgress < totalWork) {
			currentProgress += work;
			super.internalWorked(work * scale);
		}
	}

	@Override
	public void worked(int work) {
		if (work == IProgressMonitor.UNKNOWN) {
			double delta = (totalWork - currentProgress) / totalWork;
			delta /= unknownProgress < 25
					? 0.5
					: unknownProgress < 50
							? 1
							: unknownProgress < 75 ? 2 : unknownProgress < 85 ? 8 : unknownProgress < 95 ? 25 : 100;
			unknownProgress += delta;
			int offset = (int) (unknownProgress - currentProgress);
			internalWorked(offset);
		} else {
			internalWorked(work);
		}
	}

	public void unknownProgress(int current) {
		worked(IProgressMonitor.UNKNOWN);
	}

	@Override
	public void subTask(String name) {
		long time = System.currentTimeMillis();
		// redraw four times per second or if operation was changed
		boolean operationChanged = subTaskOp == null || !name.startsWith(subTaskOp)
				|| name.charAt(subTaskOp.length()) != ':' || name.endsWith(SVNMessages.Progress_Done);
		if (lastTime == 0 || time - lastTime >= 250 || operationChanged) {
			lastTime = time;
			int idx = name.indexOf(':');
			if (idx != -1) {
				subTaskOp = name.endsWith(SVNMessages.Progress_Running) ? name.substring(0, idx) : null;
			}
			super.subTask(name);
		}
	}

	public int getCurrentProgress() {
		return (int) currentProgress;
	}

}
