package com.zjhcsoft.hetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zjhcsoft.hetty.constant.Constant;
import com.zjhcsoft.hetty.exception.ApplicationException;

/**
 * 接收连接
 * 
 * @date 2015-2-2 上午09:37:40
 * @Title: MainReactor.java
 * @author lff
 */
public class MainReactor {

	private Logger logger = LoggerFactory.getLogger(getClass());

	protected Selector selector;

	private static final AtomicInteger nextId = new AtomicInteger();

	private final int id = nextId.incrementAndGet();
	private final SubReactor[] workers;
	private final AtomicInteger workerIndex = new AtomicInteger();
	public int workerCount = Constant.DEFAULTWORKCOUNT;

	public MainReactor(int port) {
		// 绑定端口
		ServerSocketChannel server = null;

		try {

			server = ServerSocketChannel.open();

			selector = Selector.open();
			// 读取缓冲区
//			server.socket().setReceiveBufferSize(1024);
			server.socket().bind(new InetSocketAddress(port));

			server.configureBlocking(false);

			server.register(selector, SelectionKey.OP_ACCEPT);

		} catch (IOException e) {
			try {
				if (server != null) {
					server.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			try {
				if (selector != null) {
					selector.close();
				}
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			throw new ApplicationException("binding port:" + port + " error"
					+ e.getMessage());
		}
		logger.info("binding port " + port + " sucess ");
		// work队列
		workers = new SubReactor[workerCount];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new SubReactor(id, i + 1, Executors
					.newCachedThreadPool());
		}
	}

	public void listen() {
		try {
			for (;;) {
				int count = selector.select();
				if (count == 0)
					return;
				Iterator<SelectionKey> iter = selector.selectedKeys()
						.iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					if (!key.isValid()) {
						continue;
					}
					if (key.isAcceptable()) {
						accept(key);
					}
					iter.remove();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel clientChannel = ssc.accept();
		clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
//		 clientChannel.socket().setTcpNoDelay(true);
//		 clientChannel.socket().setReceiveBufferSize(12024);
//		 clientChannel.socket().setSendBufferSize(1024);
		// clientChannel.configureBlocking(false);
		// clientChannel.register(selector, SelectionKey.OP_READ);
		logger.info("客户端连接:"+clientChannel.getRemoteAddress());
		SubReactor sub = nextWorker();
		sub.register(clientChannel);
	}

	SubReactor nextWorker() {
		return workers[Math.abs(workerIndex.getAndIncrement() % workers.length)];
	}
}
