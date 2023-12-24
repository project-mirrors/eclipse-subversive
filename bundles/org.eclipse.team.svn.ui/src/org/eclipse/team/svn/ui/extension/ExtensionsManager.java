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
 *    Andrej Zachar - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ui.extension;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.properties.IPredefinedPropertySet;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.decorator.IDecorationFilter;
import org.eclipse.team.svn.ui.extension.factory.ICheckoutFactory;
import org.eclipse.team.svn.ui.extension.factory.ICommitActionFactory;
import org.eclipse.team.svn.ui.extension.factory.IHistoryViewFactory;
import org.eclipse.team.svn.ui.extension.factory.IReporter;
import org.eclipse.team.svn.ui.extension.factory.IReporterFactory;
import org.eclipse.team.svn.ui.extension.factory.IReporterFactory.ReportType;
import org.eclipse.team.svn.ui.extension.factory.IReportingDescriptor;
import org.eclipse.team.svn.ui.extension.factory.IShareProjectFactory;
import org.eclipse.team.svn.ui.extension.factory.ISynchronizeViewActionContributor;
import org.eclipse.team.svn.ui.extension.impl.DefaultCheckoutFactory;
import org.eclipse.team.svn.ui.extension.impl.DefaultCommitActionFactory;
import org.eclipse.team.svn.ui.extension.impl.DefaultDecorationFilter;
import org.eclipse.team.svn.ui.extension.impl.DefaultHistoryViewFactory;
import org.eclipse.team.svn.ui.extension.impl.DefaultShareProjectFactory;
import org.eclipse.team.svn.ui.extension.impl.DefaultSynchronizeViewActionContributor;

/**
 * Manager for extension components. Used to extend Subversive without direct dependencies.
 * 
 * @author Andrej Zachar
 */
public class ExtensionsManager {
	private static final String UI_EXTENSION_NAMESPACE = "org.eclipse.team.svn.ui"; //$NON-NLS-1$

	private ICommitActionFactory currentCommitFactory;

	private IHistoryViewFactory currentMessageFactory;

	private ICheckoutFactory currentCheckoutFactory;

	private IShareProjectFactory currentShareProjectFactory;

	private IDecorationFilter currentDecorationFilter;

	private ISynchronizeViewActionContributor currentActionContributor;

	private IReportingDescriptor[] reportingDescriptors;

	private IReporterFactory[] reporterFactories;

	private static ExtensionsManager instance;

	public synchronized static ExtensionsManager getInstance() {
		if (ExtensionsManager.instance == null) {
			ExtensionsManager.instance = new ExtensionsManager();
		}
		return ExtensionsManager.instance;
	}

	public ISynchronizeViewActionContributor getCurrentSynchronizeActionContributor() {
		if (currentActionContributor == null) {
			currentActionContributor = ExtensionsManager.getDefaultSynchronizeViewActionContributor();
		}
		return currentActionContributor;
	}

	public void setCurrentSynchronizeActionContributor(ISynchronizeViewActionContributor contributor) {
		currentActionContributor = contributor;
	}

	public IDecorationFilter getCurrentDecorationFilter() {
		if (currentDecorationFilter == null) {
			currentDecorationFilter = ExtensionsManager.getDefaultDecorationFilter();
		}
		return currentDecorationFilter;
	}

	public void setCurrentDecorationFilter(IDecorationFilter filter) {
		currentDecorationFilter = filter;
	}

	public ICheckoutFactory getCurrentCheckoutFactory() {
		if (currentCheckoutFactory == null) {
			currentCheckoutFactory = ExtensionsManager.getDefaultCheckoutFactory();
		}
		return currentCheckoutFactory;
	}

	public void setCurrentCheckoutFactory(ICheckoutFactory factory) {
		currentCheckoutFactory = factory;
	}

	public IShareProjectFactory getCurrentShareProjectFactory() {
		if (currentShareProjectFactory == null) {
			currentShareProjectFactory = ExtensionsManager.getDefaultShareProjectFactory();
		}
		return currentShareProjectFactory;
	}

	public void setCurrentShareProjectFactory(IShareProjectFactory factory) {
		currentShareProjectFactory = factory;
	}

	public IReportingDescriptor[] getReportingDescriptors() {
		return reportingDescriptors;
	}

