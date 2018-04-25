/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.sqs.javamessaging;

import java.io.OutputStream;

/**
 * This class is used for checking the size of a string without copying the
 * whole string into memory and converting it to bytes array. Compared to
 * String.getBytes().length, it is more efficient and reliable for large
 * strings.
 */
class CountingOutputStream extends OutputStream {
	private long totalSize;

	@Override
	public void write(int b) {
		++totalSize;
	}

	@Override
	public void write(byte[] b) {
		totalSize += b.length;
	}

	@Override
	public void write(byte[] b, int offset, int len) {

		totalSize += len;
	}

	public long getTotalSize() {
		return totalSize;
	}
}
