package cn.com.lee.myWebSocket;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 存储全局配置
 * @author ZHIWEILI
 *
 */
public class NettyConfig {
	/**
	 * 存储每个客户端接入时的channel对象
	 */
	public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