	public IReporter getReporter(IReportingDescriptor descriptor, ReportType type) {
		for (IReporterFactory reporterFactory : reporterFactories) {
			IReporter reporter = reporterFactory.newReporter(descriptor, type);
			if (reporter != null) {
				return reporter;
			}
		}
		return null;
	}

	public ICommitActionFactory getCurrentCommitFactory() {
		if (currentCommitFactory == null) {
			currentCommitFactory = getDefaultTeamCommitFactory();
		}
		return currentCommitFactory;
	}

	public void setCurrentCommitFactory(ICommitActionFactory currentFactory) {
		currentCommitFactory = currentFactory;
	}

	public IHistoryViewFactory getCurrentMessageFactory() {
		if (currentMessageFactory == null) {
			currentMessageFactory = getDefaultCommitMessageFactory();
		}
		return currentMessageFactory;
	}

	public void setCurrentMessageFactory(IHistoryViewFactory currentMessageFactory) {
		this.currentMessageFactory = currentMessageFactory;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public IPredefinedPropertySet getPredefinedPropertySet() {
		return CoreExtensionsManager.instance().getPredefinedPropertySet();
	}

	public static ICommitActionFactory getDefaultTeamCommitFactory() {
		return new DefaultCommitActionFactory();
	}

	public static IHistoryViewFactory getDefaultCommitMessageFactory() {
		return new DefaultHistoryViewFactory();
	}

	public static ICheckoutFactory getDefaultCheckoutFactory() {
		return new DefaultCheckoutFactory();
	}

	public static IShareProjectFactory getDefaultShareProjectFactory() {
		return new DefaultShareProjectFactory();
	}

	public static IDecorationFilter getDefaultDecorationFilter() {
		return new DefaultDecorationFilter();
	}

	public static ISynchronizeViewActionContributor getDefaultSynchronizeViewActionContributor() {
		return new DefaultSynchronizeViewActionContributor();
	}

	private ExtensionsManager() {
		currentDecorationFilter = (IDecorationFilter) loadUIExtension("decoration"); //$NON-NLS-1$
		currentMessageFactory = (IHistoryViewFactory) loadUIExtension("history"); //$NON-NLS-1$
		currentCommitFactory = (ICommitActionFactory) loadUIExtension("commit"); //$NON-NLS-1$
		currentCheckoutFactory = (ICheckoutFactory) loadUIExtension("checkout"); //$NON-NLS-1$
		currentShareProjectFactory = (IShareProjectFactory) loadUIExtension("shareproject"); //$NON-NLS-1$
		currentActionContributor = (ISynchronizeViewActionContributor) loadUIExtension("synchronizeActionContribution"); //$NON-NLS-1$
		Object[] extensions = loadUIExtensions("reportingdescriptor"); //$NON-NLS-1$
		reportingDescriptors = Arrays.asList(extensions).toArray(new IReportingDescriptor[extensions.length]);
		extensions = loadUIExtensions("reporterfactory"); //$NON-NLS-1$
		reporterFactories = Arrays.asList(extensions).toArray(new IReporterFactory[extensions.length]);
		Arrays.sort(reporterFactories, (o1, o2) -> {
			IReporterFactory f1 = (IReporterFactory) o1;
			IReporterFactory f2 = (IReporterFactory) o2;
			return f1.isCustomEditorSupported() ^ f2.isCustomEditorSupported()
					? f1.isCustomEditorSupported() ? -1 : 1
					: 0;
		});
	}

	private Object loadUIExtension(String extensionPoint) {
		Object[] extensions = loadUIExtensions(extensionPoint);
		return extensions.length == 0 ? null : extensions[0];
	}

	private Object[] loadUIExtensions(String extensionPoint) {
		return loadExtensions(ExtensionsManager.UI_EXTENSION_NAMESPACE, extensionPoint);
	}

	private Object[] loadExtensions(String namespace, String extensionPoint) {
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(namespace, extensionPoint);
		IExtension[] extensions = extension.getExtensions();
		ArrayList<Object> retVal = new ArrayList<>();
		for (IExtension extension2 : extensions) {
			IConfigurationElement[] configElements = extension2.getConfigurationElements();
			for (IConfigurationElement configElement : configElements) {
				try {
					retVal.add(configElement.createExecutableExtension("class")); //$NON-NLS-1$
				} catch (CoreException ex) {
					LoggedOperation.reportError(SVNUIMessages.Error_LoadUIExtension, ex);
				}
			}
		}
		return retVal.toArray();
	}

}
