/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies, Polarion Software and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.discovery.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.osgi.util.NLS;

/**
 * A utility for accessing web resources
 * 
 * @author David Green
 * @author Igor Burilo
 */
public class WebUtil {
	/**
	 * implementors are capable of processing character content
	 * 
	 * @see WebUtil#readResource(AbstractWebLocation, TextContentProcessor, IProgressMonitor)
	 */
	public interface TextContentProcessor {
		public void process(Reader reader) throws IOException;
	}

	/**
	 * Download an HTTP-based resource
	 * 
	 * @param target
	 *            the target file to which the content is saved
	 * @param location
	 *            the web location of the content
	 * @param monitor
	 *            the monitor
	 * @throws IOException
	 *             if a network or IO problem occurs
	 */
	public static void downloadResource(File target, AbstractWebLocation location, IProgressMonitor monitor)
			throws IOException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(NLS.bind(Messages.WebUtil_task_retrievingUrl, location.getUrl()), IProgressMonitor.UNKNOWN);
		try {
			HttpClient client = new HttpClient();
			org.eclipse.mylyn.commons.net.WebUtil.configureHttpClient(client, ""); //$NON-NLS-1$

			GetMethod method = new GetMethod(location.getUrl());
			try {
				HostConfiguration hostConfiguration = org.eclipse.mylyn.commons.net.WebUtil.createHostConfiguration(
						client, location, monitor);
				int result = org.eclipse.mylyn.commons.net.WebUtil.execute(client, hostConfiguration, method, monitor);
				if (result == HttpStatus.SC_OK) {
					InputStream in = org.eclipse.mylyn.commons.net.WebUtil.getResponseBodyAsStream(method, monitor);
					try {
						in = new BufferedInputStream(in);
						OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
						try {
							int i;
							while ((i = in.read()) != -1) {
								out.write(i);
							}
						} catch (IOException e) {
							// avoid partial content
							out.close();
							target.delete();
							throw e;
						} finally {
							out.close();
						}
					} finally {
						in.close();
					}
				} else {
					throw new IOException(NLS.bind(Messages.WebUtil_cannotDownload, location.getUrl(), result));
				}
			} finally {
				method.releaseConnection();
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Read a web-based resource at the specified location using the given processor.
	 * 
	 * @param location
	 *            the web location of the content
	 * @param processor
	 *            the processor that will handle content
	 * @param monitor
	 *            the monitor
	 * @throws IOException
	 *             if a network or IO problem occurs
	 */
	public static void readResource(AbstractWebLocation location, TextContentProcessor processor,
			IProgressMonitor monitor) throws IOException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(NLS.bind(Messages.WebUtil_task_retrievingUrl, location.getUrl()), IProgressMonitor.UNKNOWN);
		try {
			HttpClient client = new HttpClient();
			org.eclipse.mylyn.commons.net.WebUtil.configureHttpClient(client, ""); //$NON-NLS-1$

			GetMethod method = new GetMethod(location.getUrl());
			try {
				HostConfiguration hostConfiguration = org.eclipse.mylyn.commons.net.WebUtil.createHostConfiguration(
						client, location, monitor);
				int result = org.eclipse.mylyn.commons.net.WebUtil.execute(client, hostConfiguration, method, monitor);
				if (result == HttpStatus.SC_OK) {
					InputStream in = org.eclipse.mylyn.commons.net.WebUtil.getResponseBodyAsStream(method, monitor);
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(in,
								method.getResponseCharSet()));
						processor.process(reader);
					} finally {
						in.close();
					}
				} else {
					throw new IOException(NLS.bind(Messages.WebUtil_cannotDownload, location.getUrl(), result));
				}
			} finally {
				method.releaseConnection();
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Verify availability of resources at the given web locations. Normally this would be done using an HTTP HEAD.
	 * 
	 * @param locations
	 *            the locations of the resource to verify
	 * @param one
	 *            indicate if only one of the resources must exist
	 * @param monitor
	 *            the monitor
	 * @return true if the resource exists
	 */
	public static boolean verifyAvailability(List<? extends AbstractWebLocation> locations, boolean one,
			IProgressMonitor monitor) {
		if (locations.isEmpty() || locations.size() > 5) {
			throw new IllegalArgumentException();
		}
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(NLS.bind(Messages.WebUtil_task_verifyingUrl, locations.get(0).getUrl()),
				IProgressMonitor.UNKNOWN);
		try {
			HttpClient client = new HttpClient();
			org.eclipse.mylyn.commons.net.WebUtil.configureHttpClient(client, ""); //$NON-NLS-1$

			HeadMethod method = new HeadMethod();
			try {
				int countFound = 0;
				for (AbstractWebLocation location : locations) {
					try {
						method.setURI(new URI(location.getUrl(), true));
					} catch (URIException e) {
						if (!one) {
							break;
						}
					}
					HostConfiguration hostConfiguration = org.eclipse.mylyn.commons.net.WebUtil.createHostConfiguration(
							client, location, monitor);
					int result;
					try {
						result = org.eclipse.mylyn.commons.net.WebUtil.execute(client, hostConfiguration, method,
								monitor);
					} catch (IOException e) {
						if (!one || e instanceof UnknownHostException) {
							return false;
						}
						continue;
					}
					if (result == HttpStatus.SC_OK) {
						++countFound;
						if (one) {
							return true;
						}
					} else {
						if (!one) {
							return false;
						}
					}
				}
				return countFound == locations.size();
			} finally {
				method.releaseConnection();
			}
		} finally {
			monitor.done();
		}
	}
}
