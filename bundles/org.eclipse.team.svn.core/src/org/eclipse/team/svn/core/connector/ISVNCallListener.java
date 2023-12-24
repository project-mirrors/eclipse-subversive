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

package org.eclipse.team.svn.core.connector;

import java.util.Map;

/**
 * Change list information call-back interface
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNCallListener {
	String GET_CONFIG_DIRECTORY = "getConfigDirectory";

	String SET_CONFIG_DIRECTORY = "setConfigDirectory";

	String SET_USERNAME = "setUsername";

	String SET_PASSWORD = "setPassword";

	String SET_PROMPT = "setPrompt";

	String GET_PROMPT = "getPrompt";

	String SET_SSH_CREDENTIALS = "setSSHCredentials";

	String SET_NOTIFICATION_CALLBACK = "setNotificationCallback";

	String GET_NOTIFICATION_CALLBACK = "getNotificationCallback";

	String CHECKOUT = "checkout";

	String LOCK = "lock";

	String UNLOCK = "unlock";

	String ADD = "add";

	String COMMIT = "commit";

	String UPDATE = "update";

	String SWITCH = "switchTo";

	String REVERT = "revert";

	String STATUS = "status";

	String RELOCATE = "relocate";

	String CLEANUP = "cleanup";

	String MERGE = "merge";

	String MERGE_TWO = "mergeTwo";

	String MERGE_REINTEGRATE = "mergeReintegrate";

	String GET_MERGE_INFO = "getMergeInfo";

	String LIST_MERGE_INFO_LOG = "listMergeInfoLog";

	String SUGGEST_MERGE_SOURCES = "suggestMergeSources";

	String RESOLVE = "resolve";

	String SET_CONFLICT_RESOLVER = "setConflictResolver";

	String GET_CONFLICT_RESOLVER = "getConflictResolver";

	String ADD_TO_CHANGE_LIST = "addToChangeList";

	String REMOVE_FROM_CHANGE_LISTS = "removeFromChangeLists";

	String DUMP_CHANGE_LISTS = "dumpChangeLists";

	String IMPORT = "importTo";

	String EXPORT = "exportTo";

	String DIFF_FILE = "diff";

	String DIFF_TWO_FILE = "diffTwoFile";

	String DIFF_STREAM = "diffStream";

	String DIFF_TWO_STREAM = "diffTwoStream";

	String DIFF_STATUS = "diffStatus";

	String DIFF_STATUS_TWO = "diffStatusTwo";

	String GET_INFO = "getInfo";

	String STREAM_FILE_CONTENT = "streamFileContent";

	String MKDIR = "mkdir";

	String MOVE_REMOTE = "moveRemote";

	String MOVE_LOCAL = "moveLocal";

	String COPY_REMOTE = "copyRemote";

	String COPY_LOCAL = "copyLocal";

	String REMOVE_REMOTE = "removeRemote";

	String REMOVE_LOCAL = "removeLocal";

	String LIST_HISTORY_LOG = "listHistoryLog";

	String ANNOTATE = "annotate";

	String LIST = "list";

	String GET_PROPERTIES = "getProperties";

	String GET_PROPERTY = "getProperty";

	String SET_PROPERTY_LOCAL = "setPropertyLocal";

	String SET_PROPERTY_REMOTE = "setPropertyRemote";

	String LIST_REVISION_PROPERTIES = "listRevisionProperties";

	String GET_REVISION_PROPERTY = "getRevisionProperty";

	String SET_REVISION_PROPERTY = "setRevisionProperty";

	String UPGRADE = "upgrade";

	String PATCH = "patch";

	String GET_CONFIGURATION_EVENT_HANDLER = "getConfigurationEventHandler";

	String SET_CONFIGURATION_EVENT_HANDLER = "setConfigurationEventHandler";

	String VACUUM = "vacuum";

	String CREATE = "createRepository";

	String DELTIFY = "deltify";

	String HOT_COPY = "hotCopy";

	String DUMP = "dump";

	String LIST_DB_LOGS = "listDBLogs";

	String LOAD = "load";

	String LIST_TRANSACTIONS = "listTransactions";

	String RECOVER = "recover";

	String FREEZE = "freeze";

	String REMOVE_TRANSACTIONS = "removeTransaction";

	String SET_REPOSITORY_REVISION_PROPERTY = "setReppositoryRevisionProperty";

	String VERIFY = "verify";

	String LIST_LOCKS = "listLocks";

	String REMOVE_LOCKS = "removeLocks";

	String REPOSITORY_UPGRADE = "repositoryUpgrade";

	String PACK = "pack";

	/**
	 * Allows to modify parameters before call (for example, you can wrap progress monitor in order to intercept progress notifications)
	 * 
	 * @param methodName
	 *            the called method name
	 * @param parameters
	 *            the call parameters
	 */
	void asked(String methodName, Map<String, Object> parameters);

	/**
	 * The method is called if SVN call was completed successfully.
	 * 
	 * @param methodName
	 *            the called method name
	 * @param parameters
	 *            the call parameters
	 * @param returnValue
	 *            the return value if any, null otherwise
	 */
	void succeeded(String methodName, Map<String, Object> parameters, Object returnValue);

	/**
	 * The method is called if there was a failure while calling SVN.
	 * 
	 * @param methodName
	 *            the called method name
	 * @param parameters
	 *            the call parameters
	 * @param exception
	 *            the failure reason
	 */
	void failed(String methodName, Map<String, Object> parameters, SVNConnectorException exception);
}
