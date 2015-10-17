/*
 * ndn-streams
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jndn.ndn_streams;

import com.intel.jndn.mock.MockFace;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.named_data.jndn.Name;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test NdnClientStream
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NdnClientStreamTest {

	MockFace face;
	NdnClientStream instance;

	public NdnClientStreamTest() {
		face = new MockFace();
		instance = new NdnClientStream(face, new Name("/test/client/stream"), 100);
	}

	@Test
	public void testSend() {
		instance.send(ByteBuffer.allocate(50));
		assertEquals(1, face.getTransport().getSentInterestPackets().size());
		assertEquals(50, face.getTransport().getSentInterestPackets().get(0).getContent().size());
	}

	@Test
	public void testOutputBuffering() throws IOException {
		instance.output().write(ByteBuffer.allocate(50).array());
		assertEquals(0, face.getTransport().getSentInterestPackets().size());

		instance.output().write(ByteBuffer.allocate(50).array());
		assertEquals(1, face.getTransport().getSentInterestPackets().size());

		instance.output().write(ByteBuffer.allocate(50).array());
		assertEquals(1, face.getTransport().getSentInterestPackets().size());
	}

	@Test
	public void testClose() throws Exception {
	}

}
