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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.SVNProperty;
import org.eclipse.team.svn.core.operation.local.property.GetPropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.GetRemotePropertiesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Model for DiffViewer
 * 
 * @author Igor Burilo
 */
public class DiffViewerSettings {

	public enum ResourceSpecificParameterKindEnum {
		MIME_TYPE, FILE_EXTENSION, DEFAULT
	}

	public static class ResourceSpecificParameterKind {
		public ResourceSpecificParameterKindEnum kindEnum;

		public String kindValue;

		public ResourceSpecificParameterKind(ResourceSpecificParameterKindEnum kindEnum, String kindValue) {
			this.kindEnum = kindEnum;
			this.kindValue = kindValue;
		}

		@Override
		public boolean equals(Object ob) {
			boolean isEqual = false;
			if (ob != null && ob instanceof ResourceSpecificParameterKind) {
				ResourceSpecificParameterKind kind = (ResourceSpecificParameterKind) ob;
				if (kindEnum == kind.kindEnum && kindValue.equals(kind.kindValue)) {
					isEqual = true;
				}
			}
			return isEqual;
		}

		public String formatKindValue() {
			String res = kindValue;
			if (ResourceSpecificParameterKindEnum.FILE_EXTENSION.equals(kindEnum)) {
				res = "." + kindValue; //$NON-NLS-1$
			}
			return res;
		}

		public static ResourceSpecificParameterKind getKind(String kindString) {
			String kindValue = null;
			ResourceSpecificParameterKindEnum kindEnum = null;
			if (kindString.startsWith(".")) { //$NON-NLS-1$
				kindEnum = ResourceSpecificParameterKindEnum.FILE_EXTENSION;
				kindValue = kindString.substring(1);
			} else if (DiffViewerSettings.DEFAULT_RESOURCE_SPECIFIC_PARAMETER_KIND.kindValue.equals(kindString)) {
				kindEnum = DiffViewerSettings.DEFAULT_RESOURCE_SPECIFIC_PARAMETER_KIND.kindEnum;
				kindValue = DiffViewerSettings.DEFAULT_RESOURCE_SPECIFIC_PARAMETER_KIND.kindValue;
			} else {
				kindEnum = ResourceSpecificParameterKindEnum.MIME_TYPE;
				kindValue = kindString;
			}
			ResourceSpecificParameterKind kind = new ResourceSpecificParameterKind(kindEnum, kindValue);
			return kind;
		}

		@Override
		public int hashCode() {
			return kindEnum.hashCode() + kindValue.hashCode();
		}
	}

	public static class ResourceSpecificParameters {

		public final static int FIELDS_COUNT = 7;

		public ResourceSpecificParameterKind kind;

		public ExternalProgramParameters params;

		public boolean isEnabled;

		public ResourceSpecificParameters(ResourceSpecificParameterKindEnum kindEnum, String kindValue,
				String diffProgramPath, String mergeProgramPath, String diffProgramParams, String mergeProgramParams) {
			this(new ResourceSpecificParameterKind(kindEnum, kindValue), new ExternalProgramParameters(diffProgramPath,
					mergeProgramPath, diffProgramParams, mergeProgramParams));
		}

		public ResourceSpecificParameters(ResourceSpecificParameterKind kind, ExternalProgramParameters params) {
			this(kind, params, true);
		}

		public ResourceSpecificParameters(ResourceSpecificParameterKind kind, ExternalProgramParameters params,
				boolean isEnabled) {
			this.kind = kind;
			this.params = params;
			this.isEnabled = isEnabled;
		}

		public String[] getAsStrings() {
			String[] res = new String[ResourceSpecificParameters.FIELDS_COUNT];

			res[0] = isEnabled ? "1" : "0"; //$NON-NLS-1$ //$NON-NLS-2$
			res[1] = kind.kindEnum.name();
			res[2] = kind.kindValue == null ? "" : kind.kindValue; //$NON-NLS-1$
			res[3] = params.diffProgramPath == null ? "" : params.diffProgramPath; //$NON-NLS-1$
			res[4] = params.mergeProgramPath == null ? "" : params.mergeProgramPath; //$NON-NLS-1$
			res[5] = params.diffParamatersString == null ? "" : params.diffParamatersString; //$NON-NLS-1$
			res[6] = params.mergeParamatersString == null ? "" : params.mergeParamatersString; //$NON-NLS-1$

			return res;
		}

		public static ResourceSpecificParameters createFromStrings(String[] strings) {
			if (strings.length != FIELDS_COUNT) {
				return null;
			}

			boolean isEnabled = "1".equals(strings[0]); //$NON-NLS-1$

			ResourceSpecificParameterKindEnum kindEnum = null;
			ResourceSpecificParameterKindEnum[] kindEnums = ResourceSpecificParameterKindEnum.values();
			for (ResourceSpecificParameterKindEnum ke : kindEnums) {
				if (ke.name().equals(strings[1])) {
					kindEnum = ke;
					break;
				}
			}

			ResourceSpecificParameters res = new ResourceSpecificParameters(kindEnum, strings[2], strings[3],
					strings[4], strings[5], strings[6]);
			res.isEnabled = isEnabled;

			return res;
		}
	}

