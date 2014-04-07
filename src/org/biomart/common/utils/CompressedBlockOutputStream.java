package org.biomart.common.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Output stream that compresses data. A compressed block is generated and
 * transmitted once a given number of bytes have been written, or when the flush
 * method is invoked.
 * 
 * Copyright 2005 - Philip Isenhour - http://javatechniques.com/
 * 
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim
 * that you wrote the original software. If you use this software in a product,
 * an acknowledgment in the product documentation would be appreciated but is
 * not required.
 * 
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 
 * 3. This notice may not be removed or altered from any source distribution.
 * 
 * $Id: CompressedBlockOutputStream.java,v 1.1 2007/11/13 11:40:04 rh4 Exp $
 */
public class CompressedBlockOutputStream extends FilterOutputStream {
	/**
	 * Buffer for input data
	 */
	private byte[] inBuf = null;

	/**
	 * Buffer for compressed data to be written
	 */
	private byte[] outBuf = null;

	/**
	 * Number of bytes in the buffer
	 */
	private int len = 0;

	/**
	 * Deflater for compressing data
	 */
	private Deflater deflater = null;

	/**
	 * Constructs a CompressedBlockOutputStream that writes to the given
	 * underlying output stream 'os' and sends a compressed block once 'size'
	 * byte have been written. The default compression strategy and level are
	 * used.
	 * 
	 * @param os
	 *            the outputstream to write to.
	 * @param size
	 *            the buffer size to use.
	 * @throws IOException
	 *             if anything went wrong.
	 */
	public CompressedBlockOutputStream(final OutputStream os, final int size)
			throws IOException {
		this(os, size, Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
	}

	/**
	 * Constructs a CompressedBlockOutputStream that writes to the given
	 * underlying output stream 'os' and sends a compressed block once 'size'
	 * byte have been written. The compression level and strategy should be
	 * specified using the constants defined in {#link #Deflator}.
	 * 
	 * @param os
	 *            the outputstream to write to.
	 * @param size
	 *            the buffer size to use.
	 * @param level
	 *            the compression level.
	 * @param strategy
	 *            the compression strategy.
	 * @throws IOException
	 *             if anything went wrong.
	 */
	public CompressedBlockOutputStream(final OutputStream os, final int size,
			final int level, final int strategy) throws IOException {
		super(os);
		this.inBuf = new byte[size];
		this.outBuf = new byte[size + 64];
		this.deflater = new Deflater(level);
		this.deflater.setStrategy(strategy);
	}

	/**
	 * Compresses any existing data and sends it.
	 * 
	 * @throws IOException
	 *             if anything went wrong.
	 */
	protected void compressAndSend() throws IOException {
		if (this.len > 0) {
			this.deflater.setInput(this.inBuf, 0, this.len);
			this.deflater.finish();
			final int size = this.deflater.deflate(this.outBuf);

			// Write the size of the compressed data, followed
			// by the size of the uncompressed data
			this.out.write(size >> 24 & 0xFF);
			this.out.write(size >> 16 & 0xFF);
			this.out.write(size >> 8 & 0xFF);
			this.out.write(size >> 0 & 0xFF);

			this.out.write(this.len >> 24 & 0xFF);
			this.out.write(this.len >> 16 & 0xFF);
			this.out.write(this.len >> 8 & 0xFF);
			this.out.write(this.len >> 0 & 0xFF);

			this.out.write(this.outBuf, 0, size);
			this.out.flush();

			this.len = 0;
			this.deflater.reset();
		}
	}

	public void write(final int b) throws IOException {
		this.inBuf[this.len++] = (byte) b;
		if (this.len == this.inBuf.length)
			this.compressAndSend();
	}

	public void write(final byte[] b, int boff, int blen) throws IOException {
		while (this.len + blen > this.inBuf.length) {
			final int toCopy = this.inBuf.length - this.len;
			System.arraycopy(b, boff, this.inBuf, this.len, toCopy);
			this.len += toCopy;
			this.compressAndSend();
			boff += toCopy;
			blen -= toCopy;
		}
		System.arraycopy(b, boff, this.inBuf, this.len, blen);
		this.len += blen;
	}

	public void flush() throws IOException {
		this.compressAndSend();
		this.out.flush();
	}

	public void close() throws IOException {
		this.compressAndSend();
		this.out.close();
	}
}