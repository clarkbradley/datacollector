/*
 *  Copyright 2017 StreamSets Inc.
 *
 *  Licensed under the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.streamsets.datacollector.credential;

import com.google.common.collect.ImmutableSet;
import com.streamsets.lib.security.http.HeadlessSSOPrincipal;
import com.streamsets.lib.security.http.SSOPrincipal;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.credential.CredentialStore;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;

public class TestGroupEnforcerCredentialStore {

  @Test(expected = RuntimeException.class)
  public void testNoContext() throws StageException {
    CredentialStore store = Mockito.mock(CredentialStore.class);
    store = new GroupEnforcerCredentialStore(store);
    store.get("g", "n", "o");
  }

  @Test
  public void testNotEnforced() throws Exception {
    CredentialStore store = Mockito.mock(CredentialStore.class);
    Mockito.when(store.get(Mockito.eq("g"), Mockito.eq("n"), Mockito.eq("o"))).thenReturn("c");
    GroupEnforcerCredentialStore enforcerStore = new GroupEnforcerCredentialStore(store);

    SSOPrincipal principal = HeadlessSSOPrincipal.createRecoveryPrincipal("uid");
    Subject subject = new Subject();
    subject.getPrincipals().add(principal);
    String value = Subject.doAs(subject, (PrivilegedExceptionAction<String>) () -> enforcerStore.get("g", "n", "o"));

    Assert.assertEquals("c", value);
  }

  @Test
  public void testEnforcedOk() throws Exception {
    CredentialStore store = Mockito.mock(CredentialStore.class);
    Mockito.when(store.get(Mockito.eq("g"), Mockito.eq("n"), Mockito.eq("o"))).thenReturn("c");
    GroupEnforcerCredentialStore enforcerStore = new GroupEnforcerCredentialStore(store);

    SSOPrincipal principal = new HeadlessSSOPrincipal("uid", ImmutableSet.of("g"));
    Subject subject = new Subject();
    subject.getPrincipals().add(principal);
    String value = Subject.doAs(subject, (PrivilegedExceptionAction<String>) () -> enforcerStore.get("g", "n", "o"));

    Assert.assertEquals("c", value);
  }

  @Test(expected = StageException.class)
  public void testEnforcedFail() throws Throwable {
    CredentialStore store = Mockito.mock(CredentialStore.class);
    Mockito.when(store.get(Mockito.eq("g"), Mockito.eq("n"), Mockito.eq("o"))).thenReturn("c");
    GroupEnforcerCredentialStore enforcerStore = new GroupEnforcerCredentialStore(store);

    SSOPrincipal principal = new HeadlessSSOPrincipal("uid", ImmutableSet.of("g"));
    Subject subject = new Subject();
    subject.getPrincipals().add(principal);
    try {
      Subject.doAs(subject, (PrivilegedExceptionAction<String>) () -> enforcerStore.get("h", "n", "o"));
    } catch (Exception ex) {
      throw ex.getCause();
    }
  }

}