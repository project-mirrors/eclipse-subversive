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

package org.eclipse.team.svn.core.utility;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.team.svn.core.SVNMessages;

/**
 * SVN-specific URL stream handler
 * 
 * @author Alexander Gurov
 */
public class SVNURLStreamHandler extends URLStreamHandler {
	protected URL url;

	public SVNURLStreamHandler() {
	}

	public URL getURL() {
		return url;
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return null;
	}

	public void setHost(String host) {
		this.setURL(url, url.getProtocol(), host, url.getPort(), url.getAuthority(), url.getUserInfo(), url.getPath(),
				url.getQuery(), url.getRef());
	}

	@Override
	protected void parseURL(URL u, String spec, int start, int limit) {
		String protocol = u.getProtocol();
		if (!protocol.equals("file") && //$NON-NLS-1$
				!protocol.equals("svn") && //$NON-NLS-1$
				!protocol.equals("http") && //$NON-NLS-1$
				!protocol.equals("https") && //$NON-NLS-1$
				!protocol.equals("svn+ssh")) { //$NON-NLS-1$
			String errMessage = SVNMessages.formatErrorString("Error_UnknownProtocol", new String[] { protocol }); //$NON-NLS-1$
			throw new RuntimeException(errMessage);
		}
		url = u;
		super.parseURL(u, spec, start, limit);
	}

}
