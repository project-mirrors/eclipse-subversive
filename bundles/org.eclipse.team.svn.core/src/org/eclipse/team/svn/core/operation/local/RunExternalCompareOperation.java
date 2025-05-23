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
 *    Igor Burilo - Initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameterKind;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameters;
import org.eclipse.team.svn.core.operation.remote.GetFileContentOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;

/**
 * Run external compare editor operation implementation
 * 
 * @author Igor Burilo
 */
public class RunExternalCompareOperation extends CompositeOperation implements IExecutable {

	public final static String DOC_SCRIPT = "diff-doc.vbs"; //$NON-NLS-1$

	public final static String DOCX_SCRIPT = "diff-doc.vbs"; //$NON-NLS-1$

	public final static String XLS_SCRIPT = "diff-xls.vbs"; //$NON-NLS-1$

	public final static String XLSX_SCRIPT = "diff-xls.vbs"; //$NON-NLS-1$

	public final static String PPT_SCRIPT = "diff-ppt.vbs"; //$NON-NLS-1$

	public final static String PPTX_SCRIPT = "diff-ppt.vbs"; //$NON-NLS-1$

	public final static String ODT_SCRIPT = "diff-odX.vbs"; //$NON-NLS-1$

	public final static String ODS_SCRIPT = "diff-odX.vbs"; //$NON-NLS-1$

	protected ExternalCompareOperation externalCompareOperation;

	@Override
	public boolean isExecuted() {
		return externalCompareOperation.isExecuted();
	}

	public interface IExternalProgramParametersProvider {
		ExternalProgramParameters getExternalProgramParameters();
	}

	public static class DefaultExternalProgramParametersProvider implements IExternalProgramParametersProvider {
		protected ExternalProgramParameters externalProgramParameters;

		public DefaultExternalProgramParametersProvider(ExternalProgramParameters externalProgramParameters) {
			this.externalProgramParameters = externalProgramParameters;
		}

		@Override
		public ExternalProgramParameters getExternalProgramParameters() {
			return externalProgramParameters;
		}
	}

	/**
	 * Determine what compare to use: external or internal
	 * 
	 * @author Igor Burilo
	 */
	public static class DetectExternalCompareOperationHelper {

		protected DiffViewerSettings diffSettings;

		protected IRepositoryResource repositoryResource;

		protected IResource resource;

		protected boolean isDiff;

		protected ExternalProgramParameters externalProgramParams;

		public DetectExternalCompareOperationHelper(IResource resource, DiffViewerSettings diffSettings,
				boolean isDiff) {
			this.diffSettings = diffSettings;
			this.resource = resource;
			this.isDiff = isDiff;
		}

		public DetectExternalCompareOperationHelper(IRepositoryResource repositoryResource,
				DiffViewerSettings diffSettings, boolean isDiff) {
			this.diffSettings = diffSettings;
			this.repositoryResource = repositoryResource;
			this.isDiff = isDiff;
		}

		public void execute(IProgressMonitor monitor) {
			if (resource != null) {
				detectWithResource(monitor);
			} else if (repositoryResource != null) {
				detectWithRepositoryResource(monitor);
			}

			//check program path
			if (externalProgramParams != null) {
				String path = null;
				if (isDiff) {
					path = externalProgramParams.diffProgramPath;
				} else {
					path = externalProgramParams.mergeProgramPath;
				}
				if (path == null || path.length() == 0) {
					externalProgramParams = null;
				}
			}
		}

		protected void detectWithRepositoryResource(IProgressMonitor monitor) {
			//apply only to files
			if (repositoryResource instanceof IRepositoryFile) {
				//at first check extension
				IRepositoryFile file = (IRepositoryFile) repositoryResource;
				ResourceSpecificParameterKind specificKind = DiffViewerSettings.getSpecificResourceKind(diffSettings,
						file, monitor);
				if (specificKind != null) {
					externalProgramParams = diffSettings.getResourceSpecificParameters(specificKind).params;
				}

				//check default external program
				ResourceSpecificParameters defaultResourceSpecificParameters = null;
				if (externalProgramParams == null && (defaultResourceSpecificParameters = diffSettings
						.getDefaultResourceSpecificParameters()) != null) {
					externalProgramParams = defaultResourceSpecificParameters.params;
				}
			}
		}

