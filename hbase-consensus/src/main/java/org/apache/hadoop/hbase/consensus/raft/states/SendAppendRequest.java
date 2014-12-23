package org.apache.hadoop.hbase.consensus.raft.states;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.consensus.exceptions.LeaderNotReadyException;
import org.apache.hadoop.hbase.consensus.fsm.Event;
import org.apache.hadoop.hbase.consensus.protocol.EditId;
import org.apache.hadoop.hbase.consensus.quorum.AppendConsensusSessionInterface;
import org.apache.hadoop.hbase.consensus.quorum.MutableRaftContext;
import org.apache.hadoop.hbase.consensus.raft.events.ReplicateEntriesEvent;

import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * The basic logic here is to ensure there is only one outstanding AppendRequest
 * in the quorum (HB is one special AppendRequest as well). The following table
 * described the mechanism of handling the next AppendRequest if the current Append
 * session has not been completed yet.
 *
 * ----------------------------------------------------------
 * |next\current |     HB               |     Append        |
 * ----------------------------------------------------------
 * |   HB        |  resend HB           | resend Append     |
 * ----------------------------------------------------------
 * |  Append     |  send immediately    | NotReadyException |
 * ----------------------------------------------------------
 */
public class SendAppendRequest extends RaftAsyncState {
  public static final Log LOG = LogFactory.getLog(SendAppendRequest.class);
  private ListenableFuture<?> sendAppendRequestFuture;

  public SendAppendRequest(MutableRaftContext context) {
    super(RaftStateType.SEND_APPEND_REQUEST, context);
  }

  @Override
  public boolean isComplete() {
    return sendAppendRequestFuture == null || sendAppendRequestFuture.isDone();
  }

  @Override
  public ListenableFuture<?> getAsyncCompletion() {
    return sendAppendRequestFuture;
  }

  @Override
  public void onEntry(final Event event) {
    super.onEntry(event);
    sendAppendRequestFuture = null;

    final EditId currentEdit = c.getCurrentEdit();
    ReplicateEntriesEvent rEvent = (ReplicateEntriesEvent)event;

    AppendConsensusSessionInterface session = c.getAppendSession(currentEdit);

    if (session != null && !session.isComplete()) {
      // Handling the case where the current append session has NOT completed

      if (rEvent.isHeartBeat() && !session.isTimeout()) {
        // Resend the heartbeat
        c.resendOutstandingAppendRequest();
      } else {
        if (session.getAppendRequest().isHeartBeat()) {
          // Cancel the original AppendSession
          session.cancel();
          // Resend the ReplicateEntriesEvent
          c.getHeartbeatTimer().reset();
          c.sendAppendRequest(rEvent);
        } else {
          // Throw the LeaderNotReadyException
          rEvent.setReplicationFailed(new LeaderNotReadyException(
            c.getLeaderNotReadyMsg()));
        }
      }
    } else { // Handling the case where the current session has completed
      // Resend the ReplicateEntriesEvent
      c.getHeartbeatTimer().reset();
      sendAppendRequestFuture = c.sendAppendRequest(rEvent);
    }
  }
}
