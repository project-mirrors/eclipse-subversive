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

package org.eclipse.team.svn.core.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.extension.crashrecovery.IResolutionHelper;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.extension.factory.ThreadNameModifierFactory;
import org.eclipse.team.svn.core.extension.options.IIgnoreRecommendations;
import org.eclipse.team.svn.core.extension.options.IOptionProvider;
import org.eclipse.team.svn.core.extension.properties.IPredefinedPropertySet;
import org.eclipse.team.svn.core.extension.properties.PredefinedProperty;
import org.eclipse.team.svn.core.extension.properties.PredefinedPropertySet;
import org.eclipse.team.svn.core.operation.LoggedOperation;

/**
 * Manager for extension components. Used to extend Subversive without direct dependencies.
 * 
 * @author Alexander Gurov
 */
public class CoreExtensionsManager {
	public static final String EXTENSION_NAMESPACE = "org.eclipse.team.svn.core"; //$NON-NLS-1$

	public static final String SVN_CONNECTOR = "svnconnector"; //$NON-NLS-1$

	public static final String CORE_OPTIONS = "coreoptions"; //$NON-NLS-1$

	public static final String CRASH_RECOVERY = "crashrecovery"; //$NON-NLS-1$

	public static final String IGNORE_RECOMMENDATIONS = "resourceIgnoreRules"; //$NON-NLS-1$

	private HashMap<String, ISVNConnectorFactory> connectors;

	private HashSet<String> validConnectors;

	private HashMap<String, IOptionProvider> optionProviders = new HashMap<>();

	private String selectedOptionProviderId;

	private IResolutionHelper[] helpers;

	private IIgnoreRecommendations[] ignoreRecommendations;

	private ArrayList<IPredefinedPropertySet> svnPropertySets;

	private static CoreExtensionsManager instance = new CoreExtensionsManager();

	private boolean disableHelpers;

	private boolean initialized;

