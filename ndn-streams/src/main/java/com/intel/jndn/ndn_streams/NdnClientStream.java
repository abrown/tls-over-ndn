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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.util.Blob;

/**
 * This class streams bytes to and from a server using NDN
 * packets--specifically, Interests with payloads and their associated Data
 * packets. Because NDN is pull-based, every transaction must be client
 * initiated; in other words, if the server calls write() multiple times but the
 * client never calls read() or write(), the server's bytes will never reach the
 * client. In the future, this could be fixed by immediately sending an empty
 * Interest from the client with possible retries (perhaps exponential
 * backoff?).
 * 
 * Yet to implement:
 *  - data signature validation
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NdnClientStream extends BaseStream {

	private static final int SECURE_NONCE_SIZE = 32;
	private static final Logger logger = Logger.getLogger(NdnClientStream.class.getName());

	private final SecureRandom random = new SecureRandom();
	private final Queue<Transaction> orderedPackets = new ConcurrentLinkedQueue<>();
	private final Map<byte[], Transaction> indexedPackets = new HashMap<>();
	private final OnFailedTransaction onFailedTransaction = new OnFailedTransaction();
	private final OnCompletedTransaction onCompletedTransaction = new OnCompletedTransaction();
	private final Object mutex = new Object();

	/**
	 * Build a client stream
	 *
	 * @param face the NDN face must have processEvents() called on it by
	 * another thread; this class will not do so
	 * @param prefix the name the server is listening on
	 * @param writeBufferSize the number of bytes to buffer on write before
	 * flushing the contents to an expressed interest
	 */
	public NdnClientStream(Face face, Name prefix, int writeBufferSize) {
		super(face, prefix, writeBufferSize);
		this.input = new NdnClientInputStream();
		this.output = new NdnClientOutputStream(writeBufferSize);
	}

	/**
	 * Reads a stream of bytes from transactions made by the
	 * {@link NdnClientStream}. TODO optimize by overriding
	 * {@link InputStream#read(byte[])}
	 */
	class NdnClientInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			while (true) {
				Transaction transaction = orderedPackets.peek();
				if (transaction == null) {
					Transaction send = send(ByteBuffer.allocate(0));
					waitOn(send);
				} else if (transaction.state == Transaction.State.CONSUMED
						|| transaction.state == Transaction.State.FAILED) {
					orderedPackets.poll();
				} else if (transaction.state == Transaction.State.IN_FLIGHT) {
					waitOn(transaction);
				} else {
					return transaction.data.getContent().buf().get();
				}
			}
		}

		private synchronized void waitOn(Transaction transaction) {
			while (transaction.state == Transaction.State.IN_FLIGHT) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	};

	/**
	 * The client's output stream will buffer written bytes until it reaches
	 * {@link #writeBufferSize} or flush() is called; then it will send the
	 * buffered bytes as the payload of an interest
	 */
	class NdnClientOutputStream extends OutputStream {

		private ByteBuffer buffer;
		private final int writeBufferSize;

		public NdnClientOutputStream(int writeBufferSize) {
			this.writeBufferSize = writeBufferSize;
			this.buffer = ByteBuffer.allocateDirect(writeBufferSize);
		}

		@Override
		public void write(int b) throws IOException {
			buffer.put((byte) b);
			if (!buffer.hasRemaining()) {
				flush();
			}
		}

		@Override
		public void flush() {
			send(buffer);
			buffer = ByteBuffer.allocateDirect(writeBufferSize);
		}
	};

	/**
	 * Send bytes using an interest payload; the interest will be identified
	 * with a secure nonce so that any returned data can be matched to the
	 * corresponding transaction
	 *
	 * @param buffer the buffered bytes to send
	 * @return a transaction that will be completed when the corresponding data
	 * packet is returned
	 */
	public Transaction send(ByteBuffer buffer) {
		byte[] nonce = createSecureNonce(SECURE_NONCE_SIZE);
		Interest interest = createInterestWithPayload(buffer, nonce);

		Transaction transaction = new Transaction(interest);
		transaction.state = Transaction.State.IN_FLIGHT;
		orderedPackets.add(transaction);
		indexedPackets.put(nonce, transaction);

		try {
			face.expressInterest(interest, onCompletedTransaction, onFailedTransaction);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed to send interest", ex);
			transaction.state = Transaction.State.FAILED;
		}

		return transaction;
	}

	/**
	 * @param buffer the payload
	 * @param nonce the unique, secure, random identifier appended to the
	 * interest
	 * @return an interest
	 */
	private Interest createInterestWithPayload(ByteBuffer buffer, byte[] nonce) {
		Name name = new Name(prefix).append(nonce);
		Interest interest = new Interest(name);
		interest.setMustBeFresh(true);
		interest.setContent(new Blob(buffer, false));
		return interest;
	}

	/**
	 * @param size the number of bytes to generate
	 * @return an array of random bytes
	 */
	public byte[] createSecureNonce(int size) {
		byte[] bytes = new byte[size];
		random.nextBytes(bytes);
		return bytes;
	}

	/**
	 * Retrieve a transaction by its nonce
	 *
	 * @param interest the interest to match against
	 * @return a matched transaction or null if not found
	 */
	private Transaction match(Interest interest) {
		byte[] nonce = interest.getName().get(-1).getValue().getImmutableArray();
		return indexedPackets.get(nonce);
	}

	/**
	 * Handle data packets returned from the server; this callback will attempt
	 * to match the secure nonce to the packet index and update the
	 * transaction's state
	 */
	private class OnCompletedTransaction implements OnData {

		@Override
		public void onData(Interest interest, Data data) {
			Transaction matchingTransaction = match(interest);

			if (matchingTransaction != null) {
				matchingTransaction.data = data;
				matchingTransaction.state = Transaction.State.RECEIVED;
			} else {
				logger.log(Level.WARNING, "Data with unmatched nonce returned: {0}", data.getName().toUri());
			}

			synchronized (mutex) {
				mutex.notifyAll();
			}
		}
	}

	/**
	 * Handle failures to retrieve data; TODO eventually this method may want to
	 * retry failed interests
	 */
	private class OnFailedTransaction implements OnTimeout {

		@Override
		public void onTimeout(Interest interest) {
			logger.log(Level.SEVERE, "Interest timed out: {0}", interest.toUri());

			Transaction matchingTransaction = match(interest);
			if (matchingTransaction != null) {
				matchingTransaction.state = Transaction.State.FAILED;
			}

			synchronized (mutex) {
				mutex.notifyAll();
			}
		}
	}
}
