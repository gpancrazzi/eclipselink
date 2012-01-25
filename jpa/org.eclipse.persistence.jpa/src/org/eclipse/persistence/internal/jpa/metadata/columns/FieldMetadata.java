/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metadata.columns;

import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;

/**
 * INTERNAL:
 * Synonym for Column for NoSql data.
 */
public class FieldMetadata extends ColumnMetadata {
    
    /**
     * INTERNAL:
     * Used for XML loading.
     */
    public FieldMetadata() {
        super("<field>");
    }
    
    /**
     * INTERNAL:
     * Used for annotation loading.
     */
    public FieldMetadata(MetadataAnnotation column, MetadataAccessor accessor) {
        super(column, accessor);
    }
}
