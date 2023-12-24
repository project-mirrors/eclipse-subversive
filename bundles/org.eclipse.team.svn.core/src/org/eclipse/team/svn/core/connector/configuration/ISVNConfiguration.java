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

package org.eclipse.team.svn.core.connector.configuration;

/**
 * SVN configuration access object interface. Manipulates in-memory information.
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library is not EPL
 * compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is providing our own connector
 * interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public interface ISVNConfiguration {
	/*
	 * The following constants are section and option names from the "config" configuration file.
	 */
	String SECTION_AUTH = "auth";

	String PASSWORD_STORES = "password-stores";

	String KWALLET_WALLET = "kwallet-wallet";

	String KWALLET_SVN_APPLICATION_NAME_WITH_PID = "kwallet-svn-application-name-with-pid";

	String SSL_CLIENT_CERT_FILE_PROMPT = "ssl-client-cert-file-prompt";

	String SECTION_HELPERS = "helpers";

	String EDITOR_CMD = "editor-cmd";

	String DIFF_CMD = "diff-cmd";

	String DIFF_EXTENSIONS = "diff-extensions";

	String DIFF3_CMD = "diff3-cmd";

	String DIFF3_HAS_PROGRAM_ARG = "diff3-has-program-arg";

	String MERGE_TOOL_CMD = "merge-tool-cmd";

	String SECTION_MISCELLANY = "miscellany";

	String GLOBAL_IGNORES = "global-ignores";

	String LOG_ENCODING = "log-encoding";

	String USE_COMMIT_TIMES = "use-commit-times";

	String ENABLE_AUTO_PROPS = "enable-auto-props";

	String ENABLE_MAGIC_FILE = "enable-magic-file";

	String NO_UNLOCK = "no-unlock";

	String MIMETYPES_FILE = "mime-types-file";

	String PRESERVED_CF_EXTS = "preserved-conflict-file-exts";

	String INTERACTIVE_CONFLICTS = "interactive-conflicts";

	String MEMORY_CACHE_SIZE = "memory-cache-size";

	String DIFF_IGNORE_CONTENT_TYPE = "diff-ignore-content-type";

	String SECTION_TUNNELS = "tunnels";

	String SECTION_AUTO_PROPS = "auto-props";

	String SECTION_WORKING_COPY = "working-copy";

	String SQLITE_EXCLUSIVE = "exclusive-locking";

	String SQLITE_EXCLUSIVE_CLIENTS = "exclusive-locking-clients";

	String SQLITE_BUSY_TIMEOUT = "busy-timeout";

	/*
	 * The following constants are section and option names from the "servers" configuration file.
	 */
	String SECTION_GROUPS = "groups";

	String SECTION_GLOBAL = "global";

	String HTTP_PROXY_HOST = "http-proxy-host";

	String HTTP_PROXY_PORT = "http-proxy-port";

	String HTTP_PROXY_USERNAME = "http-proxy-username";

	String HTTP_PROXY_PASSWORD = "http-proxy-password";

	String HTTP_PROXY_EXCEPTIONS = "http-proxy-exceptions";

	String HTTP_TIMEOUT = "http-timeout";

	String HTTP_COMPRESSION = "http-compression";

	String NEON_DEBUG_MASK = "neon-debug-mask";

	String HTTP_AUTH_TYPES = "http-auth-types";

	String SSL_AUTHORITY_FILES = "ssl-authority-files";

	String SSL_TRUST_DEFAULT_CA = "ssl-trust-default-ca";

	String SSL_CLIENT_CERT_FILE = "ssl-client-cert-file";

	String SSL_CLIENT_CERT_PASSWORD = "ssl-client-cert-password";

	String SSL_PKCS11_PROVIDER = "ssl-pkcs11-provider";

	String HTTP_LIBRARY = "http-library";

	String STORE_PASSWORDS = "store-passwords";

	String STORE_PLAINTEXT_PASSWORDS = "store-plaintext-passwords";

	String STORE_AUTH_CREDS = "store-auth-creds";

	String STORE_SSL_CLIENT_CERT_PP = "store-ssl-client-cert-pp";

	String STORE_SSL_CLIENT_CERT_PP_PLAINTEXT = "store-ssl-client-cert-pp-plaintext";

	String USERNAME = "username";

	String HTTP_BULK_UPDATES = "http-bulk-updates";

	String HTTP_MAX_CONNECTIONS = "http-max-connections";

	String HTTP_CHUNKED_REQUESTS = "http-chunked-requests";

	String SERF_LOG_COMPONENTS = "serf-log-components";

	String SERF_LOG_LEVEL = "serf-log-level";

	/**
	 * "true" value in configuration.
	 */
	String TRUE = "TRUE";

	/**
	 * "false" value in configuration.
	 */
	String FALSE = "FALSE";

	/**
	 * "ask" value in configuration.
	 */
	String ASK = "ASK";

	/**
	 * Returns a reference to the "config" settings category.
	 * 
	 * @return ISVNConfigurationCategory
	 */
	ISVNConfigurationCategory getConfigCategory();

	/**
	 * Returns a reference to the "servers" settings category.
	 * 
	 * @return ISVNConfigurationCategory
	 */
	ISVNConfigurationCategory getServersCategory();
}
