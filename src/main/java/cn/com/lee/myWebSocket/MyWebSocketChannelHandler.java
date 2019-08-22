package cn.com.lee.myWebSocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
/**
 * 初始化连接时的组件
 * @author ZHIWEILI
 *
 */

public class MyWebSocketChannelHandler extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel e) throws Exception {
		ChannelPipeline pipeline = e.pipeline();
		pipeline.addLast("http-codec", new HttpServerCodec());
		pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
		pipeline.addLast("http-chunked",new ChunkedWriteHandler());
		pipeline.addLast("handler", new MyWebSocketHandler());
	}

}
