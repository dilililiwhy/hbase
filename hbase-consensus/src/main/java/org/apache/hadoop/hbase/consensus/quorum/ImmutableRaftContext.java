package org.apache.hadoop.hbase.consensus.quorum;

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


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HServerAddress;
import org.apache.hadoop.hbase.consensus.log.CommitLogManagerInterface;
import org.apache.hadoop.hbase.consensus.metrics.ConsensusMetrics;
import org.apache.hadoop.hbase.consensus.protocol.ConsensusHost;
import org.apache.hadoop.hbase.consensus.protocol.EditId;
import org.apache.hadoop.hbase.consensus.rpc.PeerStatus;
import org.apache.hadoop.hbase.regionserver.RaftEventListener;

import java.util.Map;

/**
 * Declares a set of immutable methods which can be used to make decisions
 * during various events in the state machine.
 */
public interface ImmutableRaftContext {
  /**
   * Tells whether it is currently leader or not.
   * @return
   */
  boolean isLeader();

  /**
   * Tells whether it is currently a candidate or not.
   * @return
   */
  boolean isCandidate();

  /**
   * Tells whether it is currently a follower or not.
   * @return
   */
  boolean isFollower();

  /**
   * Returns the current {term, index} for the state.
   * @return
   */
  EditId getCurrentEdit();

  /**
   * Returns the last committed {term, index} for the state.
   * @return
   */
  EditId getCommittedEdit();

  /**
   * Returns the last round's {term, index}.
   * @return
   */
  EditId getPreviousEdit();

  /**
   * Get the current leader's information.
   * @return
   */
  ConsensusHost getLeader();

  /**
   * Returns the ID for the current server
   * @return
   */
  String getMyAddress();

  /**
   * Returns the majority cnt for the current quorum, including the current server
   * @return
   */
  int getMajorityCnt();

  /**
   * Get the id of the last peer we voted for.
   * @return
   */
  ConsensusHost getLastVotedFor();

  /**
   * Return the outstanding append session.
   * @return
   */
  AppendConsensusSessionInterface getOutstandingAppendSession();

  /**
   * Return the outstanding append session if it matches the given edit.
   * @param id
   * @return
   */
  AppendConsensusSessionInterface getAppendSession(final EditId id);

  /**
   * Return the outstanding election session.
   * @return
   */
  VoteConsensusSessionInterface getOutstandingElectionSession();

  /**
   * Return the outstanding election session if it matches the given edit.
   * @param id
   * @return
   */
  VoteConsensusSessionInterface getElectionSession(final EditId id);

  /**
   * Is transaction log accessible
   */
  boolean isLogAccessible();

  int getRanking();

  boolean validateLogEntry(final EditId id);

  String getQuorumName();

  Configuration getConf();

  EditId getLastLogIndex();

  void stop(boolean wait);

  CommitLogManagerInterface getLogManager();

  QuorumInfo getQuorumInfo();

  RaftEventListener getDataStoreEventListener();

  long getMinUnPersistedIndexAcrossQuorum();

  ConsensusMetrics getConsensusMetrics();

  Map<HServerAddress,Integer> getNewConfiguration();

  QuorumMembershipChangeRequest getUpdateMembershipRequest();

  long getPurgeIndex();

  PeerStatus getStatus();

  int getAppendEntriesMaxTries();

  long getLastAppendRequestReceivedTime();

  int getNumPendingEvents();

  boolean isPartOfNewQuorum();
}
