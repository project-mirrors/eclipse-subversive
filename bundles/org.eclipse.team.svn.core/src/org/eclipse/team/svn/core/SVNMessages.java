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

package org.eclipse.team.svn.core;

import org.eclipse.osgi.util.NLS;

public class SVNMessages extends BaseMessages {

	protected static final String BUNDLE_NAME = "org.eclipse.team.svn.core.messages"; //$NON-NLS-1$

	public static String Console_Action_Added;

	public static String Console_Action_Deleted;

	public static String Console_Action_Locked;

	public static String Console_Action_Modified;

	public static String Console_Action_Replaced;

	public static String Console_Action_Restored;

	public static String Console_Action_Reverted;

	public static String Console_Action_Unlocked;

	public static String Console_AtRevision;

	public static String Console_CommittedRevision;

	public static String Console_Status;

	public static String Console_Status_Added;

	public static String Console_Status_Conflicted;

	public static String Console_Status_Deleted;

	public static String Console_Status_Merged;

	public static String Console_Status_Missing;

	public static String Console_Status_Modified;

	public static String Console_Status_New;

	public static String Console_Status_Obstructed;

	public static String Console_Status_Replaced;

	public static String Console_Status_TreeConflicting;

	public static String Console_TransmittingData;

	public static String Console_Update_Status_changed;

	public static String Console_Update_Status_conflicted;

	public static String Console_Update_Status_conflicted_unresolved;

	public static String Console_Update_Status_inapplicable;

	public static String Console_Update_Status_merged;

	public static String Console_Update_Status_missing;

	public static String Console_Update_Status_obstructed;

	public static String Console_Update_Status_unchanged;

	public static String Console_Update_Status_unknown;

	public static String Console_UpdateExternal;

	public static String Error_AlreadyExists;

	public static String Error_AlreadyExists_Id;

	public static String Error_AnotherProvider;

	public static String Error_AnotherProvider_Id;

	public static String Error_AuthenticationCancelled;

	public static String Error_AuthenticationCancelled_Id;

	public static String Error_AutoDisconnect;

	public static String Error_AutoDisconnect_Id;

	public static String Error_CannotCheckOutMeta;

	public static String Error_CannotCheckOutMeta_Id;

	public static String Error_CheckCache;

	public static String Error_CheckCache_Id;

	public static String Error_CreateDirectory;

	public static String Error_CreateDirectory_Id;

	public static String Error_InaccessibleResource;

	public static String Error_InaccessibleResource_Id;

	public static String Error_InaccessibleResource_2;

	public static String Error_InaccessibleResource_Id_2;

	public static String Error_InvalidExtensionPoint;

	public static String Error_InvalidExtensionPoint_Id;

	public static String Error_LoadExtensions;

	public static String Error_LoadLocations;

	public static String Error_LoadLocationsFromFile;

	public static String Error_LockedExternally;

	public static String Error_LockedExternally_Id;

	public static String Error_NoSVNClient;

	public static String Error_NonSVNPath;

	public static String Error_NonSVNPath_Id;

	public static String Error_NotConnectedProject;

	public static String Error_NotConnectedProject_Id;

	public static String Error_NotRelatedURL;

	public static String Error_NotRelatedURL_Id;

	public static String Error_NullURL;

	public static String Error_NullURL_Id;

	public static String Error_RepositoryInaccessible;

	public static String Error_RepositoryInaccessible_Id;

	public static String Error_ScheduledTask;

	public static String Error_ShareCanceled;

	public static String Error_ShareCanceled_Id;

	public static String Error_ShorterURL;

	public static String Error_ShorterURL_Id;

	public static String Error_UnknownProjectLayoutType;

	public static String Error_UnknownProjectLayoutType_Id;

	public static String Error_UnknownProtocol;

	public static String Error_UnknownProtocol_Id;

	public static String Error_UnknownStatus;

	public static String Error_UnknownStatus_Id;

	public static String Error_UnrecognizedNodeKind;

	public static String Error_UnrecognizedNodeKind_Id;

	public static String Error_SaveMigratePreference;

	public static String Error_SaveAutherizationInfo;

	public static String Error_LoadAuthorizationInfo;

