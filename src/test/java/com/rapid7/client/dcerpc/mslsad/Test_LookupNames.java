/**
 * Copyright 2017, Rapid7, Inc.
 *
 * License: BSD-3-clause
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 */

package com.rapid7.client.dcerpc.mslsad;

import com.rapid7.client.dcerpc.mslsad.messages.LsarLookupNamesRequest;
import com.rapid7.client.dcerpc.mslsad.messages.LsarLookupNamesResponse;
import com.rapid7.client.dcerpc.mslsad.objects.LSAPRReferencedDomainList;
import com.rapid7.client.dcerpc.mslsad.objects.LSAPRTranslatedSIDs;
import com.rapid7.client.dcerpc.objects.ContextHandle;
import com.rapid7.client.dcerpc.objects.MalformedSIDException;
import com.rapid7.client.dcerpc.objects.RPCSID;
import com.rapid7.client.dcerpc.service.dto.SID;
import com.rapid7.client.dcerpc.transport.RPCTransport;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Test_LookupNames {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @SuppressWarnings("unchecked")
    @Test
    public void parseLookupNamesResponse()
        throws IOException
    {
        final LsarLookupNamesResponse response = new LsarLookupNamesResponse();
        response.fromHexString(
            "00000200010000000400020020000000010000001a001c00080002000c0002000e000000000000000d0000005700310030002d0045004e0054002d005800360034002d005500000004000000010400000000000515000000a43cb4affe0503bd73de0f3501000000100002000100000001000000f4010000000000000100000000000000");
        LSAPRReferencedDomainList lsaprReferencedDomainList = response.getLsaprReferencedDomainList();
        LSAPRTranslatedSIDs lsaprTranslatedSIDs = response.getLsaprTranslatedSIDs();

        assertEquals(lsaprReferencedDomainList.getLsaprTrustInformations()[0].getSid().toString(), "S-1-5-21-2947824804-3171091966-890232435");
        assertEquals(lsaprTranslatedSIDs.getLsaprTranslatedSIDArray()[0].getRelativeId(), 500);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void encodeLookupNamesRequest()
        throws IOException {
        final ContextHandle fakePolicyHandle = new ContextHandle("000000008e3039708fdd9f488f9665426d0d9c57");
        final String[] names = {"Administrator"};
        final LsarLookupNamesRequest request = new LsarLookupNamesRequest(fakePolicyHandle, names);
        assertEquals(request.toHexString(), "000000008e3039708fdd9f488f9665426d0d9c5701000000010000001a001a00000002000d000000000000000d000000410064006d0069006e006900730074007200610074006f007200000000000000000000000100000000000000");
    }

    //This test is to verify that the Service correctly sets invalid SIDs to null from a valid response
    @Test
    public void testLookupNamesService() throws IOException, MalformedSIDException {
        final RPCTransport transport = mock(RPCTransport.class);
        final ContextHandle handle = mock(ContextHandle.class);
        final LocalSecurityAuthorityService localSecurityAuthorityService = new LocalSecurityAuthorityService(transport);

        final LsarLookupNamesResponse response = new LsarLookupNamesResponse();

        response.fromHexString(
                "00000200010000000400020020000000010000001a001c00080002000c0002000e000000000000000d0000005700310030002d0045004e0054002d005800360034002d005500000004000000010400000000000515000000a43cb4affe0503bd73de0f3504000000100002000400000001000000f40100000000000001000000e90300000000000001000000f5010000000000000800000000000000ffffffff0300000007010000");

        when(transport.call(any(LsarLookupNamesRequest.class))).thenReturn(response);

        SID[] SIDs = localSecurityAuthorityService.lookupNames(handle, (String[]) null);

        SID domainSID = new SID((byte)1,new byte[]{0,0,0,0,0,5}, new long[]{21,2947824804l,3171091966l, 890232435l});
        SID[] expectedSIDs = new SID[4];
        expectedSIDs[0] = domainSID.addRelativeId(500);
        expectedSIDs[1] = domainSID.addRelativeId(1001);
        expectedSIDs[2] = domainSID.addRelativeId(501);
        expectedSIDs[3] = null;

        assertArrayEquals(SIDs, expectedSIDs);
    }
}
