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
import org.eclipse.team.svn.core.connector.ISVNCredentialsPrompt;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.utility.ILoggedOperationFactory;

/**
 * This interface allows us to provide repository management options to IRepositoryLocation instances
 * 
 * @author Alexander Gurov
 */
public interface IOptionProvider {
	/**
	 * Preferred SVN connector plug-in ID
	 */
	String SVN_CONNECTOR_ID = "svnConnectorId"; //$NON-NLS-1$

	/**
	 * Default trunk folder name
	 */
	String DEFAULT_TRUNK_NAME = "defaultTrunkName"; //$NON-NLS-1$

	/**
	 * Default branches folder name
	 */
	String DEFAULT_BRANCHES_NAME = "defaultBranchesName"; //$NON-NLS-1$

	/**
	 * Default tags folder name
	 */
	String DEFAULT_TAGS_NAME = "defaultTagsName"; //$NON-NLS-1$

	/**
	 * <code>true</code> if persistent SSH connections are enabled, <code>false<code> or <code>null</code> otherwise
	 */
	String PERSISTENT_SSH_ENABLED = "persistentSSHEnabled"; //$NON-NLS-1$

	/**
	 * <code>true</code> if SVN status cache is enabled, <code>false<code> or <code>null</code> otherwise
	 */
	String SVN_CACHE_ENABLED = "svnCacheEnabled"; //$NON-NLS-1$

	/**
	 * <code>true</code> if "text" MIME-type should be set, <code>false<code> or <code>null</code> otherwise
	 */
	String TEXT_MIME_TYPE_REQUIRED = "textMIMETypeRequired"; //$NON-NLS-1$

	/**
	 * <code>true</code> if auto-share is enabled, <code>false<code> or <code>null</code> otherwise
	 */
	String AUTOMATIC_PROJECT_SHARE_ENABLED = "automaticProjectShareEnabled"; //$NON-NLS-1$

	/**
	 * <code>true</code> if "commit derived resources" option is enabled, <code>false<code> or <code>null</code> otherwise
	 */
	String COMMIT_DERIVED_ENABLED = "commitDerivedEnabled"; //$NON-NLS-1$

	IOptionProvider DEFAULT = new AbstractOptionProvider() {
		@Override
		public String getId() {
			return "org.eclipse.team.svn.core.optionprovider"; //$NON-NLS-1$
		}
	};

	/**
	 * Returns option provider ID
	 * 
	 * @return {@link String}
	 */
	String getId();

	/**
	 * Returns a set of option provider identifiers this one is superior to or <code>null</code> if there are none.
	 * 
	 * @return String []
	 */
	String[] getCoveredProviders();

	/**
	 * Returns read-only files modification validator
	 * 
	 * @return read-only files modification validator
	 */
	FileModificationValidator getFileModificationValidator();

	/**
	 * Provides credentials prompt call-back
	 * 
	 * @return credentials prompt call-back
	 */
	ISVNCredentialsPrompt getCredentialsPrompt();

	/**
	 * Provide logged operation factory which allows to override exceptions handling
	 * 
	 * @return logged operation factory instance
	 */
	ILoggedOperationFactory getLoggedOperationFactory();

	/**
	 * Installs additional handlers into project set processing workflow
	 * 
	 * @param op
	 *            project set processing workflow
	 */
	void addProjectSetCapabilityProcessing(CompositeOperation op);

	/**
	 * Returns set of automatic properties
	 * 
	 * @param template
	 *            resource name template
	 * @return set of properties
	 */
	SVNProperty[] getAutomaticProperties(String template);

	/**
	 * Provides access to internationalization strings
	 * 
	 * @return nationalized value
	 */
	String getResource(String key);

	/**
	 * Returns <code>true</code> if the option is a <code>boolean</code> value and set to <code>true</code>
	 * 
	 * @param key
	 *            the option's name
	 * @return
	 */
	boolean is(String key);

	/**
	 * Returns <code>true</code> if the option is set and is not <code>null</code>
	 * 
	 * @param key
	 *            the option's name
	 * @return
	 */
	boolean has(String key);

	/**
	 * Returns the string value of the option. If the value is not a string returns <code>null</code>;
	 * 
	 * @param key
	 *            the option's name
	 * @return
	 */
	String getString(String key);

	/**
	 * Returns the specified option value
	 * 
	 * @param key
	 *            option name
	 * @return
	 */
	Object get(String key);
}
