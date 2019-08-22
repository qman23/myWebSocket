package cn.com.lee.myWebSocket;

import java.nio.charset.Charset;
import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * 响应websocket请求的核心业务处理类
 * @author ZHIWEILI
 *
 */
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object>{

	private WebSocketServerHandshaker handshaker;
	
	private static final String WEB_SOCKET_URL = "ws://localhost:8888/websocket";
	
	

	/**
	 * 客户端与服务端创建连接的时候调用
	 */
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof FullHttpRequest) {//处理客户端发起的http握手请求业务
			handHttpRequest(ctx, (FullHttpRequest)msg);
		}else if(msg instanceof WebSocketFrame) {//处理websocket连接业务
			handWebSocketFrame(ctx, (WebSocketFrame)msg);
		}
	}
//	/**
//	 * 客户端与服务端创建连接的时候调用
//	 */
//	@Override
//	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//		if(msg instanceof FullHttpRequest) {//处理客户端发起的http握手请求业务
//			handHttpRequest(ctx, (FullHttpRequest)msg);
//		}else if(msg instanceof WebSocketFrame) {//处理websocket连接业务
//			handWebSocketFrame(ctx, (WebSocketFrame)msg);
//		}
//	}

	/**
	 * 处理websocket请求
	 * @param ctx
	 * @param frame
	 */
	private void handWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		
		//判断是否是关闭websocket指令
		if(frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());
		}
		//判断是否是ping消息
		if(frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		
		//如果是二进制消息，抛出异常
		if(!(frame instanceof TextWebSocketFrame)) {
			System.out.println("We don't support binary message!");
			throw new RuntimeException("["+this.getClass().getName()+"] don't support msg");
		}
		
		String request = ((TextWebSocketFrame)frame).text();
		System.out.println("receieve msg from client:"+request);
		
		TextWebSocketFrame twsf = new TextWebSocketFrame(new Date().toString()+ctx.channel().id()+"----->"+request);
		
		//群发,服务端向所有客户端发消息
		NettyConfig.group.writeAndFlush(twsf);
	}
	
	/**
	 * 处理客户端发起的http握手请求业务
	 * @param ctx
	 * @param req
	 */
	private void handHttpRequest(ChannelHandlerContext ctx,FullHttpRequest req) {
		if(!req.getDecoderResult().isSuccess()|| !("websocket".equals(req.headers().get("Upgrade")))) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
		handshaker = wsFactory.newHandshaker(req);
		if(handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		}else {
			handshaker.handshake(ctx.channel(), req);
		}
	}
	
	/**
	 * 服务端向客户端响应消息
	 * @param ctx
	 * @param req
	 * @param resp
	 */
	private void sendHttpResponse(ChannelHandlerContext ctx,FullHttpRequest req,DefaultFullHttpResponse resp) {
		if(resp.getStatus().code()!=200) {
			ByteBuf buf =Unpooled.copiedBuffer(resp.getStatus().toString(),CharsetUtil.UTF_8);
			resp.content().writeBytes(buf);
			buf.release();
		}
		//服务端向客户端发送数据
		ChannelFuture f = ctx.channel().writeAndFlush(resp);
		if(resp.getStatus().code()!=200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	/**
	 * 客户端与服务端创建连接调用
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		NettyConfig.group.add(ctx.channel());
		System.out.println("client and server connection active..");
	}

	/**
	 * 客户端与服务端断开电泳
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		NettyConfig.group.remove(ctx.channel());
		System.out.println("client and server connection close..");
	}

	/**
	 * 服务端接收客户端发送过来的数据结束后调用
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	/**
	 * 工程出现异常的时候调用
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	
}
