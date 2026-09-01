package com.joyzl.backdoor;

import java.util.HashMap;

// 避免泛型警告
public class Parameters extends HashMap<String, String> implements MultipleMessage {
	private static final long serialVersionUID = 1L;

	@Override
	public MultipleMessage one() {
		return this;
	}
}