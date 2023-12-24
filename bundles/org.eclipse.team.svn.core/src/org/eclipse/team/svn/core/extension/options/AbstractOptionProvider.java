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

package org.eclipse.team.svn.core.extension.options;

import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;

/**
 * The abstract implementation of the IOptionProvider interface
 * 
 * @author Alexander Gurov
 */
public abstract class AbstractOptionProvider implements IOptionProvider {
	@Override
	public String[] getCoveredProviders() {
		return null;
	}

	@Override
	public ISVNCredentialsPrompt getCredentialsPrompt() {
		return null;
	}

	@Override
	public ILoggedOperationFactory getLoggedOperationFactory() {
		return ILoggedOperationFactory.DEFAULT;
	}

	@Override
	public void addProjectSetCapabilityProcessing(CompositeOperation op) {
	}

	@Override
	public FileModificationValidator getFileModificationValidator() {
		return null;
	}

	@Override
	public SVNProperty[] getAutomaticProperties(String template) {
		return new SVNProperty[0];
	}

	@Override
	public String getResource(String key) {
		return SVNMessages.getErrorString(key);
	}

	@Override
	public boolean is(String key) {
		Object value = get(key);
		return value instanceof Boolean && ((Boolean) value).booleanValue();
	}

	@Override
	public boolean has(String key) {
		return get(key) != null;
	}

	@Override
	public String getString(String key) {
		Object value = get(key);
		return value instanceof String ? (String) value : null;
	}

	@Override
	public Object get(String key) {
		if (IOptionProvider.SVN_CONNECTOR_ID.equals(key)) {
			return getSVNConnectorId();
		}
		if (IOptionProvider.DEFAULT_TRUNK_NAME.equals(key)) {
			return getDefaultTrunkName();
		}
		if (IOptionProvider.DEFAULT_BRANCHES_NAME.equals(key)) {
			return getDefaultBranchesName();
		}
		if (IOptionProvider.DEFAULT_TAGS_NAME.equals(key)) {
			return getDefaultTagsName();
		}
		if (IOptionProvider.PERSISTENT_SSH_ENABLED.equals(key)) {
			return isPersistentSSHEnabled();
		}
		if (IOptionProvider.TEXT_MIME_TYPE_REQUIRED.equals(key)) {
			return isTextMIMETypeRequired();
		}
		if (IOptionProvider.SVN_CACHE_ENABLED.equals(key)) {
			return isSVNCacheEnabled();
		}
		if (IOptionProvider.AUTOMATIC_PROJECT_SHARE_ENABLED.equals(key)) {
			return isAutomaticProjectShareEnabled();
		}
		return null;
	}

	protected String getSVNConnectorId() {
		return ISVNConnectorFactory.DEFAULT_ID;
	}

	protected String getDefaultTrunkName() {
		return "trunk"; //$NON-NLS-1$
	}

	protected String getDefaultBranchesName() {
		return "branches"; //$NON-NLS-1$
	}

	protected String getDefaultTagsName() {
		return "tags"; //$NON-NLS-1$
	}

	protected boolean isAutomaticProjectShareEnabled() {
		return false;
	}

	protected boolean isSVNCacheEnabled() {
		return true;
	}

	protected boolean isTextMIMETypeRequired() {
		return true;
	}

	protected boolean isPersistentSSHEnabled() {
		return true;
	}

	protected boolean isCommitDerivedEnabled() {
		return false;
	}
}
