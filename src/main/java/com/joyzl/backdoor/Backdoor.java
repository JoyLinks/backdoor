/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import com.joyzl.logger.Logger;
import com.joyzl.network.Executor;
import com.joyzl.network.Point;
import com.joyzl.network.buffer.DataBuffer;
import com.joyzl.network.chain.ChainGenericsHandler;
import com.joyzl.network.chain.UDPServer;
import com.joyzl.network.chain.UDPSlave;
import com.joyzl.network.codec.KeyValueCoder;

/**
 * 发现与远程维护端
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2026年1月7日
 */
public final class Backdoor {

	private UDPServer server;
	private ReceivedCallback<String> onMessage;
	private ReceivedCallback<Parameters> onSetting;
	private ReceivedCallback<FetchFilePackage> onPackage;
	private ReceivedCallback<ScriptCommand> onCommand;
	// 接收文件存储目录
	private Path dir = Path.of("stpfiles");

	public void reset(String point) throws IOException {
		if (dir != null) {
			this.dir = Path.of("stpfiles");
		}
		final int port = Point.getPort(point);
		final String host = Point.getHost(point);
		if (server != null) {
			if (server.active()) {
				final InetSocketAddress address;
				if (host == null || host.length() == 0) {
					address = new InetSocketAddress(port);
				} else {
					address = new InetSocketAddress(host, port);
				}
				if (address.equals(server.getLocalAddress())) {
					return;
				}
			}
			close();
		}
		server = new UDPServer(new Handler(), host, port);
		server.receive();
	}

	public void reset(String dir, String point) throws IOException {
		if (dir == null) {
			this.dir = Path.of("stpfiles");
		} else {
			this.dir = Path.of(dir);
		}
		reset(point);
	}

	public void close() {
		onMessage = null;
		onSetting = null;
		if (server != null) {
			server.close();
			server = null;
		}
	}

	public void setOnMessage(ReceivedCallback<String> handler) {
		onMessage = handler;
	}

	public void setOnSetting(ReceivedCallback<Parameters> handler) {
		onSetting = handler;
	}

	public void setOnPackage(ReceivedCallback<FetchFilePackage> handler) {
		onPackage = handler;
	}

	public void setOnCommand(ReceivedCallback<ScriptCommand> handler) {
		onCommand = handler;
	}

	private class Handler implements ChainGenericsHandler<UDPSlave, Object> {

		@Override
		public void connected(UDPSlave slave) throws Exception {
			// UDP 不使用
		}

		@Override
		public Object decode(UDPSlave slave, DataBuffer buffer) throws Exception {
			final byte value = buffer.backByte();
			if (value == 0x0D || value == 0x0A) {
				// 普通消息 ASCII\r ASCII\n
				return new String(buffer.readASCIIs(buffer.readable()));
			} else if (value == 0x05) {
				// 设置或读取参数
				final Parameters parameters = new Parameters();
				KeyValueCoder.decode(parameters, buffer);
				return parameters;
			} else if (value == 0x06) {
				// 远程执行脚本文件
				final String command = buffer.readString();
				final String inputs = buffer.readString();
				return new ScriptCommand(dir, command, inputs);
			} else if (value == 0x01) {
				// 通知文件包下载
				final String name = buffer.readString();
				final int total = buffer.readVarint();
				return new FetchFilePackage(slave, dir, name, total);
			} else if (value == 0x03) {
				// 文件内容数据块
				final FetchFilePackage filePackage = slave.getContext(FetchFilePackage.class);
				if (filePackage != null) {
					filePackage.save(buffer);
					return filePackage;
				} else {
					Logger.error("BACKDOOR 报文缺失上下文");
					buffer.clear();
					return null;
				}
			} else {
				Logger.error("BACKDOOR 报文无法识别");
				buffer.clear();
				return null;
			}
		}

		@Override
		public void received(UDPSlave slave, Object message) throws Exception {
			if (message != null) {
				// MESSAGE
				if (message instanceof String text) {
					if (onMessage != null) {
						onMessage.received(slave, text);
					}
					slave.send("OK");
				} else
				// SETTING
				if (message instanceof Parameters parameters) {
					if (onSetting != null) {
						onSetting.received(slave, parameters);
					}
					slave.send(parameters);
				} else
				// FILE PACKAGE
				if (message instanceof FetchFilePackage filePackage) {
					if (filePackage.nextBlock()) {
						slave.setContext(filePackage);
						slave.send(filePackage);
					} else {
						filePackage.close();
						slave.removeContext(FetchFilePackage.class);
						if (onPackage != null) {
							onPackage.received(slave, filePackage);
						}
					}
				} else
				// COMMAND
				if (message instanceof ScriptCommand command) {
					if (onCommand != null) {
						onCommand.received(slave, command);
					}
					Executor.submit(command);
				}
			}
		}

		@Override
		public DataBuffer encode(UDPSlave slave, Object message) throws Exception {
			final DataBuffer buffer = DataBuffer.instance();
			if (message instanceof String) {
				// 发送扫码响应（可能会被对方忽略）
				buffer.writeASCIIs(message.toString());
				buffer.writeByte(0x0A);
			} else if (message instanceof Parameters parameters) {
				// 发送设置参数响应
				KeyValueCoder.encode(parameters, buffer);
				buffer.writeByte(0x05);
			} else if (message instanceof FetchFilePackage filePackage) {
				// 发送文件块请求
				buffer.writeVarint(filePackage.nextOffset());
				buffer.writeVarint(filePackage.nextLength());
				buffer.writeByte(0x02);
			} else {
				throw new UnsupportedOperationException(message.getClass().toString());
			}
			return buffer;
		}

		@Override
		public void sent(UDPSlave slave, Object message) throws Exception {
		}

		@Override
		public void disconnected(UDPSlave chain) throws Exception {
			// UDP不使用
		}

		@Override
		public void beat(UDPSlave slave) throws Exception {
			// UDP不使用
		}

		@Override
		public void error(UDPSlave chain, Throwable e) {
			Logger.error(e);
		}
	}
}