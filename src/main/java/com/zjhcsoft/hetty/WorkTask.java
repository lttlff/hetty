package com.zjhcsoft.hetty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zjhcsoft.hetty.constant.Constant;
import com.zjhcsoft.hetty.http.DefaultHttpResponse;
import com.zjhcsoft.hetty.http.HttpResponse;
import com.zjhcsoft.hetty.http.HttpResponseStatus;
import com.zjhcsoft.hetty.http.HttpVersion;

/**
 * @date 2015-2-2 下午03:30:46
 * @Title: WorkTask.java
 * @author lff
 */
public class WorkTask implements Runnable {
	Logger logger = LoggerFactory.getLogger(WorkTask.class);
	volatile Selector selector;
	/* 缓冲区大小 */
	private int BLOCK = Constant.DEFAULT_BLOCK;
	/* 发送数据缓冲区 */
	private ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK);
	/* 接受数据缓冲区 徐扩展采用 缓冲池，推外内存 */
	private ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK);

	public WorkTask(Selector selector) {
		this.selector = selector;
	}

	@Override
	public void run() {
		try {
			for (;;) {
				// 选择一组键，并且相应的通道已经打开

				selector.select();
				// 返回此选择器的已选择键集。
				Set<SelectionKey> selectionKeys = selector.selectedKeys();

				for (Iterator<SelectionKey> i = selectionKeys.iterator(); i
						.hasNext();) {
					SelectionKey key = i.next();
					i.remove();
					if (key.isReadable()) {
						processRead(key);
					} else if (key.isWritable()) {
						processWrite(key);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processRead(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out our read buffer so it's ready for new data
		// this.receivebuffer.clear();
		// read 读取到缓冲
		// Attempt to read off the channel
		
		int numRead;
		this.receivebuffer.clear();
		try {
			numRead = socketChannel.read(this.receivebuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			e.printStackTrace();
			logger.error("客户端断开连接:"+socketChannel.getRemoteAddress());
			key.cancel();
			socketChannel.close();
			return;
		}
		logger.info("读取客户端请求:"+socketChannel.getRemoteAddress()+":"+numRead);
		// byte[] bytes = clientMessage.get(socketChannel);
		byte[] bytes = null;
		if (bytes == null) {
			bytes = new byte[0];
		}
		if (numRead > 0) {
			byte[] newBytes = new byte[bytes.length + numRead];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			System.arraycopy(receivebuffer.array(), 0, newBytes, bytes.length,
					numRead);
			String message = new String(newBytes);
			System.out.println(message);
			logger.info(message);
		} else {
			System.out.println(":numRead"+numRead);
		}
		// decode

		// 业务处理 业务线程池处理，和io线程隔离开来

		// 注册写事件
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void processWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// encode
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK);
		// write
		socketChannel.write(ByteBuffer.wrap("{sucess:true}".getBytes()));
		// 注册读事件或者断开
		socketChannel.register(selector, SelectionKey.OP_READ);
	}
}