	public static String Error_RemoveAuthorizationInfo;

	public static String Error_NoParent;

	public static String MergeScope_Name;

	public static String Operation_AddRepositoryLocation;

	public static String Operation_AddRepositoryLocation_Error;

	public static String Operation_AddRepositoryLocation_Id;

	public static String Operation_AddRevisionLink;

	public static String Operation_AddRevisionLink_Id;

	public static String Operation_AddToSVN;

	public static String Operation_AddToSVNFile;

	public static String Operation_AddToSVNFile_Error;

	public static String Operation_AddToSVNFile_Id;

	public static String Operation_AddToSVNIgnore;

	public static String Operation_AddToSVNIgnoreFile;

	public static String Operation_AddToSVNIgnoreFile_Error;

	public static String Operation_AddToSVNIgnoreFile_Id;

	public static String Operation_AddToSVNIgnore_Error;

	public static String Operation_AddToSVNIgnore_Id;

	public static String Operation_AddToSVN_Error;

	public static String Operation_AddToSVN_Id;

	public static String Operation_Branch;

	public static String Operation_Branch_Error;

	public static String Operation_Branch_Id;

	public static String Operation_BreakLock;

	public static String Operation_BreakLock_Error;

	public static String Operation_BreakLock_Id;

	public static String Operation_CheckOut;

	public static String Operation_CheckOutAs;

	public static String Operation_CheckOutAs_Error;

	public static String Operation_CheckOutAs_Id;

	public static String Operation_CheckOutAs_PrepareFS;

	public static String Operation_CheckOut_Error;

	public static String Operation_CheckOut_Id;

	public static String Operation_CheckProperty;

	public static String Operation_CheckProperty_Id;

	public static String Operation_CheckoutAsFile;

	public static String Operation_CheckoutAsFile_Error;

	public static String Operation_CheckoutAsFile_Id;

	public static String Operation_CleanupFile;

	public static String Operation_CleanupFile_Error;

	public static String Operation_CleanupFile_Id;

	public static String Operation_CleanupResources;

	public static String Operation_CleanupResources_Error;

	public static String Operation_CleanupResources_Id;

	public static String Operation_ClearLocalStatuses;

	public static String Operation_ClearLocalStatuses_Error;

	public static String Operation_ClearLocalStatuses_Id;

	public static String Operation_Commit;

	public static String Operation_CommitFile;

	public static String Operation_CommitFile_Error;

	public static String Operation_CommitFile_Id;

	public static String Operation_Commit_Error;

	public static String Operation_Commit_Id;

	public static String Operation_CopyFile;

	public static String Operation_CopyFile_Error;

	public static String Operation_CopyFile_Id;

	public static String Operation_CopyLocal;

	public static String Operation_CopyLocalH;

	public static String Operation_CopyLocalH_Error;

	public static String Operation_CopyLocalH_Id;

	public static String Operation_CopyLocal_Error;

	public static String Operation_CopyLocal_Id;

	public static String Operation_CopyRemote;

	public static String Operation_CopyRemote_Error;

	public static String Operation_CopyRemote_Id;

	public static String Operation_CreateFile;

	public static String Operation_CreateFile_Error;

	public static String Operation_CreateFile_Id;

	public static String Operation_CreateFolder;

	public static String Operation_CreateFolder_Error;

	public static String Operation_CreateFolder_Id;

	public static String Operation_CreatePatchFile;

	public static String Operation_CreatePatchFile_Error;

	public static String Operation_CreatePatchFile_Id;

	public static String Operation_CreatePatchLocal;

	public static String Operation_CreatePatchLocal_Error;

	public static String Operation_CreatePatchLocal_Id;

	public static String Operation_CreatePatchRemote;

	public static String Operation_CreatePatchRemote_Id;

	public static String Operation_DeleteFile;

	public static String Operation_DeleteFile_Error;

	public static String Operation_DeleteFile_Id;

	public static String Operation_DeleteLocal;

	public static String Operation_DeleteLocal_Error;

	public static String Operation_DeleteLocal_Id;

	public static String Operation_DeleteRemote;

	public static String Operation_DeleteRemote_Error;

