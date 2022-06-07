/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.persistence.platform.database;

import org.eclipse.persistence.queries.ValueReadQuery;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p><b>Purpose</b>: Provides MariaDB specific behavior.
 */
public class MariaDBPlatform extends MySQLPlatform {

    @Override
    public void initializeConnectionData(Connection connection) throws SQLException {
        this.driverSupportsNationalCharacterVarying = true;
    }

    @Override
    public boolean isMariaDB() {
        return true;
    }

    @Override
    public boolean isMySQL() {
        return false;
    }

    @Override
    public boolean isFractionalTimeSupported() {
        return true;
    }

    /**
     * INTERNAL: Indicates whether the platform supports sequence objects.
     */
    @Override
    public boolean supportsSequenceObjects() {
        return true;
    }

    /**
     * INTERNAL: Returns query used to read value generated by sequence object
     * (like Oracle sequence). This method is called when sequence object
     * NativeSequence is connected, the returned query used until the sequence
     * is disconnected. If the platform supportsSequenceObjects then (at least)
     * one of buildSelectQueryForSequenceObject methods should return non-null
     * query.
     */
    @Override
    public ValueReadQuery buildSelectQueryForSequenceObject(String qualifiedSeqName, Integer size) {
        return new ValueReadQuery("select nextval(" + qualifiedSeqName + ")");
    }

    /**
     * INTERNAL: Override this method if the platform supports sequence objects
     * and it's possible to alter sequence object's increment in the database.
     */
    @Override
    public boolean isAlterSequenceObjectSupported() {
        return true;
    }

    /**
     * Return the drop schema definition. Subclasses should override as needed.
     */
    @Override
    public String getDropDatabaseSchemaString(String schema) {
        return "DROP SCHEMA IF EXISTS " + schema;
    }
}
