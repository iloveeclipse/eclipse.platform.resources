/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.utils.Assert;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class LocalMetaArea implements ICoreConstants {
	protected IPath metaAreaLocation;

	/* package */ static final String F_BACKUP_FILE_EXTENSION = ".bak"; //$NON-NLS-1$
	/* package */ static final String F_DESCRIPTION = ".workspace"; //$NON-NLS-1$
	/* package */ static final String F_HISTORY_STORE = ".history"; //$NON-NLS-1$
	/* package */ static final String F_MARKERS = ".markers"; //$NON-NLS-1$
	/* package */ static final String F_PROJECT = ".prj"; //$NON-NLS-1$
	/* package */ static final String F_OLD_PROJECT_LOCATION = ".location"; //$NON-NLS-1$
	/* package */ static final String F_PROJECTS = ".projects"; //$NON-NLS-1$
	/* package */ static final String F_PROPERTIES = ".properties"; //$NON-NLS-1$
	/* package */ static final String F_ROOT = ".root"; //$NON-NLS-1$
	/* package */ static final String F_SAFE_TABLE = ".safetable"; //$NON-NLS-1$
	/* package */ static final String F_SNAP = ".snap"; //$NON-NLS-1$
	/* package */ static final String F_SNAP_EXTENSION = "snap"; //$NON-NLS-1$
	/* package */ static final String F_SYNCINFO = ".syncinfo"; //$NON-NLS-1$
	/* package */ static final String F_TREE = ".tree"; //$NON-NLS-1$
public LocalMetaArea() {
}
/**
 * For backwards compatibility, if there is a project at the old 
 * project description location, delete it.
 */
public void clearOldDescription(IProject target) {
	Workspace.clear(getDescriptionLocationFor(target).toFile());
}
public void create(IProject target) {
	java.io.File file = locationFor(target).toFile();
	//make sure area is empty
	Workspace.clear(file);
	file.mkdirs();
}
/**
 * The project is being deleted.  Delete all meta-data associated with the project.
 */
public void delete(IProject target) throws CoreException {
	IPath path = locationFor(target);
	if (!Workspace.clear(path.toFile()) && path.toFile().exists()) {
		String message = Policy.bind("resources.deleteMeta", target.getFullPath().toString()); //$NON-NLS-1$
		throw new ResourceException(IResourceStatus.FAILED_DELETE_METADATA, target.getFullPath(), message, null);
	}
}


public IPath getBackupLocationFor(IPath file) {
	return file.removeLastSegments(1).append(file.lastSegment() + F_BACKUP_FILE_EXTENSION);
}
/**
 * Returns the location of the private project description file in the metadata
 * area.  Also, in 1.0, this was the only location for the project description
 * file.
 */
protected IPath getDescriptionLocationFor(IProject target) {
	return locationFor(target).append(F_PROJECT);
}
public IPath getHistoryStoreLocation() {
	return getLocation().append(F_HISTORY_STORE);
}
/**
 * Returns the local filesystem location which contains the META data for
 * the resources plugin (i.e., the entire workspace).
 */
public IPath getLocation() {
	if (metaAreaLocation == null)
		metaAreaLocation = ResourcesPlugin.getPlugin().getStateLocation();
	return metaAreaLocation;
}
/**
 * Returns the path of the file in which to save markers for the given resource.
 * Should only be called for the workspace root and projects.
 */
public IPath getMarkersLocationFor(IResource resource) {
	Assert.isNotNull(resource);
	Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
	return locationFor(resource).append(F_MARKERS);
}
/**
 * Returns the path of the file in which to snapshot markers for the given resource.
 * Should only be called for the workspace root and projects.
 */
public IPath getMarkersSnapshotLocationFor(IResource resource) {
	return getMarkersLocationFor(resource).addFileExtension(F_SNAP_EXTENSION);
}
public IPath getPropertyStoreLocation(IResource resource) {
	int type = resource.getType();
	Assert.isTrue(type != IResource.FILE && type != IResource.FOLDER);
	return locationFor(resource).append(F_PROPERTIES);
}
public IPath getSafeTableLocationFor(String pluginId) {
	IPath prefix = getLocation().append(F_SAFE_TABLE);
	// if the plugin is the resources plugin, we return the master table location
	if (pluginId.equals(ResourcesPlugin.getPlugin().getDescriptor().getUniqueIdentifier()))
		return prefix.append(pluginId); // master table
	int saveNumber = getWorkspace().getSaveManager().getSaveNumber(pluginId);
	return prefix.append(pluginId + "." + saveNumber); //$NON-NLS-1$
}
public IPath getSnapshotLocationFor(IResource resource) {
	return getLocation().append(F_SNAP);
}
/**
 * Returns the path of the file in which to save the sync information for the given resource.
 * Should only be called for the workspace root and projects.
 */
public IPath getSyncInfoLocationFor(IResource resource) {
	Assert.isNotNull(resource);
	Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
	return locationFor(resource).append(F_SYNCINFO);
}
/**
 * Returns the path of the file in which to snapshot the sync information for the given resource.
 * Should only be called for the workspace root and projects.
 */
public IPath getSyncInfoSnapshotLocationFor(IResource resource) {
	return getSyncInfoLocationFor(resource).addFileExtension(F_SNAP_EXTENSION);
}
/**
 * Returns the local file system location of the tree file for the given resource. This
 * file does not follow the same save number as its plug-in. So, the number here is called
 * "sequence number" and not "save number" to avoid confusion.
 */
public IPath getTreeLocationFor(IResource target, boolean updateSequenceNumber) {
	IPath key = target.getFullPath().append(F_TREE);
	String sequenceNumber = getWorkspace().getSaveManager().getMasterTable().getProperty(key.toString());
	if (sequenceNumber == null)
		sequenceNumber = "0"; //$NON-NLS-1$
	if (updateSequenceNumber) {
		int n = new Integer(sequenceNumber).intValue() + 1;
		n = n < 0 ? 1 : n;
		sequenceNumber = new Integer(n).toString();
		getWorkspace().getSaveManager().getMasterTable().setProperty(key.toString(), new Integer(sequenceNumber).toString());
	}
	return locationFor(target).append(sequenceNumber + F_TREE);
}
public IPath getWorkingLocation(IResource resource, IPluginDescriptor plugin) {
	return locationFor(resource).append(plugin.getUniqueIdentifier());
}
protected Workspace getWorkspace() {
	return (Workspace) ResourcesPlugin.getWorkspace();
}
public IPath getWorkspaceDescriptionLocation() {
	return getLocation().append(F_DESCRIPTION);
}
public boolean hasSavedProject(IProject project) {
	//if there is a location file, then the project exists
	return getDescriptionLocationFor(project).toFile().exists() || locationFor(project).append(F_OLD_PROJECT_LOCATION).toFile().exists();
}
public boolean hasSavedWorkspace() throws CoreException {
	return getWorkspaceDescriptionLocation().toFile().exists() || getBackupLocationFor(getWorkspaceDescriptionLocation()).toFile().exists();
}
/**
 * Returns the local filesystem location in which the meta data for the given
 * resource is stored.
 */
public IPath locationFor(IResource resource) {
	if (resource.getType() == IResource.ROOT)
		return getLocation().append(F_ROOT);
	else
		return getLocation().append(F_PROJECTS).append(resource.getProject().getName());
}
/**
 * Reads and returns the project content location for the given project.
 * Returns null if the default content location should be used.
 * In the case of failure, log the exception and return null, thus reverting to 
 * using the default location.  This method is for backwards compatibility
 * with 2.0 workspaces that contained a .location file.
 */
protected IPath readOldLocation(IProject target) {
	IPath locationFile = locationFor(target).append(F_OLD_PROJECT_LOCATION);
	java.io.File file = locationFile.toFile();
	if (!file.exists()) {
		locationFile = getBackupLocationFor(locationFile);
		file = locationFile.toFile();
		if (!file.exists())
			return null;
	}
	try {
		SafeChunkyInputStream input = new SafeChunkyInputStream(file);
		DataInputStream dataIn = new DataInputStream(input);
		try {
			String projectLocation = dataIn.readUTF();
			return new Path(projectLocation);
		} finally {
			dataIn.close();
		}
	} catch (IOException e) {
		String msg = Policy.bind("resources.exReadProjectLocation", target.getName()); //$NON-NLS-1$
		ResourcesPlugin.getPlugin().getLog().log(new ResourceStatus(
			IStatus.ERROR, IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, e));
		return null;
	}
}
/**
 * Reads and returns the project description for the given project.
 * Returns null if there was no project description file on disk.
 * Throws an exception if there was any failure to read the project.
 */
public ProjectDescription read(IProject project) throws CoreException {
	IPath path = getDescriptionLocationFor(project);
	if (!path.toFile().exists()) {
		//look for an old .location file (2.0 workspaces)
		ProjectDescription result = new ProjectDescription();
		result.setLocation(readOldLocation(project));
		return result;
	}
	IPath tempPath = getBackupLocationFor(path);
	ProjectDescription description = null;
	try {
		description = (ProjectDescription)new ModelObjectReader().read(path, tempPath);
	} catch (IOException e) {
		String msg = Policy.bind("resources.readMeta", project.getName()); //$NON-NLS-1$
		throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, project.getFullPath(), msg, e);
	}
	if (description == null) {
		String msg = Policy.bind("resources.readMeta", project.getName()); //$NON-NLS-1$
		throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, project.getFullPath(), msg, null);
	}
	return description;
}
public WorkspaceDescription readWorkspace() throws CoreException {
	IPath path = getWorkspaceDescriptionLocation();
	IPath tempPath = getBackupLocationFor(path);
	try {
		return (WorkspaceDescription) new ModelObjectReader().read(path, tempPath);
	} catch (IOException e) {
		return null;
	}
}
/**
 * Write the private state from the project description.
 */