	public static String Operation_DeleteRemote_Id;

	public static String Operation_DetectCharset;

	public static String Operation_DetectCharset_Id;

	public static String Operation_DiscardRepositoryLocation;

	public static String Operation_DiscardRepositoryLocation_Error;

	public static String Operation_DiscardRepositoryLocation_Id;

	public static String Operation_Disconnect;

	public static String Operation_DisconnectFile;

	public static String Operation_DisconnectFile_Error;

	public static String Operation_DisconnectFile_Id;

	public static String Operation_Disconnect_Error;

	public static String Operation_Disconnect_Id;

	public static String Operation_Error_LogHeader;

	public static String Operation_ExportProjectSet;

	public static String Operation_ExportProjectSet_Id;

	public static String Operation_ExportRevision;

	public static String Operation_ExportRevision_Error;

	public static String Operation_ExportRevision_Id;

	public static String Operation_ExtractTo;

	public static String Operation_ExtractTo_Error;

	public static String Operation_ExtractTo_Folders;

	public static String Operation_ExtractTo_Id;

	public static String Operation_ExtractTo_LocalFile;

	public static String Operation_ExtractTo_RemoteFile;

	public static String Operation_FetchRepositoryRoot;

	public static String Operation_FetchRepositoryRoot_Id;

	public static String Operation_FindRelatedProjects;

	public static String Operation_FindRelatedProjects_Id;

	public static String Operation_FiniExtractLog;

	public static String Operation_FiniExtractLog_Id;

	public static String Operation_FreezeExternals;

	public static String Operation_FreezeExternals_Id;

	public static String Operation_GetAllFiles;

	public static String Operation_GetAllFiles_Error;

	public static String Operation_GetAllFiles_Id;

	public static String Operation_GetAnnotation;

	public static String Operation_GetAnnotation_Error;

	public static String Operation_GetAnnotation_Error_IsBinary;

	public static String Operation_GetAnnotation_Id;

	public static String Operation_GetContent;

	public static String Operation_GetContent_Error;

	public static String Operation_GetContent_Id;

	public static String Operation_GetFileContent;

	public static String Operation_GetFileContent_CreateStream;

	public static String Operation_GetFileContent_CreateStream_Id;

	public static String Operation_GetFileContent_Error;

	public static String Operation_GetFileContent_Id;

	public static String Operation_GetFileContent_Local;

	public static String Operation_GetFileContent_Local_Error;

	public static String Operation_GetFileContent_Local_Id;

	public static String Operation_GetFileContent_Revision;

	public static String Operation_GetFileContent_Revision_Error;

	public static String Operation_GetFileContent_Revision_Id;

	public static String Operation_GetFileContent_SetContent;

	public static String Operation_GetFileContent_SetContent_Id;

	public static String Operation_GetLogMessages;

	public static String Operation_GetLogMessages_Error;

	public static String Operation_GetLogMessages_Id;

	public static String Operation_GetMultiProperties;

	public static String Operation_GetMultiProperties_Error;

	public static String Operation_GetMultiProperties_Id;

	public static String Operation_GetProperties;

	public static String Operation_GetPropertiesFile;

	public static String Operation_GetPropertiesFile_Error;

	public static String Operation_GetPropertiesFile_Id;

	public static String Operation_GetProperties_Error;

	public static String Operation_GetProperties_Id;

	public static String Operation_GetResourceList;

	public static String Operation_GetResourceList_Error;

	public static String Operation_GetResourceList_Id;

	public static String Operation_GetRevisionProperties;

	public static String Operation_GetRevisionProperties_Error;

	public static String Operation_GetRevisionProperties_Id;

	public static String Operation_Import;

	public static String Operation_ImportProjectSet;

	public static String Operation_ImportProjectSet_Id;

	public static String Operation_Import_Error;

	public static String Operation_Import_Id;

	public static String Operation_Info;

	public static String Operation_Info_Error;

	public static String Operation_Info_Id;

	public static String Operation_InitExtractLog;

	public static String Operation_InitExtractLog_Id;

	public static String Operation_JavaHLMerge;

	public static String Operation_JavaHLMergeFile;

