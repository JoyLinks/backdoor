/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.backdoor;

/**
 * 回调函数
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年11月21日
 */
public interface Callback<T> {

	void received(T result);
}