package com.zjhcsoft.hetty.util;

import java.nio.ByteBuffer;

/** 
 * @date 2015-2-3 下午02:17:14
 * @Title: ByteBuffers.java 
 * @author lff
 */
public class ByteBuffers {
	
	public static ByteBuffer wrappedBuffer(ByteBuffer... buffers){
		byte[] bytes = new byte[0];
		for(ByteBuffer byteBuffer : buffers){
			int numRead = byteBuffer.limit() - byteBuffer.position();
			byte[] newBytes = new byte[bytes.length + numRead];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			System.arraycopy(byteBuffer.array(), 0, newBytes, bytes.length,
					numRead);
			bytes = newBytes;
		}
		return ByteBuffer.wrap(bytes);
	}
}