	public static String Operation_JavaHLMergeFile_Error;

	public static String Operation_JavaHLMergeFile_Id;

	public static String Operation_JavaHLMerge_Error;

	public static String Operation_JavaHLMerge_Id;

	public static String Operation_LocalStatusFile;

	public static String Operation_LocalStatusFile_Error;

	public static String Operation_LocalStatusFile_Id;

	public static String Operation_LocateProjects;

	public static String Operation_LocateProjects_Id;

	public static String Operation_LocateProjects_Scanning;

	public static String Operation_LocateURLInHistory;

	public static String Operation_LocateURLInHistory_Id;

	public static String Operation_Lock;

	public static String Operation_LockFile;

	public static String Operation_LockFile_Error;

	public static String Operation_LockFile_Id;

	public static String Operation_Lock_Error;

	public static String Operation_Lock_Id;

	public static String Operation_MarkAsMerged;

	public static String Operation_MarkAsMerged_Error;

	public static String Operation_MarkAsMerged_Id;

	public static String Operation_MarkResolved;

	public static String Operation_MarkResolvedFile;

	public static String Operation_MarkResolvedFile_Error;

	public static String Operation_MarkResolvedFile_Id;

	public static String Operation_MarkResolved_Error;

	public static String Operation_MarkResolved_Id;

	public static String Operation_Merge;

	public static String Operation_MergeStatus;

	public static String Operation_MergeStatus_Error;

	public static String Operation_MergeStatus_Id;

	public static String Operation_Merge_Error;

	public static String Operation_Merge_Id;

	public static String Operation_MoveFile;

	public static String Operation_MoveFile_Error;

	public static String Operation_MoveFile_Id;

	public static String Operation_MoveLocal;

	public static String Operation_MoveLocal_Error;

	public static String Operation_MoveLocal_Id;

	public static String Operation_MoveRemote;

	public static String Operation_MoveRemote_Error;

	public static String Operation_MoveRemote_Id;

	public static String Operation_NotifyProjectChange;

	public static String Operation_NotifyProjectChange_Id;

	public static String Operation_OpenProject;

	public static String Operation_OpenProject_Id;

	public static String Operation_PreparedBranch;

	public static String Operation_PreparedBranch_Error;

	public static String Operation_PreparedBranch_Id;

	public static String Operation_PreparedTag;

	public static String Operation_PreparedTag_Error;

	public static String Operation_PreparedTag_Id;

	public static String Operation_Reconnect;

	public static String Operation_Reconnect_Error;

	public static String Operation_Reconnect_Id;

	public static String Operation_RefreshResources;

	public static String Operation_RefreshResources_Error;

	public static String Operation_RefreshResources_Id;

	public static String Operation_RefreshResources_DamagedProjectFile;

	public static String Operation_RelocateFile;

	public static String Operation_RelocateFile_Error;

	public static String Operation_RelocateFile_Id;

	public static String Operation_RelocateResources;

	public static String Operation_RelocateResources_Error;

	public static String Operation_RelocateResources_Id;

	public static String Operation_RemoveNonSVN;

	public static String Operation_RemoveNonSVN_Error;

	public static String Operation_RemoveNonSVN_Id;

	public static String Operation_RemoveProperties;

	public static String Operation_RemovePropertiesFile;

	public static String Operation_RemovePropertiesFile_Error;

	public static String Operation_RemovePropertiesFile_Id;

	public static String Operation_RemoveProperties_Error;

	public static String Operation_RemoveProperties_Id;

	public static String Operation_Rename;

	public static String Operation_Rename_Error;

	public static String Operation_Rename_Id;

	public static String Operation_ReplaceWithRemote;

	public static String Operation_ResourcesChanged;

	public static String Operation_ResourcesChanged_Id;

	public static String Operation_RestoreExternals;

	public static String Operation_RestoreExternals_Id;

	public static String Operation_RestoreMeta;

	public static String Operation_RestoreMeta_Id;

	public static String Operation_Revert;

	public static String Operation_RevertFile;

	public static String Operation_RevertFile_Error;

	public static String Operation_RevertFile_Id;

	public static String Operation_Revert_Error;

	public static String Operation_Revert_Id;

