package com.zjhcsoft.hetty;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import com.zjhcsoft.hetty.exception.ApplicationException;

/**
 * 接收 read,write事件
 * 
 * @date 2015-2-2 上午11:03:05
 * @Title: SubReactor.java
 * @author lff
 */
public class SubReactor {
	private final int bossId;
	private final int id;
	private final Executor executor;
	private boolean started;
	volatile Selector selector;
	private final Object startStopLock = new Object();

	SubReactor(int bossId, int id, Executor executor) {
		this.bossId = bossId;
		this.id = id;
		this.executor = executor;
	}

	void register(SocketChannel client) {

		Selector selector;

		synchronized (startStopLock) {

			if (!started) {
				// Open a selector if this worker didn't start yet.
				try {
					this.selector = selector = Selector.open();
				} catch (Throwable t) {
					throw new ApplicationException(
							"Failed to create a selector.", t);
				}
				started = true;

				executor.execute(new WorkTask(this.selector));
			} else {
				// Use the existing selector if this worker has been started.
				selector = this.selector;
			}
		}
		// 注册监听
		try {
			client.configureBlocking(false);
			client.register(selector, SelectionKey.OP_READ);
		} catch (IOException e) {
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			throw new ApplicationException("注册事件异常" + e.getMessage());
		}
	}
}
