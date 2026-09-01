package com.joyzl.backdoor;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.joyzl.logger.Logger;

/**
 * 执行命令行脚本
 * 
 * @author ZhangXi 2026年9月1日
 */
public class ScriptCommand implements MultipleMessage, Runnable {

	private final Path dir;
	private final String command;
	private final String input;

	/**
	 * 执行脚本
	 * 
	 * @param command 脚本文件
	 * @param input 标准输入
	 */
	public ScriptCommand(Path dir, String command, String input) {
		this.command = command;
		this.input = input;
		this.dir = dir;
	}

	public ScriptCommand(String command, String input) {
		this.command = command;
		this.input = input;
		this.dir = null;
	}

	@Override
	public MultipleMessage one() {
		return this;
	}

	@Override
	public void run() {
		try {
			final Path path = dir.toAbsolutePath().getParent();
			Logger.info("BACKDOOR CMD ", command);
			Logger.info("BACKDOOR DIR ", path);

			final ProcessBuilder builder = new ProcessBuilder();
			builder.redirectOutput(path.resolve("process.log").toFile());
			builder.redirectError(path.resolve("process.log").toFile());
			builder.directory(path.toFile());

			String text = System.getProperty("java.home");
			builder.environment().put("JAVA_HOME", text);

			text = System.getProperty("os.name").toLowerCase();
			if (text.contains("windows")) {
				/*-
				 * Windows
				 * powershell.exe -ExecutionPolicy Bypass -WindowStyle Hidden -File command
				 * cmd.exe /c start /b command
				 */
				text = command.toLowerCase();
				if (text.endsWith(".ps1")) {
					builder.command("powershell.exe", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", command);
				} else if (text.endsWith(".cmd") || text.endsWith(".bat")) {
					builder.command("cmd.exe", "/c", "start", "/b", command);
				} else {
					Logger.info("BACKDOOR ", "无效脚本 ", command);
					return;
				}
			} else {
				/*-
				 * Linux
				 * bash stpfiles/update.sh
				 */
				text = command.toLowerCase();
				if (text.endsWith(".sh")) {
					builder.command("bash", command);
				} else {
					Logger.info("BACKDOOR ", "无效脚本 ", command);
					return;
				}
			}

			final Process process = builder.start();
			if (input != null && input.length() > 0) {
				final OutputStream output = process.getOutputStream();
				output.write(input.getBytes(StandardCharsets.UTF_8));
				output.write('\n');
				output.flush();
				// 等待读取
				Thread.sleep(1000);
				output.close();
			}
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public String getCommand() {
		return command;
	}

	public String getInput() {
		return input;
	}
}