public void write(IProject target) throws CoreException {
	IPath location = locationFor(target).append(F_PROJECT);
	java.io.File file = location.toFile();
	//delete any old file
	Workspace.clear(file);
	IProjectDescription description = ((Project)target).internalGetDescription().copyWithPrivateState();
	if (description == null)
		return;
	try {
		new ModelObjectWriter().write(description, location, getBackupLocationFor(location));
	} catch (IOException e) {
		String message = Policy.bind("resources.exSaveProjectLocation", target.getName()); //$NON-NLS-1$
		throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, e);
	}
	//clear legacy .location file
	Workspace.clear(locationFor(target).append(F_OLD_PROJECT_LOCATION).toFile());
}
/**
 * Writes the workspace description to the local meta area. This method
 * is synchronized to prevent multiple current write attempts.
 */
public synchronized void write(WorkspaceDescription description) throws CoreException {
	IPath path = getWorkspaceDescriptionLocation();
	path.toFile().getParentFile().mkdirs();
	IPath tempPath = getBackupLocationFor(path);
	try {
		new ModelObjectWriter().write(description, path, tempPath);
	} catch (IOException e) {
		String message = Policy.bind("resources.writeWorkspaceMeta", path.toString()); //$NON-NLS-1$
		throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, null, message, e);
	}
	description.clean();
}
}
