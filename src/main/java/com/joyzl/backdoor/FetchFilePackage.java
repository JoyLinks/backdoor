package com.joyzl.backdoor;

import static com.joyzl.network.Utility.noEmpty;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.joyzl.logger.Logger;
import com.joyzl.network.Executor;
import com.joyzl.network.buffer.DataBuffer;
import com.joyzl.network.chain.UDPSlave;

/**
 * 文件包接收
 * 
 * @author ZhangXi 2026年9月1日
 */
public final class FetchFilePackage implements Runnable, Closeable {

	// 远程主动发送包名称和大小，创建此对象
	// 本地逐块数据获取，直至全部下载
	// 只能是压缩包文件，解压既校验

	private final Path dir;
	/** 文件名，不含路径 */
	private final String name;
	/** 文件总大小 */
	private final int total;
	/** 已接收字节 */
	private int size;

	/**
	 * 创建文件包上下文
	 * 
	 * @param slave 推送文件包的对端链路，将通过此链路拉取文件包
	 * @param dir 文件包保存的目录
	 * @param name 文件包名称（含扩展名）
	 * @param total 文件包字节总数
	 */
	FetchFilePackage(UDPSlave slave, Path dir, String name, int total) {
		this.slave = slave;
		this.total = total;
		this.name = name;
		this.dir = dir;
		size = 0;
	}

	@Override
	public String toString() {
		return "FilePackage " + name + ',' + size + '/' + total;
	}

	private FileOutputStream output;
	private ScheduledFuture<?> future;
	private final UDPSlave slave;
	private long timestamp;

	@Override
	public void close() throws IOException {
		if (future != null) {
			future.cancel(false);
		}
		if (output != null) {
			output.close();
		}
	}

	@Override
	public void run() {
		// 执行超时检查
		// 6秒超时，30分钟放弃
		final long time = System.currentTimeMillis() - timestamp;
		try {
			if (time > 6000) {
				if (time < 1000 * 60 * 30) {
					if (nextBlock()) {
						// 重试请求
						slave.send(this);
					} else {
						// 解压文件
						close();
						unpack();
					}
				} else {
					// 放弃
					close();
				}
			}
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	/** 解压文件包 */
	public void unpack() throws IOException, InterruptedException {
		final ProcessBuilder builder = new ProcessBuilder();
		builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.redirectError(ProcessBuilder.Redirect.DISCARD);
		builder.directory(dir.toFile());

		final String file = name.toLowerCase();
		if (file.endsWith(".zip")) {
			// 通过 Java jar 工具解压，避免操作系统没有解压工具
			// 设置环境变量，否则无法定位 jar.exe
			// Java jar 只能解压 zip 压缩包
			String text = System.getProperty("java.home");
			builder.environment().put("JAVA_HOME", text);
			text = builder.environment().get("PATH");
			if (noEmpty(text)) {
				text = "%JAVA_HOME%\\bin;" + text;
			} else {
				text = "%JAVA_HOME%\\bin;";
			}
			builder.environment().put("PATH", text);
			builder.command("jar", "xf", name);
		} else if (file.endsWith(".tar.gz")) {
			// Linux 必须使用 tar.gz 否则文件将丢失属性
			// 如果可执行文件丢失属性，则无法被运行
			// 通过 tar 工具解压 tar.gz
			builder.command("tar", "-xzf", name);
		} else {
			Logger.info("BACKDOOR 不支持的压缩包 ", name);
			return;
		}

		final Process process = builder.start();
		final int exit = process.waitFor();

		Logger.info("BACKDOOR 解压 ", name, " EXIT:", exit);
		Files.delete(dir.resolve(name));
	}

	/** 增量保存数据块 */
	void save(DataBuffer buffer) throws IOException {
		if (output == null) {
			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}
			output = new FileOutputStream(dir.resolve(name), false);
		}
		timestamp = System.currentTimeMillis();
		size += buffer.readable();
		buffer.read(output);
	}

	/** 是否还有下一块数据 */
	public boolean nextBlock() throws IOException {
		if (size < total) {
			if (future == null) {
				future = Executor.scheduleAtFixedRate(this, 2, 2, TimeUnit.SECONDS);
			}
			timestamp = System.currentTimeMillis();
			return true;
		} else {
			if (output != null) {
				output.close();
			}
			return false;
		}
	}

	/** 下一块数据长度 */
	public int nextLength() {
		if (size < total) {
			// 当前UDP链路仅提供1024缓存区大小
			return total - size > 1023 ? 1023 : total - size;
		}
		return 0;
	}

	/** 下一块数据偏移 */
	public int nextOffset() {
		return size;
	}
}