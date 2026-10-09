package com.joyzl.backdoor;

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
	private final boolean exit;

	/**
	 * 执行脚本
	 * 
	 * @param command 命令文件
	 * @param exit 是否退出当前程序，如果执行更新则需要退出当前程序，否则无法覆盖文件
	 */
	public ScriptCommand(Path dir, String command, boolean exit) {
		this.command = command;
		this.exit = exit;
		this.dir = dir;
	}

	public ScriptCommand(String command, boolean exit) {
		this.command = command;
		this.exit = exit;
		this.dir = null;
	}

	@Override
	public MultipleMessage one() {
		return this;
	}

	@Override
	public void run() {
		try {
			// 工作目录，当前程序运行目录
			final Path work = dir.toAbsolutePath().getParent();
			Logger.info("BACKDOOR DIR ", work);

			// 启动的脚本进程应脱离Java进程
			// 否则Java进程退出后会连带脚本进程强制被结束
			final ProcessBuilder builder = new ProcessBuilder();
			builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
			// Java进程在更新时会被停止，指定输出被中断
			// builder.redirectOutput(path.resolve("process.log").toFile());
			// builder.redirectError(path.resolve("process.log").toFile());
			builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			builder.redirectError(ProcessBuilder.Redirect.DISCARD);
			builder.directory(work.toFile());

			final String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("windows")) {
				String cmd = command.toLowerCase();
				if (cmd.endsWith(".ps1")) {
					/*-
					 * Windows
					 * powershell.exe -Command "Start-Process powershell -ArgumentList '-ExecutionPolicy Bypass -Command "& \"stpfiles\\update.ps1\" >> script.log 2>&1"' -WindowStyle Hidden
					 */
					cmd = "Start-Process powershell -ArgumentList '-ExecutionPolicy Bypass -Command \"& \\\"" + command + "\\\" >> script.log 2>&1\"' -WindowStyle Hidden";
					builder.command("powershell.exe", "-Command", cmd);
				} else if (cmd.endsWith(".cmd") || cmd.endsWith(".bat")) {
					/*-
					 * Windows
					 * cmd.exe /c start /b "" "stpfiles\\update.ps1" >> script.log 2>&1
					 */
					builder.command("cmd.exe", "/c", "start /b \"\" \"" + command + "\" >> script.log 2>&1");
				} else {
					Logger.info("BACKDOOR ", "无效脚本 ", command);
					return;
				}
			} else {
				String cmd = command.toLowerCase();
				if (cmd.endsWith(".sh")) {
					/*-
					 * Linux
					 * bash -c nohup bash 'stpfiles/update.sh' >> script.log 2>&1 &
					 */
					builder.command("bash", "-c", "nohup bash '" + command + "' >> script.log 2>&1 &");
				} else {
					Logger.info("BACKDOOR ", "无效脚本 ", command);
					return;
				}
			}

			Logger.info(String.join(" ", builder.command()));
			builder.start();
		} catch (Exception e) {
			Logger.error(e);
		} finally {
			if (exit) {
				System.exit(0);
			}
		}
	}

	public String getCommand() {
		return command;
	}

	public boolean getExit() {
		return exit;
	}
}