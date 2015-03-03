package com.zjhcsoft.hetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @date 2015-2-5 上午10:15:07
 * @Title: HttpClient.java
 * @author lff
 */
public class HttpClient implements Runnable {
	/* 标识数字 */
	private static int flag = 0;
	/* 缓冲区大小 */
	private static int BLOCK = 40960;
	/* 接受数据缓冲区 */
	private ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK);
	/* 发送数据缓冲区 */
	private ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK);
	/* 服务器端地址 */
	private final static InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(
			"localhost", 8080);
	private static AtomicInteger atoc = new AtomicInteger();
	private Selector selector;
	SocketChannel client;
	static String sendText = "body:[11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "21111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "31111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "41111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "51111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "61111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "71111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "81111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "91111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "01111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "10111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n"
			+ "12111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111]\n";

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println(sendText.length());
		HttpClient httpClient = new HttpClient();
		httpClient.init();
		httpClient.connect();
		ExecutorService executor = Executors.newFixedThreadPool(5);
		for (int i = 0; i < 5; i++) {
			new Thread(httpClient).start();
		}
		System.out.println("end .. ");
	}

	public void init() throws IOException {
		// 打开socket通道
		SocketChannel socketChannel = SocketChannel.open();
		// 设置为非阻塞方式
		socketChannel.configureBlocking(false);
		// 打开选择器
		selector = Selector.open();
		// 注册连接服务端socket动作
		socketChannel.register(selector, SelectionKey.OP_CONNECT);
		// 连接
		socketChannel.connect(SERVER_ADDRESS);
		// 分配缓冲区大小内存
		sendbuffer.clear();
		sendbuffer.put(sendText.getBytes());
	}

	public void connect() throws IOException {
		// 选择一组键，其相应的通道已为 I/O 操作准备就绪。
		// 此方法执行处于阻塞模式的选择操作。
		selector.select();
		// 返回此选择器的已选择键集。
		Set<SelectionKey> selectionKeys = selector.selectedKeys();
		// System.out.println(selectionKeys.size());
		Iterator<SelectionKey> iterator = selectionKeys.iterator();
		while (iterator.hasNext()) {
			SelectionKey selectionKey = iterator.next();
			if (selectionKey.isConnectable()) {
				client = (SocketChannel) selectionKey.channel();
				// 判断此通道上是否正在进行连接操作。
				// 完成套接字通道的连接过程。
				if (client.isConnectionPending()) {
					client.finishConnect();
					System.out.println("完成连接!");
					// sendbuffer.clear();
					// sendbuffer.put(sendText.getBytes());
					sendbuffer.flip();
					client.write(sendbuffer);
				}
				client.register(selector, SelectionKey.OP_WRITE);
			}
			iterator.remove();
		}
	}

	public void write() throws IOException {
		String header = Thread.currentThread().getName() + "("
				+ atoc.getAndIncrement() + ")";
		System.out.println(header);
		ByteBuffer sendbuffer = ByteBuffer.wrap((header + sendText).getBytes());
		client.write(sendbuffer);
	}

	public void listen() throws IOException {
		while (true) {
			// 选择一组键，其相应的通道已为 I/O 操作准备就绪。
			// 此方法执行处于阻塞模式的选择操作。
			selector.select();
			// 返回此选择器的已选择键集。
			Set<SelectionKey> selectionKeys = selector.selectedKeys();
			// System.out.println(selectionKeys.size());
			Iterator<SelectionKey> iterator = selectionKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey selectionKey = iterator.next();
				this.handleKey(selectionKey);
				iterator.remove();
			}
		}
	}

	public void handleKey(SelectionKey selectionKey) throws IOException {
		if (selectionKey.isConnectable()) {
			System.out.println("client connect");
			client = (SocketChannel) selectionKey.channel();
			// 判断此通道上是否正在进行连接操作。
			// 完成套接字通道的连接过程。
			if (client.isConnectionPending()) {
				client.finishConnect();
				System.out.println("完成连接!");
				// sendbuffer.clear();
				// sendbuffer.put(sendText.getBytes());
				sendbuffer.flip();
				client.write(sendbuffer);
			}
			client.register(selector, SelectionKey.OP_WRITE);
		} else if (selectionKey.isReadable()) {
			SocketChannel client = (SocketChannel) selectionKey.channel();
			// 将缓冲区清空以备下次读取
			receivebuffer.clear();
			// 读取服务器发送来的数据到缓冲区中
			int count = client.read(receivebuffer);
			if (count > 0) {
				String receiveText = new String(receivebuffer.array(), 0, count);
				System.out.println("客户端接受服务器端数据--:" + receiveText);
				client.register(selector, SelectionKey.OP_WRITE);
				// client.close();
				// selector.close();
			}
		} else if (selectionKey.isWritable()) {
			// sendbuffer.clear();
			SocketChannel client = (SocketChannel) selectionKey.channel();
			// sendText = "message from client--" + (flag++);
			System.out.println("input sth");

			// sendbuffer.put(sendText.getBytes());
			// 将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位
			sendbuffer.clear();
			sendbuffer.put((Thread.currentThread().getName() + "("
					+ atoc.getAndIncrement() + ")" + sendText).getBytes());
			sendbuffer.flip();
			client.write(sendbuffer);
			client.register(selector, SelectionKey.OP_READ);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				/*
				 * try { Thread.sleep(1000); } catch (InterruptedException e) { //
				 * TODO Auto-generated catch block e.printStackTrace(); }
				 */
				if (atoc.get() < 500) {
					this.write();
				} else {

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
