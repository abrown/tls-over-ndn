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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public abstract class BaseStream implements Closeable {

	protected final Face face;
	protected final Name prefix;
	protected final int writeBufferSize;
	protected InputStream input;
	protected OutputStream output;

	public BaseStream(Face face, Name prefix, int writeBufferSize) {
		this.face = face;
		this.prefix = prefix;
		this.writeBufferSize = writeBufferSize;
	}

	/**
	 * @return an input stream
	 */
	public InputStream input() {
		return input;
	}

	/**
	 * @return the output stream for this client
	 */
	public OutputStream output() {
		return output;
	}

	/**
	 * Shut down the streams
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		input.close();
		output.close();
	}

}
