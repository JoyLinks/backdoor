/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.joyzl.codec.BigEndianDataOutput;

/**
 * FileChannel InputStream
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年7月30日
 */
final class FileOutputStream extends OutputStream implements BigEndianDataOutput {

	private final FileChannel channel;
	private final ByteBuffer buffer;

	public FileOutputStream(File file, boolean append) throws IOException {
		this(file.toPath(), 65536, append);
	}

	public FileOutputStream(Path file, boolean append) throws IOException {
		this(file, 65536, append);
	}

	public FileOutputStream(File file, int bufferSize, boolean append) throws IOException {
		this(file.toPath(), bufferSize, append);
	}

	public FileOutputStream(Path file, int bufferSize, boolean append) throws IOException {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("缓冲区数量无效");
		}
		if (append) {
			channel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} else {
			channel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		buffer = ByteBuffer.allocateDirect(bufferSize);
	}

	@Override
	public void writeByte(int b) throws IOException {
		write(b);
	}

	@Override
	public void write(int b) throws IOException {
		if (buffer.hasRemaining()) {
			buffer.put((byte) b);
		} else {
			flush();
			buffer.put((byte) b);
		}
	}

	@Override
	public void write(byte bytes[], int off, int len) throws IOException {
		if (len == 0) {
			return;
		}
		if (bytes == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || len > bytes.length - off) {
			throw new IndexOutOfBoundsException();
		}
		if (len <= buffer.remaining()) {
			buffer.put(bytes, off, len);
		} else {
			flush();
			if (len < buffer.capacity()) {
				buffer.put(bytes, off, len);
			} else {
				do {
					len -= channel.write(ByteBuffer.wrap(bytes, off, len));
				} while (len > 0);
			}
		}
	}

	public void write(ByteBuffer b) throws IOException {
		if (b.remaining() == 0) {
			return;
		}
		if (b.remaining() <= buffer.remaining()) {
			buffer.put(b);
		} else {
			flush();
			if (b.remaining() < buffer.capacity()) {
				buffer.put(b);
			} else {
				do {
					channel.write(b);
				} while (b.hasRemaining());
			}
		}
	}

	@Override
	public void flush() throws IOException {
		buffer.flip();
		do {
			if (channel.write(buffer) <= 0) {
				break;
			}
		} while (buffer.hasRemaining());
		buffer.clear();
	}

	@Override
	public void close() throws IOException {
		if (channel.isOpen()) {
			flush();
			channel.force(true);
			channel.close();
		}
	}

	public ByteBuffer buffer() {
		return buffer;
	}
}