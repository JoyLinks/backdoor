package com.joyzl.backdoor;

import com.joyzl.network.chain.UDPSlave;

/**
 * 多发过程进度
 * 
 * @author ZhangXi 2026年9月1日
 */
public interface MultipleProgress {

	void update(UDPSlave slave);

	void complete();

}