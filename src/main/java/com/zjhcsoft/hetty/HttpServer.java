package com.zjhcsoft.hetty;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/** 
 * @date 2015-2-2 上午09:30:39
 * @Title: HttpServer.java 
 * @author lff
 */
public class HttpServer {
	//sokect参数
	private volatile Map<String, Object> options = new HashMap<String, Object>();
	
	public void start(){
		this.start(8080);
	}
	public void start(int port){
		MainReactor server = new MainReactor(port);
		server.listen();
	}
	
	public static void  main(String[] args) throws UnsupportedEncodingException{
		System.out.println("\r\n".getBytes("ASCII"));
		new HttpServer().start();
	}
}
