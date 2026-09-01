package com.joyzl.backdoor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

import com.joyzl.network.Executor;
import com.joyzl.network.chain.UDPSlave;

/**
 * 命令多发
 * 
 * @author ZhangXi 2026年9月1日
 */
public class MultipleSends implements Runnable {

	private final List<UDPSlave> slaves = new ArrayList<>();
	private final MultipleMessage message;
	private final MultipleProgress progress;

	public MultipleSends(MultipleMessage message, MultipleProgress progress) {
		this.progress = progress;
		this.message = message;
	}

	private Future<?> future;

	void start() {
		if (future == null) {
			future = Executor.submit(this);
		}
	}

	public void stop() {
		if (future != null) {
			future.cancel(false);
			future = null;
		}
	}

	@Override
	public void run() {
		final Random rand = new Random();
		Future<?> future;
		try {
			for (UDPSlave slave : slaves) {
				if (progress != null) {
					progress.update(slave);
				}

				slave.send(message);

				future = this.future;
				if (future == null || future.isCancelled()) {
					return;
				} else {
					// 大量群发时减少网络压力
					Thread.sleep(100 + rand.nextInt(2000));
				}
			}

			if (progress != null) {
				progress.complete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<UDPSlave> slaves() {
		return slaves;
	}
}