		protected void detectWithResource(IProgressMonitor monitor) {
			//apply only to files
			if (resource instanceof IFile) {
				//at first check extension
				IFile file = (IFile) resource;
				ResourceSpecificParameterKind specificKind = DiffViewerSettings.getSpecificResourceKind(diffSettings,
						file, monitor);
				if (specificKind != null) {
					externalProgramParams = diffSettings.getResourceSpecificParameters(specificKind).params;
				}

				//check default external program
				ResourceSpecificParameters defaultResourceSpecificParameters = null;
				if (externalProgramParams == null && (defaultResourceSpecificParameters = diffSettings
						.getDefaultResourceSpecificParameters()) != null) {
					externalProgramParams = defaultResourceSpecificParameters.params;
				}
			}
		}

		public ExternalProgramParameters getExternalProgramParameters() {
			return externalProgramParams;
		}
	}

	public static class DetectExternalCompareOperation extends AbstractActionOperation
			implements IExternalProgramParametersProvider {

		protected IResource resource;

		protected IRepositoryResource repositoryResource;

		protected IRepositoryResourceProvider provider;

		protected DiffViewerSettings diffSettings;

		protected DetectExternalCompareOperationHelper helper;

		protected DetectExternalCompareOperation(DiffViewerSettings diffSettings) {
			super("Operation_DetectExternalCompare", SVNMessages.class); //$NON-NLS-1$
			this.diffSettings = diffSettings;
		}

		public DetectExternalCompareOperation(IResource resource, DiffViewerSettings diffSettings) {
			this(diffSettings);
			this.resource = resource;
		}

		public DetectExternalCompareOperation(IRepositoryResource repositoryResource, DiffViewerSettings diffSettings) {
			this(diffSettings);
			this.repositoryResource = repositoryResource;
		}

		public DetectExternalCompareOperation(IRepositoryResourceProvider provider, DiffViewerSettings diffSettings) {
			this(diffSettings);
			this.provider = provider;
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			if (resource != null) {
				helper = new DetectExternalCompareOperationHelper(resource, diffSettings, true);
			} else {
				IRepositoryResource reposResource = provider != null
						? provider.getRepositoryResources()[0]
						: repositoryResource;
				helper = new DetectExternalCompareOperationHelper(reposResource, diffSettings, true);
			}

			helper.execute(monitor);
		}

		@Override
		public ExternalProgramParameters getExternalProgramParameters() {
			return helper.getExternalProgramParameters();
		}
	}

	/**
	 * Run external compare editor
	 *
	 * @author Igor Burilo
	 */
	public static class ExternalCompareOperationHelper {

		protected String baseFile;

		protected String currentFile;

		protected String newFile;

		protected String targetFile;

		protected ExternalProgramParameters externalProgramParams;

		protected boolean isDiff;

		public ExternalCompareOperationHelper(String baseFile, String currentFile, String newFile,
				ExternalProgramParameters externalProgramParams) {
			this(baseFile, currentFile, newFile, currentFile, externalProgramParams, true);
		}

		public ExternalCompareOperationHelper(String baseFile, String currentFile, String newFile,
				ExternalProgramParameters externalProgramParams, boolean isDiff) {
			this(baseFile, currentFile, newFile, currentFile, externalProgramParams, isDiff);
		}

		public ExternalCompareOperationHelper(String baseFile, String currentFile, String newFile, String targetFile,
				ExternalProgramParameters externalProgramParams) {
			this(baseFile, currentFile, newFile, targetFile, externalProgramParams, true);
		}

		public ExternalCompareOperationHelper(String baseFile, String currentFile, String newFile, String targetFile,
				ExternalProgramParameters externalProgramParams, boolean isDiff) {
			this.externalProgramParams = externalProgramParams;
			this.baseFile = baseFile != null ? baseFile : ""; //$NON-NLS-1$
			this.currentFile = currentFile != null ? currentFile : ""; //$NON-NLS-1$
			this.newFile = newFile != null ? newFile : ""; //$NON-NLS-1$
			this.targetFile = targetFile != null ? targetFile : ""; //$NON-NLS-1$
			this.isDiff = isDiff;
		}

