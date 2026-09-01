/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.joyzl.codec.BigEndianDataInput;

/**
 * FileChannel InputStream
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年7月30日
 */
final class FileInputStream extends InputStream implements BigEndianDataInput {

	private final FileChannel channel;
	private final ByteBuffer buffer;
	private long position = 0;

	public FileInputStream(File file) throws IOException {
		this(file.toPath(), 65536);
	}

	public FileInputStream(Path file) throws IOException {
		this(file, 65536);
	}

	public FileInputStream(File file, int bufferSize) throws IOException {
		this(file.toPath(), bufferSize);
	}

	public FileInputStream(Path file, int bufferSize) throws IOException {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("缓冲区数量无效");
		}

		channel = FileChannel.open(file, StandardOpenOption.READ);
		buffer = ByteBuffer.allocateDirect(bufferSize);
		buffer.flip();
	}

	@Override
	public byte readByte() throws IOException {
		if (buffer.hasRemaining()) {
			return buffer.get();
		} else {
			buffer.clear();
			int size = channel.read(buffer, position);
			if (size <= 0) {
				return -1;
			}
			position += size;
			buffer.flip();
			return buffer.get();
		}
	}

	@Override
	public int read() throws IOException {
		if (buffer.hasRemaining()) {
			return buffer.get() & 0xFF;
		} else {
			buffer.clear();
			int size = channel.read(buffer, position);
			if (size <= 0) {
				return -1;
			}
			position += size;
			buffer.flip();
			return buffer.get() & 0xFF;
		}
	}

	@Override
	public int read(byte bytes[], int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		if (bytes == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || len > bytes.length - off) {
			throw new IndexOutOfBoundsException();
		}
		if (len <= buffer.remaining()) {
			buffer.get(bytes, off, len);
			return len;
		} else {
			int size = buffer.remaining();
			if (size > 0) {
				buffer.get(bytes, off, size);
				off += size;
				len -= size;
			} else if (position >= channel.size()) {
				return -1;
			}

			int s = channel.read(ByteBuffer.wrap(bytes, off, len), position);
			if (s > 0) {
				position += s;
				size += s;
			}
			return size;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		if (n <= 0) {
			return 0;
		}
		if (n <= buffer.remaining()) {
			buffer.position((int) (buffer.position() + n));
			return n;
		} else {
			long s = n - buffer.remaining();
			buffer.position(buffer.limit());

			if (s <= channel.size() - position) {
				channel.position(position += s);
				return n;
			} else {
				s -= channel.size() - position;
				channel.position(position += channel.size());
				return n - s;
			}
		}
	}

	@Override
	public int available() throws IOException {
		long size = buffer.remaining() + channel.size() - position;
		if (size > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int) size;
		}
		// return buffer.remaining() + (int) Math.min(Integer.MAX_VALUE,
		// channel.size() - (position + buffer.position()));
	}

	@Override
	public void close() throws IOException {
		if (channel.isOpen()) {
			channel.close();
		}
	}

	public void resetPosition() throws IOException {
		buffer.clear().flip();
		channel.position(0);
		position = 0;
	}

	public long position() {
		return position - buffer.remaining();
	}
}