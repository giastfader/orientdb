/*
 *
 *  *  Copyright 2014 Orient Technologies LTD (info(at)orientechnologies.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientechnologies.com
 *
 */
package com.orientechnologies.orient.core.exception;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.common.exception.OHighLevelException;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.version.ORecordVersion;
import com.orientechnologies.orient.core.version.OVersionFactory;

/**
 * Exception thrown when MVCC is enabled and a record cannot be updated or deleted because versions don't match.
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 * 
 */
public class OConcurrentModificationException extends ONeedRetryException implements OHighLevelException {

  private static final String MESSAGE_OPERATION      = "are";
  private static final String MESSAGE_RECORD_VERSION = "your=v";
  private static final String MESSAGE_DB_VERSION     = "db=v";

  private static final long   serialVersionUID       = 1L;

  private ORID                rid;
  private ORecordVersion      databaseVersion        = OVersionFactory.instance().createVersion();
  private ORecordVersion      recordVersion          = OVersionFactory.instance().createVersion();
  private int                 recordOperation;

  public OConcurrentModificationException(OConcurrentModificationException exception) {
    super(exception);

    this.rid = exception.rid;
    this.recordVersion = exception.recordVersion;
    this.databaseVersion = exception.databaseVersion;
    this.recordOperation = exception.recordOperation;
  }

  protected OConcurrentModificationException(String message) {
    super(message);
  }

  public OConcurrentModificationException(final ORID iRID, final ORecordVersion iDatabaseVersion,
      final ORecordVersion iRecordVersion, final int iRecordOperation) {
    super(makeMessage(iRecordOperation, iRID, iDatabaseVersion, iRecordVersion));

    if (OFastConcurrentModificationException.enabled())
      throw new IllegalStateException("Fast-throw is enabled. Use OFastConcurrentModificationException.instance() instead");

    rid = iRID;
    databaseVersion.copyFrom(iDatabaseVersion);
    recordVersion.copyFrom(iRecordVersion);
    recordOperation = iRecordOperation;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof OConcurrentModificationException))
      return false;

    final OConcurrentModificationException other = (OConcurrentModificationException) obj;

    if (recordOperation == other.recordOperation && rid.equals(other.rid)) {
      if ((databaseVersion == null && other.databaseVersion == null)
          || (databaseVersion != null && databaseVersion.equals(other.databaseVersion)))
        if ((recordVersion == null && other.recordVersion == null)
            || (recordVersion != null && recordVersion.equals(other.recordVersion)))
          return true;
    }

    return false;
  }

  public ORecordVersion getEnhancedDatabaseVersion() {
    return databaseVersion;
  }

  public ORecordVersion getEnhancedRecordVersion() {
    return recordVersion;
  }

  public ORID getRid() {
    return rid;
  }

  private static String makeMessage(int recordOperation, ORID rid, ORecordVersion databaseVersion, ORecordVersion recordVersion) {
    final String operation = ORecordOperation.getName(recordOperation);

    final StringBuilder sb = new StringBuilder();
    sb.append("Cannot ");
    sb.append(operation);
    sb.append(" the record ");
    sb.append(rid);
    sb.append(" because the version is not the latest. Probably you are ");
    sb.append(operation.toLowerCase().substring(0, operation.length() - 1));
    sb.append("ing an old record or it has been modified by another user (db=v");
    sb.append(databaseVersion);
    sb.append(" your=v");
    sb.append(recordVersion);
    sb.append(")");
    return sb.toString();
  }
}