		public void execute(IProgressMonitor monitor) throws Exception {
			String programPath = isDiff
					? externalProgramParams.diffProgramPath
					: externalProgramParams.mergeProgramPath;
			String programParameters = isDiff
					? externalProgramParams.diffParamatersString
					: externalProgramParams.mergeParamatersString;

			String processedCmd = programPath;
			String diffParameters = prepareParameters(monitor, programParameters);
			processedCmd += " " + diffParameters; //$NON-NLS-1$
			//System.out.println("Exec: " + processedCmd);

			/*Process process = */Runtime.getRuntime().exec(processedCmd);
			/*
			if (process != null) {
				new ReaderThread(process.getInputStream(), System.out).start();
				new ReaderThread(process.getErrorStream(), System.err).start();
				process.getOutputStream().close();
			}
			
			//waits for external process to terminate
			//Note that we don't wait for Eclipse's compare editor
			process.waitFor();
			
			//update resource in Eclipse
			//local.getResource().refreshLocal(Depth.INFINITY, monitor);
			*/
		}

		protected String prepareParameters(IProgressMonitor monitor, String params) throws IOException {
			String paramaters = params;
			paramaters = paramaters.replace("${base}", baseFile); //$NON-NLS-1$
			paramaters = paramaters.replace("${mine}", currentFile); //$NON-NLS-1$
			paramaters = paramaters.replace("${theirs}", newFile); //$NON-NLS-1$
			paramaters = paramaters.replace("${merged}", targetFile); //$NON-NLS-1$

			if (paramaters.indexOf("${default-doc-program}") != -1) { //$NON-NLS-1$
				paramaters = paramaters.replace("${default-doc-program}", //$NON-NLS-1$
						RunExternalCompareOperation.getScriptFile(RunExternalCompareOperation.DOC_SCRIPT, monitor)
								.getAbsolutePath());
			}
			if (paramaters.indexOf("${default-xls-program}") != -1) { //$NON-NLS-1$
				paramaters = paramaters.replace("${default-xls-program}", //$NON-NLS-1$
						RunExternalCompareOperation.getScriptFile(RunExternalCompareOperation.XLS_SCRIPT, monitor)
								.getAbsolutePath());
			}
			if (paramaters.indexOf("${default-ppt-program}") != -1) { //$NON-NLS-1$
				paramaters = paramaters.replace("${default-ppt-program}", //$NON-NLS-1$
						RunExternalCompareOperation.getScriptFile(RunExternalCompareOperation.PPT_SCRIPT, monitor)
								.getAbsolutePath());
			}

			if (paramaters.indexOf("${default-odt-program}") != -1) { //$NON-NLS-1$
				paramaters = paramaters.replace("${default-odt-program}", //$NON-NLS-1$
						RunExternalCompareOperation.getScriptFile(RunExternalCompareOperation.ODT_SCRIPT, monitor)
								.getAbsolutePath());
			}
			if (paramaters.indexOf("${default-ods-program}") != -1) { //$NON-NLS-1$
				paramaters = paramaters.replace("${default-ods-program}", //$NON-NLS-1$
						RunExternalCompareOperation.getScriptFile(RunExternalCompareOperation.ODS_SCRIPT, monitor)
								.getAbsolutePath());
			}

			return paramaters;
		}
	}

	/**
	 * Prepare files and run external compare editor
	 * 
	 * @author Igor Burilo
	 */
	public static class ExternalCompareOperation extends CompositeOperation {

		protected IExternalProgramParametersProvider parametersProvider;

		protected ExternalProgramParameters externalProgramParams;

		protected boolean isExecuted;

