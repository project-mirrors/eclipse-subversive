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

package org.eclipse.team.svn.core.operation.file;

import java.io.File;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;

/**
 * Abstract class which allows to operate with java.io.File
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractFileOperation extends AbstractActionOperation {
	public static final LockingRule EXCLUSIVE = new LockingRule();

	private IFileProvider provider;

	private File[] files;

	public AbstractFileOperation(String operationName, Class<? extends NLS> messagesClass, File[] files) {
		super(operationName, messagesClass);
		this.files = files;
	}

	public AbstractFileOperation(String operationName, Class<? extends NLS> messagesClass, IFileProvider provider) {
		super(operationName, messagesClass);
		this.provider = provider;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		if (files == null) {
			return AbstractFileOperation.EXCLUSIVE;
		}
		HashSet<ISchedulingRule> ruleSet = new HashSet<>();
		for (File file : files) {
			ruleSet.add(this.getSchedulingRule(file));
		}
		return ruleSet.size() == 1
				? (ISchedulingRule) ruleSet.iterator().next()
				: new MultiRule(ruleSet.toArray(new IResource[ruleSet.size()]));
	}

	protected File[] operableData() {
		return files == null ? provider.getFiles() : files;
	}

	protected ISchedulingRule getSchedulingRule(File file) {
		File parent = file.getParentFile();
		return new LockingRule(parent != null ? parent : file);
	}

	public static class LockingRule implements ISchedulingRule {
		protected IPath filePath;

		// always exclusive
		public LockingRule() {
			this((IPath) null);
		}

		public LockingRule(File file) {
			this(file == null ? null : new Path(file.getAbsolutePath()));
		}

		public LockingRule(IPath filePath) {
			this.filePath = filePath;
		}

		@Override
		public boolean isConflicting(ISchedulingRule arg) {
			if (arg instanceof LockingRule) {
				LockingRule rule = (LockingRule) arg;
				return filePath == null || rule.filePath == null || filePath.isPrefixOf(rule.filePath)
						|| rule.filePath.isPrefixOf(filePath);
			}
			return false;
		}

		@Override
		public boolean contains(ISchedulingRule arg) {
			if (this == arg) {
				return true;
			}
			if (arg instanceof LockingRule) {
				LockingRule rule = (LockingRule) arg;
				return filePath == rule.filePath || filePath.isPrefixOf(rule.filePath);
			}
			if (arg instanceof MultiRule) {
				MultiRule rule = (MultiRule) arg;
				ISchedulingRule[] children = rule.getChildren();
				for (ISchedulingRule child : children) {
					if (!contains(child)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return filePath == null ? 0 : filePath.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LockingRule) {
				LockingRule rule = (LockingRule) obj;
				return filePath == rule.filePath || filePath != null && filePath.equals(rule.filePath);
			}
			return false;
		}

	}

}
