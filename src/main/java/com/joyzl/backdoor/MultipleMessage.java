/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

/**
 * UDP 终端管理命令
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2026年8月28日
 */
public interface MultipleMessage {

	/** 返回发送实例，如果命令实例可发送给多个终端则返回相同同实例，否则应返回新实例 */
	MultipleMessage one();
	// Multiple
}