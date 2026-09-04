package com.joyzl.backdoor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import com.joyzl.network.buffer.DataBuffer;
import com.joyzl.network.chain.ChainGenericsHandler;
import com.joyzl.network.chain.UDPServer;
import com.joyzl.network.chain.UDPSlave;
import com.joyzl.network.codec.KeyValueCoder;

/**
 * 维护端
 * 
 * @author ZhangXi 2026年9月1日
 */
public class Maintainer {

	/** 监听接收消息 */
	private final UDPServer server;

	private ReceivedCallback<String> onMessage;
	private ReceivedCallback<Parameters> onSetting;
	private ReceivedCallback<PushFilePackage> onPackage;

	public Maintainer() throws IOException {
		// 无须指定端口，随机端口会通过连接被终端获得
		server = new UDPServer(new Handler(), null, 0);
		server.receive();
	}

	public void setOnMessage(ReceivedCallback<String> handler) {
		onMessage = handler;
	}

	public void setOnSetting(ReceivedCallback<Parameters> handler) {
		onSetting = handler;
	}

	public void setOnPackage(ReceivedCallback<PushFilePackage> handler) {
		onPackage = handler;
	}

	/**
	 * 指定广播地址和端口发布广播，收到广播的客户端将发回 Parameters 消息
	 * 
	 * @param host 广播地址 255.255.255.0
	 * @param port 客户端监听端口
	 */
	public void broadcast(String host, int port) {
		// 构建广播地址和从链路
		final InetSocketAddress address = new InetSocketAddress(host, port);
		// 发送广播等待终端回复
		final UDPSlave slave = new UDPSlave(server, address);
		slave.send(new Parameters());
	}

	public MultipleSends scans(InetSocketAddress address, MultipleProgress handler) {
		final MultipleSends scanner = new MultipleSends(new Parameters(), handler);
		final byte[] b4 = address.getAddress().getAddress();
		final int port = address.getPort();
		try {
			for (int i = 1; i < 254; i++) {
				b4[3] = (byte) i;
				scanner.slaves().add(new UDPSlave(server, new InetSocketAddress(InetAddress.getByAddress(b4), port)));
			}
		} catch (UnknownHostException e) {
			// 这个异常不会出现
			throw new RuntimeException(e);
		}
		scanner.start();
		return scanner;
	}

	public MultipleSends sends(Collection<UDPSlave> slaves, MultipleMessage message, MultipleProgress handler) {
		final MultipleSends scanner = new MultipleSends(message, handler);
		scanner.slaves().addAll(slaves);
		scanner.start();
		return scanner;
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
				// 响应普通文本
				return new String(buffer.readASCIIs(buffer.readable()));
			} else if (value == 0x05) {
				// 响应参数发回
				final Parameters parameters = new Parameters();
				KeyValueCoder.decode(parameters, buffer);
				return parameters;
			} else if (value == 0x02) {
				// 文件块请求
				final PushFilePackage filePackage = slave.getContext(PushFilePackage.class);
				if (filePackage != null) {
					int offset = buffer.readVarint();
					int length = buffer.readVarint();
					filePackage.nextBlock(offset, length);
					return filePackage;
				} else {
					System.err.println("MANAGER 报文缺失上下文");
					buffer.clear();
					return null;
				}
			} else {
				System.err.println("MANAGER 报文无法识别");
				buffer.clear();
				return null;
			}
		}

		@Override
		public void received(UDPSlave slave, Object message) throws Exception {
			if (message != null) {
				if (message instanceof String text) {
					if (onMessage != null) {
						onMessage.received(slave, text);
					}
				} else if (message instanceof Parameters parameters) {
					if (onSetting != null) {
						onSetting.received(slave, parameters);
					}
				} else if (message instanceof PushFilePackage filePackage) {
					if (onPackage != null) {
						onPackage.received(slave, filePackage);
					}
					if (filePackage.sendBlock()) {
						slave.send(filePackage);
					} else {
						slave.removeContext(PushFilePackage.class);
						filePackage.close();
					}
				}
			}
		}

		@Override
		public DataBuffer encode(UDPSlave slave, Object message) throws Exception {
			final DataBuffer buffer = DataBuffer.instance();
			if (message instanceof String text) {
				// 发送文本
				buffer.writeASCIIs(text);
				buffer.writeASCII('\n');
			} else if (message instanceof Parameters parameters) {
				// 发送参数集
				KeyValueCoder.encode(parameters, buffer);
				buffer.writeByte(0x05);
			} else if (message instanceof PushFilePackage filePackage) {
				// 发送文件包
				if (filePackage.sendBlock()) {
					filePackage.readBlock(buffer);
					buffer.writeByte(0x03);
				} else {
					buffer.writeString(filePackage.name());
					buffer.writeVarint(filePackage.total());
					buffer.writeByte(0x01);
				}
			} else if (message instanceof ScriptCommand command) {
				// 发送执行命令
				buffer.writeString(command.getCommand());
				buffer.writeString(command.getInput());
				buffer.writeBoolean(command.getExit());
				buffer.writeByte(0x06);
			} else {
				throw new UnsupportedOperationException(message.getClass().toString());
			}
			return buffer;
		}

		@Override
		public void sent(UDPSlave slave, Object message) throws Exception {
			if (message instanceof PushFilePackage filePackage) {
				slave.setContext(filePackage);
			}
		}

		@Override
		public void disconnected(UDPSlave slave) throws Exception {
			// UDP 不使用
		}

		@Override
		public void beat(UDPSlave slave) throws Exception {
			// UDP 不使用
		}

		@Override
		public void error(UDPSlave slave, Throwable e) {
			e.printStackTrace();
		}
	}

	public void close() {
		server.close();
	}
}