	public static String Operation_SaveMeta;

	public static String Operation_SaveMeta_Id;

	public static String Operation_SaveRepositoryLocations;

	public static String Operation_SaveRepositoryLocations_Error;

	public static String Operation_SaveRepositoryLocations_Id;

	public static String Operation_SendNotifications;

	public static String Operation_SendNotifications_Id;

	public static String Operation_SetMultiProperties;

	public static String Operation_SetMultiProperties_Error;

	public static String Operation_SetMultiProperties_Id;

	public static String Operation_SetProperties;

	public static String Operation_SetPropertiesFile;

	public static String Operation_SetPropertiesFile_Error;

	public static String Operation_SetPropertiesFile_Id;

	public static String Operation_SetProperties_Error;

	public static String Operation_SetProperties_Id;

	public static String Operation_SetRevisionAuthorName;

	public static String Operation_SetRevisionProperty;

	public static String Operation_ShareFile;

	public static String Operation_ShareFile_DefaultComment;

	public static String Operation_ShareFile_Error;

	public static String Operation_ShareFile_Id;

	public static String Operation_ShareProject;

	public static String Operation_ShareProject_DefaultComment;

	public static String Operation_ShareProject_Error;

	public static String Operation_ShareProject_Id;

	public static String Operation_Switch;

	public static String Operation_SwitchFile;

	public static String Operation_SwitchFile_Error;

	public static String Operation_SwitchFile_Id;

	public static String Operation_Switch_Error;

	public static String Operation_Switch_Id;

	public static String Operation_Tag;

	public static String Operation_Tag_Error;

	public static String Operation_Tag_Id;

	public static String Operation_Unlock;

	public static String Operation_UnlockFile;

	public static String Operation_UnlockFile_Error;

	public static String Operation_UnlockFile_Id;

	public static String Operation_Unlock_Error;

	public static String Operation_Unlock_Id;

	public static String Operation_Update;

	public static String Operation_UpdateFile;

	public static String Operation_UpdateFile_Error;

	public static String Operation_UpdateFile_Id;

	public static String Operation_UpdateSVNCache;

	public static String Operation_UpdateSVNCache_Id;

	public static String Operation_UpdateStatus;

	public static String Operation_UpdateStatusFile;

	public static String Operation_UpdateStatusFile_Error;

	public static String Operation_UpdateStatusFile_Id;

	public static String Operation_UpdateStatus_Error;

	public static String Operation_UpdateStatus_Id;

	public static String Operation_Update_Error;

	public static String Operation_Update_Id;

	public static String Operation_DetectExternalCompare;

	public static String Operation_DetectExternalCompare_Id;

	public static String Operation_DetectExternalCompare_Error;

	public static String Operation_ExternalCompare;

	public static String Operation_ExternalCompare_Id;

	public static String Operation_ExternalCompare_Error;

	public static String Operation_UDiffGenerate;

	public static String Operation_UDiffGenerate_Id;

	public static String Operation_UDiffGenerate_Error;

	public static String Operation_ExternalRepositoryCompare;

	public static String Operation_ExternalRepositoryCompare_Id;

	public static String Operation_ExternalRepositoryCompare_Error;

	public static String Operation_CopyRemoteToWC;

	public static String Operation_CopyRemoteToWC_Id;

	public static String Operation_FileReplaceListener;

	public static String Operation_FileReplaceListener_Id;

	public static String Operation_FileReplaceListener_Error;

	public static String Operation_GenerateExternalsProperty;

	public static String Operation_GenerateExternalsProperty_Id;

	public static String Operation_GenerateExternalsProperty_Error;

	public static String Operation_GetRemoteChildren;

	public static String Operation_GetRemoteChildren_Error;

	public static String Operation_GetRemoteChildren_Id;

	public static String Operation_CopyResourceFromHook;

	public static String Operation_CopyResourceFromHook_Id;

	public static String Operation_CopyResourceFromHook_Error;

	public static String Operation_TrackMoveResult;

	public static String Operation_TrackMoveResult_Id;

	public static String Operation_TrackDeleteResult;

	public static String Operation_TrackDeleteResult_Id;

	public static String Operation_Upgrade;