	public static CoreExtensionsManager instance() {
		if (CoreExtensionsManager.instance != null && !CoreExtensionsManager.instance.initialized) {
			synchronized (CoreExtensionsManager.class) {
				if (!CoreExtensionsManager.instance.initialized) {
					Object[] extensions = CoreExtensionsManager.loadCoreExtensions(CoreExtensionsManager.CORE_OPTIONS);
					HashSet<String> inferiors = new HashSet<>();
					for (int i = 0; i < extensions.length; i++) {
						IOptionProvider optProvider = (IOptionProvider) extensions[i];
						String id = CoreExtensionsManager.instance.registerOptionProvider(optProvider);
						if (i == extensions.length - 1) {
							CoreExtensionsManager.instance.selectOptionProvider(id);
						}
						String[] inferiorOnes = optProvider.getCoveredProviders();
						if (inferiorOnes != null) {
							Collections.addAll(inferiors, inferiorOnes);
						}
					}
					for (Object extension : extensions) {
						IOptionProvider optProvider = (IOptionProvider) extension;
						String id = optProvider.getId();
						if (!inferiors.contains(id)) {
							CoreExtensionsManager.instance.selectOptionProvider(id);
							break;
						}
					}
					extensions = CoreExtensionsManager.loadCoreExtensions(CoreExtensionsManager.CRASH_RECOVERY);
					CoreExtensionsManager.instance.helpers = Arrays.asList(extensions)
							.toArray(new IResolutionHelper[extensions.length]);
					extensions = CoreExtensionsManager.loadCoreExtensions(CoreExtensionsManager.IGNORE_RECOMMENDATIONS);
					CoreExtensionsManager.instance.ignoreRecommendations = Arrays.asList(extensions)
							.toArray(new IIgnoreRecommendations[extensions.length]);
					CoreExtensionsManager.instance.initialized = true;
				}

				PredefinedPropertySet pSet = null;
				CoreExtensionsManager.instance.svnPropertySets = new ArrayList<>();
				IExtensionPoint extension = Platform.getExtensionRegistry()
						.getExtensionPoint(CoreExtensionsManager.EXTENSION_NAMESPACE, "svnproperties"); //$NON-NLS-1$
				for (IExtension ext : extension.getExtensions()) {
					IConfigurationElement[] configElements = ext.getConfigurationElements();
					for (IConfigurationElement element : configElements) {
						if (element.getName() == "svnproperty") { //$NON-NLS-1$
							if (pSet == null) {
								pSet = new PredefinedPropertySet();
								CoreExtensionsManager.instance.svnPropertySets.add(pSet);
							}
							String name = element.getAttribute("name"); //$NON-NLS-1$
							String typeStr = element.getAttribute("type"); //$NON-NLS-1$
							typeStr = typeStr == null ? "common" : typeStr; //$NON-NLS-1$
							String revision = element.getAttribute("revision"); //$NON-NLS-1$
							String group = element.getAttribute("group"); //$NON-NLS-1$
							IConfigurationElement[] partNode = element.getChildren("description"); //$NON-NLS-1$
							String description = partNode != null && partNode.length > 0 ? partNode[0].getValue() : ""; //$NON-NLS-1$
							partNode = element.getChildren("defaultValue"); //$NON-NLS-1$
							String defaultValue = partNode != null && partNode.length > 0 ? partNode[0].getValue() : ""; //$NON-NLS-1$
							partNode = element.getChildren("validationRegexp"); //$NON-NLS-1$
							String validationRegexp = partNode != null && partNode.length > 0
									? partNode[0].getValue()
									: null;

							int type = PredefinedProperty.TYPE_NONE;
							if ("file".equals(typeStr)) { //$NON-NLS-1$
								type = PredefinedProperty.TYPE_FILE;
							} else if ("folder".equals(typeStr)) { //$NON-NLS-1$
								type = PredefinedProperty.TYPE_FOLDER;
							} else if ("common".equals(typeStr)) { //$NON-NLS-1$
								type = PredefinedProperty.TYPE_COMMON;
							}
							if (revision != null && Boolean.parseBoolean(revision)) {
								type |= PredefinedProperty.TYPE_REVISION;
							}
							if (group != null && Boolean.parseBoolean(group)) {
								type |= PredefinedProperty.TYPE_GROUP;
							}
							pSet.registerProperty(
									new PredefinedProperty(name, description, defaultValue, validationRegexp, type));
						} else { // svnpropertyset
							try {
								CoreExtensionsManager.instance.svnPropertySets
										.add((IPredefinedPropertySet) element.createExecutableExtension("class"));
							} catch (CoreException ex) {
								LoggedOperation.reportError(SVNMessages.getErrorString("Error_LoadExtensions"), ex); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}
		return CoreExtensionsManager.instance;
	}

	public IPredefinedPropertySet getPredefinedPropertySet() {
		PredefinedPropertySet retVal = new PredefinedPropertySet();
		for (IPredefinedPropertySet set : svnPropertySets) {
			retVal.registerProperties(set.getPredefinedProperties());
		}
		return retVal;
	}

	public IIgnoreRecommendations[] getIgnoreRecommendations() {
		return ignoreRecommendations;
	}

	public IResolutionHelper[] getResolutionHelpers() {
		return disableHelpers ? new IResolutionHelper[0] : helpers;
	}

	public void setResolutionHelpersDisabled(boolean disable) {
		disableHelpers = disable;
	}

	public boolean isResoultionHelpersDisabled() {
		return disableHelpers;
	}

	public IOptionProvider getOptionProvider(String id) {
		return optionProviders.containsKey(id) ? optionProviders.get(id) : IOptionProvider.DEFAULT;
	}

	public IOptionProvider getOptionProvider() {
		return this.getOptionProvider(selectedOptionProviderId);
	}

	public void setOptionProvider(IOptionProvider optionProvider) {
		selectOptionProvider(registerOptionProvider(optionProvider));
	}

	public void selectOptionProvider(String id) {
		selectedOptionProviderId = id;
	}

	public String getSelectedOptionProviderId() {
		return selectedOptionProviderId;
	}

	public String registerOptionProvider(IOptionProvider optionProvider) {
		optionProviders.put(optionProvider.getId(), optionProvider);
		return optionProvider.getId();
	}

	public Collection<String> getAccessibleClientIds() {
		initializeConnectors();
		return connectors.keySet();
	}

	public Collection<ISVNConnectorFactory> getAccessibleClients() {
		initializeConnectors();
		return connectors.values();
	}

	public ISVNConnectorFactory getSVNConnectorFactory() {
		String id = this.getOptionProvider().getString(IOptionProvider.SVN_CONNECTOR_ID);
		return this.getSVNConnectorFactory(id);
	}

	public ISVNConnectorFactory getSVNConnectorFactory(String id) {
		ISVNConnectorFactory retVal = getFirstValidConnector(id);
		if (retVal == null) {
			retVal = ISVNConnectorFactory.EMPTY;
		}
		return retVal;
	}

	public static boolean isExtensionsRegistered(String extensionPoint) {
		Object[] extensions = CoreExtensionsManager.loadCoreExtensions(extensionPoint);
		return extensions != null && extensions.length > 0;
	}

	private ISVNConnectorFactory getFirstValidConnector(String id) {
		initializeConnectors();

		if (validConnectors.contains(id)) {
			return connectors.get(id);
		} else if (validConnectors.contains(ISVNConnectorFactory.DEFAULT_ID)) {
			return connectors.get(ISVNConnectorFactory.DEFAULT_ID);
		}
		for (ISVNConnectorFactory connector : connectors.values()) {
			if (validConnectors.contains(connector.getId())) {
				return connector;
			}
		}
		return null;
	}

	private CoreExtensionsManager() {
	}

	private synchronized void initializeConnectors() {
		if (connectors == null) {
			synchronized (this) {
				if (connectors == null) {
					connectors = new HashMap<>();
					validConnectors = new HashSet<>();
					Object[] extensions = CoreExtensionsManager.loadCoreExtensions(CoreExtensionsManager.SVN_CONNECTOR);
					for (Object extension : extensions) {
						ISVNConnectorFactory factory = new ThreadNameModifierFactory(
								(ISVNConnectorFactory) extension);
						try {
							// extension point API changed and old connectors will be declined due to version changes or AbstractMethodError.
							if (factory.getCompatibilityVersion()
									.compareTo(ISVNConnectorFactory.CURRENT_COMPATIBILITY_VERSION) != 0) {
								continue;
							}
						} catch (Throwable ex) {
							continue;
						}
						connectors.put(factory.getId(), factory);
						validateClient(factory);
					}
				}
			}
		}
	}

	//NIC rename validateAndAddClient
	private void validateClient(ISVNConnectorFactory connector) {
		try {
			connector.createConnector().dispose();
			validConnectors.add(connector.getId());
		} catch (Throwable ex) {
			ex.printStackTrace();
			// do nothing
		}
	}

	private static Object[] loadCoreExtensions(String extensionPoint) {
		return CoreExtensionsManager.loadExtensions(CoreExtensionsManager.EXTENSION_NAMESPACE, extensionPoint);
	}

	private static Object[] loadExtensions(String namespace, String extensionPoint) {
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(namespace, extensionPoint);
		if (extension == null) {
			String errMessage = SVNMessages.formatErrorString("Error_InvalidExtensionPoint", //$NON-NLS-1$
					new String[] { namespace, extensionPoint });
			throw new RuntimeException(errMessage);
		}
		IExtension[] extensions = extension.getExtensions();
		ArrayList<Object> retVal = new ArrayList<>();
		for (IExtension extension2 : extensions) {
			IConfigurationElement[] configElements = extension2.getConfigurationElements();
			for (IConfigurationElement configElement : configElements) {
				try {
					retVal.add(configElement.createExecutableExtension("class")); //$NON-NLS-1$
				} catch (CoreException ex) {
					LoggedOperation.reportError(SVNMessages.getErrorString("Error_LoadExtensions"), ex); //$NON-NLS-1$
				}
			}
		}
		return retVal.toArray();
	}

}