		public ExternalCompareOperation(ILocalResource local, IRepositoryResource remote,
				final IExternalProgramParametersProvider parametersProvider) {
			super("Operation_ExternalCompare", SVNMessages.class); //$NON-NLS-1$
			this.parametersProvider = parametersProvider;

			/* As external program allows only cmd params we should pre-create
			 * files for compare.
			 */

			SVNRevision revision = remote.getSelectedRevision();
			boolean fetchRemote = revision.getKind() == Kind.HEAD || revision.getKind() == Kind.NUMBER;

			//old file
			final AbstractGetFileContentOperation oldFileGetOp = new GetLocalFileContentOperation(local.getResource(),
					Kind.BASE);
			this.add(oldFileGetOp);

			//current
			final AbstractGetFileContentOperation currentFileGetOp = new GetLocalFileContentOperation(
					local.getResource(), Kind.WORKING);
			this.add(currentFileGetOp, new IActionOperation[] { oldFileGetOp });

			//remote: either from repository or BASE
			final AbstractGetFileContentOperation newFileGetOp = fetchRemote
					? new GetFileContentOperation(remote)
					: new GetLocalFileContentOperation(local.getResource(), Kind.BASE);
			this.add(newFileGetOp, new IActionOperation[] { currentFileGetOp });

			//Run external program
			this.add(new AbstractActionOperation("Operation_ExternalCompare", SVNMessages.class) { //$NON-NLS-1$
				@Override
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					ExternalCompareOperationHelper externalRunHelper = new ExternalCompareOperationHelper(
							oldFileGetOp.getTemporaryPath(), currentFileGetOp.getTemporaryPath(),
							newFileGetOp.getTemporaryPath(), externalProgramParams);
					externalRunHelper.execute(monitor);
				}
			}, new IActionOperation[] { oldFileGetOp, currentFileGetOp, newFileGetOp });
		}

		@Override
		protected void runImpl(IProgressMonitor monitor) throws Exception {
			externalProgramParams = parametersProvider.getExternalProgramParameters();
			if (externalProgramParams != null) {
				isExecuted = true;
				super.runImpl(monitor);
			} else {
				isExecuted = false;
			}
		}

		public boolean isExecuted() {
			return isExecuted;
		}
	}

	public RunExternalCompareOperation(ILocalResource local, IRepositoryResource remote,
			DiffViewerSettings diffSettings) {
		super("Operation_ExternalCompare", SVNMessages.class); //$NON-NLS-1$

		DetectExternalCompareOperation detectOperation = new DetectExternalCompareOperation(local.getResource(),
				diffSettings);
		this.add(detectOperation);

		externalCompareOperation = new ExternalCompareOperation(local, remote, detectOperation);
		this.add(externalCompareOperation, new IActionOperation[] { detectOperation });
	}

	public static File getScriptFile(String fileName, IProgressMonitor monitor) throws IOException {
		File stateArea = SVNTeamPlugin.instance().getStateLocation().toFile();
		File scriptFile = new File(stateArea, fileName);
		if (!scriptFile.exists()) {
			URL scriptUrl = FileLocator.find(SVNTeamPlugin.instance().getBundle(), new Path("/resources/" + fileName), //$NON-NLS-1$
					null);

			if (scriptUrl == null) {
				throw new RuntimeException("Failed to locate script file. File name: " + fileName); //$NON-NLS-1$
			}

			InputStream in = null;
			FileOutputStream out = null;
			try {
				in = scriptUrl.openStream();
				out = new FileOutputStream(scriptFile);
				int loaded = 0;
				byte[] buf = new byte[2048];
				while ((loaded = in.read(buf)) > 0 && !monitor.isCanceled()) {
					out.write(buf, 0, loaded);
				}
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ie) {
						//ignore
					}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException ie) {
						//ignore
					}
				}
			}
		}
		return scriptFile;
	}

//	public static class ReaderThread extends Thread {
//
//		private final InputStream myInputStream;
//	    private final OutputStream myOutputStream;
//
//	    public ReaderThread(InputStream is, OutputStream os) {
//	        myInputStream = is;
//	        myOutputStream = os;
//	        setDaemon(true);
//	    }
//
//	    public void run() {
//	        try {
//	            while(true) {
//	                int read = myInputStream.read();
//	                if (read < 0) {
//	                    return;
//	                }
//	                myOutputStream.write(read);
//	            }
//	        } catch (IOException e) {
//	        } finally {
//	            try {
//	            	myInputStream.close();
//	                myOutputStream.flush();
//	            } catch (IOException e) {
//	            	//Just ignore. Stream closing.
//	            }
//	        }
//	    }
//	}

}