	public static class ExternalProgramParameters {
		public String diffProgramPath;

		public String diffParamatersString;

		public String mergeProgramPath;

		public String mergeParamatersString;

		public ExternalProgramParameters(String diffProgramPath, String mergeProgramPath, String diffParamatersString,
				String mergeParamatersString) {
			this.diffProgramPath = diffProgramPath;
			this.mergeProgramPath = mergeProgramPath;
			this.diffParamatersString = diffParamatersString;
			this.mergeParamatersString = mergeParamatersString;
		}
	}

	public static ResourceSpecificParameterKind DEFAULT_RESOURCE_SPECIFIC_PARAMETER_KIND = new ResourceSpecificParameterKind(
			ResourceSpecificParameterKindEnum.DEFAULT, "*"); //$NON-NLS-1$

	protected Map<ResourceSpecificParameterKind, ResourceSpecificParameters> specificParameters = new HashMap<>();

	public static ResourceSpecificParameterKind getSpecificResourceKind(DiffViewerSettings diffSettings, IFile file,
			IProgressMonitor monitor) {
		ResourceSpecificParameterKind kind = null;

		//check file extension
		String fileExtension = file.getFileExtension();
		ResourceSpecificParameterKind tmpKind;
		if (fileExtension != null && diffSettings.specificParameters.containsKey(
				tmpKind = new ResourceSpecificParameterKind(ResourceSpecificParameterKindEnum.FILE_EXTENSION,
						fileExtension))) {
			kind = tmpKind;
		}

		//check mime type
		if (kind == null) {
			String mimeType = null;

			if (IStateFilter.SF_VERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(file))) {
				GetPropertiesOperation op = new GetPropertiesOperation(file);
				ProgressMonitorUtility.doTaskExternalDefault(op, monitor);
				if (op.getExecutionState() == IStatus.OK) {
					SVNProperty[] props = op.getProperties();
					if (props != null) {
						for (SVNProperty prop : props) {
							if (SVNProperty.BuiltIn.MIME_TYPE.equals(prop.name)) {
								mimeType = prop.value;
								break;
							}
						}
					}
				}
			}

			if (mimeType != null && diffSettings.specificParameters.containsKey(
					tmpKind = new ResourceSpecificParameterKind(ResourceSpecificParameterKindEnum.MIME_TYPE,
							mimeType))) {
				kind = tmpKind;
			}
		}

