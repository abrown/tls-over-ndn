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

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Describes a Interest-Data transaction initiated by an NDN client
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Transaction {

	public State state = State.NOT_SENT;
	public final Interest interest;
	public Data data;

	/**
	 * Build a transaction from an interest
	 *
	 * @param interest
	 */
	public Transaction(Interest interest) {
		this.interest = interest;
	}

	/**
	 * The possible states of a transaction
	 */
	public enum State {

		NOT_SENT, IN_FLIGHT, FAILED, RECEIVED, CONSUMED
	}
}
