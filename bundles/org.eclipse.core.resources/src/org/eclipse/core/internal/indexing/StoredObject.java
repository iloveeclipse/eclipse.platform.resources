/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.indexing;

import java.util.Observable;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

public abstract class StoredObject extends Observable implements Referable, Insertable {

	public static final int MAXIMUM_OBJECT_SIZE = ObjectStore.MAXIMUM_OBJECT_SIZE;
	public static final int TYPE_LENGTH = 2;
	public static final int TYPE_OFFSET = 0;
	protected ObjectAddress address;
	protected int referenceCount;

	protected ObjectStore store;
	protected int type;

	/** 
	 * Constructs a new object so that it can be stored.
	 */
	protected StoredObject() {
		type = getRequiredType();
	}

	/** 
	 * Constructs a new instance from a field.
	 */
	protected StoredObject(Field f, ObjectStore store, ObjectAddress address) throws CoreException {
		if (f.length() < getMinimumSize() || f.length() > getMaximumSize())
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("objectStore.objectSizeFailure"), null));//$NON-NLS-1$
		extractValues(f);
		setStore(store);
		setAddress(address);
	}

	/**
	 * Adds a reference.
	 */
	public final int addReference() {
		referenceCount++;
		return referenceCount;
	}

	/**
	 * Places the contents of the buffer into the members.
	 * Subclasses should implement and call super.
	 */
	protected void extractValues(Field f) throws CoreException {
		type = f.subfield(TYPE_OFFSET, TYPE_LENGTH).getInt();
		if (type != getRequiredType())
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("objectStore.objectTypeFailure"), null));//$NON-NLS-1$
	}

	/**
	 * Returns the address of the object.
	 * Subclasses must not override.
	 */
	public final ObjectAddress getAddress() {
		return address;
	}

	/**
	 * Returns the maximum size of this object's instance -- including its type field.
	 * Subclasses can override.  The default is to have the this be equal to the minimum
	 * size, forcing a fixed size object.
	 */
	protected int getMaximumSize() {
		return getMinimumSize();
	}

	/**
	 * Returns the minimum size of this object's instance -- including its type field.
	 * Subclasses should override.
	 */
	protected int getMinimumSize() {
		return 2;
	}

	/**
	 * Returns the required type of this class of object.
	 * Subclasses must override.
	 */
	protected abstract int getRequiredType();

	/**
	 * Returns the store of the object.
	 * Subclasses must not override.
	 */
	public final ObjectStore getStore() {
		return store;
	}

	/**
	 * Tests for existing references.
	 */
	public final boolean hasReferences() {
		return referenceCount > 0;
	}

	/**
	 * Places the contents of the fields into the buffer.
	 * Subclasses should implement and call super.
	 */
	protected void insertValues(Field f) {
		f.subfield(TYPE_OFFSET, TYPE_LENGTH).put(type);
	}

	/**
	 * Returns the actual size of this object's instance -- including its type field.
	 * Subclasses should override.
	 */
	protected int length() {
		return getMinimumSize();
	}

	/**
	 * Removes a reference.
	 */
	public final int removeReference() {
		if (referenceCount > 0)
			referenceCount--;
		return referenceCount;
	}

	public final void setAddress(ObjectAddress address) {
		this.address = address;
	}

	public final void setStore(ObjectStore store) {
		this.store = store;
	}

	/**
	 * Returns a byte array value of the object.
	 */
	public final byte[] toByteArray() {
		Field f = new Field(length());
		insertValues(f);
		return f.get();
	}

	/** 
	 * Provides a printable representation of this object.  Subclasses must implement.
	 */
	public abstract String toString();
}