	public static String Operation_Upgrade_Id;

	public static String Progress_Done;

	public static String Progress_Running;

	public static String Progress_SubTask;

	public static String SVNInfo_Author;

	public static String SVNInfo_Date;

	public static String SVNInfo_NoAuthor;

	public static String SVNInfo_NoComment;

	public static String SVNInfo_NoDate;

	public static String Status_Added;

	public static String Status_Conflicting;

	public static String Status_Deleted;

	public static String Status_Linked;

	public static String Status_Missing;

	public static String Status_Modified;

	public static String Status_New;

	public static String Status_None;

	public static String Status_Ignored;

	public static String Status_Normal;

	public static String Status_NotExists;

	public static String Status_Obstructed;

	public static String Status_Prereplaced;

	public static String Status_Replaced;

	public static String TreeConflicting;

	public static String BundleDiscoveryStrategy_categoryDisallowed;

	public static String BundleDiscoveryStrategy_task_loading_local_extensions;

	public static String BundleDiscoveryStrategy_task_processing_extensions;

	public static String BundleDiscoveryStrategy_unexpected_element;

	public static String ConnectorCategory_connectorCategory_relevance_invalid;

	public static String ConnectorCategory_must_specify_connectorCategory_id;

	public static String ConnectorCategory_must_specify_connectorCategory_name;

	public static String ConnectorDescriptor_invalid_connectorDescriptor_siteUrl;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_categoryId;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_id;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_kind;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_license;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_name;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_provider;

	public static String ConnectorDescriptor_must_specify_connectorDescriptor_siteUrl;

	public static String ConnectorDiscovery_bundle_references_unknown_category;

	public static String ConnectorDiscovery_duplicate_category_id;

	public static String ConnectorDiscovery_exception_disposing;

	public static String ConnectorDiscovery_illegal_filter_syntax;

	public static String ConnectorDiscovery_task_discovering_connectors;

	public static String ConnectorDiscovery_task_verifyingAvailability;

	public static String ConnectorDiscovery_unexpected_exception;

	public static String ConnectorDiscoveryExtensionReader_unexpected_element_icon;

	public static String ConnectorDiscoveryExtensionReader_unexpected_element_overview;

	public static String ConnectorDiscoveryExtensionReader_unexpected_value_kind;

	public static String DirectoryParser_no_directory;

	public static String DirectoryParser_unexpected_element;

	public static String DiscoveryRegistryStrategy_cannot_load_bundle;

	public static String DiscoveryRegistryStrategy_missing_pluginxml;

	public static String FeatureFilter_must_specify_featureFilter_featureId;

	public static String FeatureFilter_must_specify_featureFilter_version;

	public static String Group_must_specify_group_id;

	public static String RemoteBundleDiscoveryStrategy_cannot_download_bundle;

	public static String RemoteBundleDiscoveryStrategy_empty_directory;

	public static String RemoteBundleDiscoveryStrategy_io_failure_discovery_directory;

	public static String RemoteBundleDiscoveryStrategy_io_failure_temp_storage;

	public static String RemoteBundleDiscoveryStrategy_task_remote_discovery;

	public static String RemoteBundleDiscoveryStrategy_unexpectedError;

	public static String RemoteBundleDiscoveryStrategy_unknown_host_discovery_directory;

	public static String RemoteBundleDiscoveryStrategy_unrecognized_discovery_url;

	public static String WebUtil_cannotDownload;

	public static String WebUtil_task_retrievingUrl;

	public static String WebUtil_task_verifyingUrl;

	public static String MergeSubscriber_Name;

	public static String UpdateSubscriber_Name;

	public static String CommitOperation_3;

	public static String ResourceVariant_unversioned;

	public static String ResourceVariant_deleted;

	static {
		//load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, SVNMessages.class);
	}

	public static String getString(String key) {
		return BaseMessages.getString(key, SVNMessages.class);
	}

	public static String getErrorString(String key) {
		return BaseMessages.getErrorString(key, SVNMessages.class);
	}

	public static String formatErrorString(String key, Object[] args) {
		return BaseMessages.formatErrorString(key, args, SVNMessages.class);
	}
}
