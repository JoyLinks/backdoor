/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

import com.joyzl.network.chain.UDPSlave;

/**
 * 消息收到通知
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年11月21日
 */
public interface ReceivedCallback<T> {

	void received(UDPSlave slave, T result);
}