		return kind;
	}

	public static ResourceSpecificParameterKind getSpecificResourceKind(DiffViewerSettings diffSettings,
			IRepositoryFile file, IProgressMonitor monitor) {
		ResourceSpecificParameterKind kind = null;

		//check file extension
		String fileExtension = null;
		String fileName = file.getName();
		int index = fileName.lastIndexOf("."); //$NON-NLS-1$
		if (index != -1 && index != fileName.length()) {
			fileExtension = fileName.substring(index + 1);
			ResourceSpecificParameterKind tmpKind;
			if (diffSettings.specificParameters.containsKey(tmpKind = new ResourceSpecificParameterKind(
					ResourceSpecificParameterKindEnum.FILE_EXTENSION, fileExtension))) {
				kind = tmpKind;
			}
		}

		//check mime type
		if (kind == null) {
			String mimeType = null;

			GetRemotePropertiesOperation op = new GetRemotePropertiesOperation(file);
			ProgressMonitorUtility.doTaskExternalDefault(op, monitor);
			if (op.getExecutionState() == IStatus.OK) {
				SVNProperty[] props = op.getProperties();
				if (props != null) {
					for (SVNProperty prop : props) {
						if (SVNProperty.BuiltIn.MIME_TYPE.equals(prop.name)) {
							mimeType = prop.value;
							break;
						}
					}
				}
			}

			ResourceSpecificParameterKind tmpKind;
			if (mimeType != null && diffSettings.specificParameters.containsKey(
					tmpKind = new ResourceSpecificParameterKind(ResourceSpecificParameterKindEnum.MIME_TYPE,
							mimeType))) {
				kind = tmpKind;
			}
		}

		return kind;
	}

	public boolean addResourceSpecificParameters(ResourceSpecificParameterKindEnum kindEnum, String kindValue,
			ExternalProgramParameters params) {
		return this.addResourceSpecificParameters(kindEnum, kindValue, params, true);
	}

	public boolean addResourceSpecificParameters(ResourceSpecificParameterKindEnum kindEnum, String kindValue,
			ExternalProgramParameters params, boolean isEnabled) {
		ResourceSpecificParameterKind kind = new ResourceSpecificParameterKind(kindEnum, kindValue);
		ResourceSpecificParameters resourceParams = new ResourceSpecificParameters(kind, params, isEnabled);
		return this.addResourceSpecificParameters(resourceParams);
	}

	public boolean addResourceSpecificParameters(ResourceSpecificParameters resourceParams) {
		boolean isAdded = false;
		if (!specificParameters.containsKey(resourceParams.kind)) {
			specificParameters.put(resourceParams.kind, resourceParams);
			isAdded = true;

			//fire event
			if (!listenersList.isEmpty()) {
				Object[] listeners = listenersList.getListeners();
				for (Object listener2 : listeners) {
					IDiffViewerChangeListener listener = (IDiffViewerChangeListener) listener2;
					listener.addResourceSpecificParameters(resourceParams);
				}
			}
		}
		return isAdded;
	}

	public void removeResourceSpecificParameters(ResourceSpecificParameters resourceParams) {
		specificParameters.remove(resourceParams.kind);

		//fire event
		if (!listenersList.isEmpty()) {
			Object[] listeners = listenersList.getListeners();
			for (Object listener2 : listeners) {
				IDiffViewerChangeListener listener = (IDiffViewerChangeListener) listener2;
				listener.removeResourceSpecificParameters(resourceParams);
			}
		}
	}

	public void updateResourceSpecificParameters(ResourceSpecificParameters resourceParams) {
		//fire event
		if (!listenersList.isEmpty()) {
			Object[] listeners = listenersList.getListeners();
			for (Object listener2 : listeners) {
				IDiffViewerChangeListener listener = (IDiffViewerChangeListener) listener2;
				listener.changeResourceSpecificParameters(resourceParams);
			}
		}
	}

	public ResourceSpecificParameters getResourceSpecificParameters(ResourceSpecificParameterKind kind) {
		return specificParameters.get(kind);
	}

	public ResourceSpecificParameters[] getResourceSpecificParameters() {
		return specificParameters.values().toArray(new ResourceSpecificParameters[0]);
	}

	public ResourceSpecificParameters getDefaultResourceSpecificParameters() {
		return specificParameters.get(DEFAULT_RESOURCE_SPECIFIC_PARAMETER_KIND);
	}

	public static DiffViewerSettings getDefaultDiffViewerSettings() {
		DiffViewerSettings diffSettings = new DiffViewerSettings();
		if (FileUtility.isWindows()) {
			//doc
			String diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			String diffParametersString = "\"${default-doc-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			String mergeProgramPath = "wscript.exe"; //$NON-NLS-1$
			String mergeParametersString = "\"${default-doc-program}\" \"${theirs}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "doc", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));
			//docx
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-doc-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = "wscript.exe"; //$NON-NLS-1$
			mergeParametersString = "\"${default-doc-program}\" \"${theirs}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "docx", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));

			//xls
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-xls-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = null;
			mergeParametersString = null;
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "xls", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));
			//xlsx
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-xls-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = null;
			mergeParametersString = null;
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "xlsx", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));

			//ppt
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-ppt-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = null;
			mergeParametersString = null;
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "ppt", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));
			//pptx
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-ppt-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = null;
			mergeParametersString = null;
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "pptx", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));

			//odt
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-odt-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = "wscript.exe"; //$NON-NLS-1$
			mergeParametersString = "\"${default-odt-program}\" \"${theirs}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "odt", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));
			//ods
			diffProgramPath = "wscript.exe"; //$NON-NLS-1$
			diffParametersString = "\"${default-ods-program}\" \"${base}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			mergeProgramPath = "wscript.exe"; //$NON-NLS-1$
			mergeParametersString = "\"${default-ods-program}\" \"${theirs}\" \"${mine}\" //E:vbscript"; //$NON-NLS-1$
			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "ods", //$NON-NLS-1$
					new ExternalProgramParameters(diffProgramPath, mergeProgramPath, diffParametersString,
							mergeParametersString));

//			//java
//			String programPath = "C:/Program Files/TortoiseSVN/bin/TortoiseMerge.exe";
//			//String parametersString = "/theirs:\"${theirs}\" /base:\"${base}\" /mine:\"${mine}\" /merged:\"${merged}\"";
//			String parametersString = "/base:\"${base}\" /mine:\"${mine}\" /merged:\"${merged}\"";
//			diffSettings.addResourceSpecificParameters(ResourceSpecificParameterKindEnum.FILE_EXTENSION, "java", new ExternalProgramParameters(programPath, parametersString));
		}
		return diffSettings;
	}

	//---- listeners

	public interface IDiffViewerChangeListener {
		void addResourceSpecificParameters(ResourceSpecificParameters pameters);

		void removeResourceSpecificParameters(ResourceSpecificParameters pameters);

		void changeResourceSpecificParameters(ResourceSpecificParameters pameters);
	}

	protected ListenerList listenersList = new ListenerList();

	public void addChangeListener(IDiffViewerChangeListener listener) {
		listenersList.add(listener);
	}

	public void removeChangeListener(IDiffViewerChangeListener listener) {
		listenersList.remove(listener);
	}
}
