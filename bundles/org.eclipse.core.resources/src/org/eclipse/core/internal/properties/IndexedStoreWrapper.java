package org.eclipse.core.internal.properties;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.internal.indexing.*;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class IndexedStoreWrapper {

	private IndexedStore store;
	private IPath location;
	
	/* constants */
	private static final String INDEX_NAME = "index";


public IndexedStoreWrapper(IPath location) {
	this.location = location;
}

private void open() {
	try {
		store = new IndexedStore();
		store.open(location.toOSString());
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotOpen", location.toOSString());
		ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
		ResourcesPlugin.getPlugin().getLog().log(status);
	}
}

private void recreate() {
	close();
	String name = location.toOSString();
	// Rename the problematic store for future analysis.
	location.toFile().renameTo(location.append(".001").toFile());
	location.toFile().delete();
	if (!location.toFile().exists())
		open();
}

public synchronized void close() {
	if (store == null)
		return;
	try {
		store.close();
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotClose", location.toOSString());
		ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
		ResourcesPlugin.getPlugin().getLog().log(status);
	}
}

public synchronized void commit() throws CoreException {
	if (store == null)
		return;
	try {
		store.commit();
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotCommit", location.toOSString());
		ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
		throw new ResourceException(status);
	}
}

private void create() throws CoreException {
	open();
	if (store == null) {
		recreate();
		if (store == null) {
			String message = Policy.bind("indexed.couldNotCreate", location.toOSString());
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, null);
			throw new ResourceException(status);
		}
	}		
}

private Index createIndex() throws CoreException {
	try {
		return getStore().createIndex(INDEX_NAME);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotCreateIndex", location.toOSString());
		ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
		throw new ResourceException(status);
	}
}

public synchronized Index getIndex() throws CoreException {
	Exception problem = null;
	try {
		return getStore().getIndex(INDEX_NAME);
	} catch (IndexedStoreException e) {
		if (e.id == IndexedStoreException.IndexNotFound)
			return createIndex();
		problem = e;
		return null;
	} catch (Exception e) {
		problem = e;
		return null;
	} finally {
		if (problem != null) {
			String message = Policy.bind("indexed.couldNotGetIndex", location.toOSString());
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_READ_LOCAL, location, message, problem);
			throw new ResourceException(status);
		}
	}
}

public synchronized void rollback() {
	if (store == null)
		return;
	try {
		store.rollback();
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotCommit", location.toOSString());
		ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
		ResourcesPlugin.getPlugin().getLog().log(status);
	}
}

public synchronized String getObjectAsString(ObjectID id) throws CoreException {
	try {
		return getStore().getObjectAsString(id);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotRead", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, location, message, e);
	}
}

private IndexedStore getStore() throws CoreException {
	if (store == null)
		create();
	return store;
}

public synchronized IndexCursor getCursor() throws CoreException {
	try {
		return getIndex().open();
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotCreateCursor", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, location, message, e);
	}
}

public synchronized ObjectID createObject(String s) throws CoreException {
	try {
		return getStore().createObject(s);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotWrite", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
	}
}

public synchronized ObjectID createObject(byte[] b) throws CoreException {
	try {
		return getStore().createObject(b);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotWrite", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, location, message, e);
	}
}


public synchronized void removeObject(ObjectID id) throws CoreException {
	try {
		getStore().removeObject(id);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotDelete", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_DELETE_LOCAL, location, message, e);
	}
}

public synchronized byte[] getObject(ObjectID id) throws CoreException {
	try {
		return getStore().getObject(id);
	} catch (Exception e) {
		String message = Policy.bind("indexed.couldNotRead", location.toOSString());
		throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, location, message, e);
	}
}

public synchronized void reset() {
	recreate();
}
}