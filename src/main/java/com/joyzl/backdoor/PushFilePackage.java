/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import com.joyzl.network.buffer.DataBuffer;

/**
 * 文件包发送
 * 
 * @author ZhangXi 2026年9月1日
 */
public class PushFilePackage implements MultipleMessage, Closeable {

	/** 文件包 */
	private final File file;
	/** 文件总大小 */
	private final int total;

	public PushFilePackage(File file) {
		total = (int) file.length();
		this.file = file;
	}

	@Override
	public MultipleMessage one() {
		return new PushFilePackage(file);
	}

	private FileInputStream input;
	private int offset, length;

	@Override
	public void close() throws IOException {
		if (input != null) {
			input.close();
			input = null;
		}
	}

	public void nextBlock(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	public boolean sendBlock() {
		return length > 0;
	}

	void readBlock(DataBuffer buffer) throws IOException {
		if (input == null) {
			input = new FileInputStream(file);
		}
		if (input.position() != offset) {
			input.resetPosition();
			input.skip(offset);
		}
		buffer.write(input, length);
	}

	/** 0~1000 ‰ */
	public int percentage() {
		return (int) ((offset + length) * 1D / total * 1000);
	}

	public String name() {
		return file.getName();
	}

	public int total() {
		return total;
	}
}