/* * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import java.util.HashMap;
import java.util.Map;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 *
 * @author yjdwbj
 */
public class DispatchCenter {
    
    private static DispatchCenter instance; 
    
   // private final ChannelGroup channelGroup;
    private final Map<String,Object[]> UserAndChannel;
    private final Map<String,Channel> AppAndChannel;
    
    
    
    
    DispatchCenter()
    {
        //channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        UserAndChannel = new HashMap<String,Object[]>();
        AppAndChannel = new HashMap<String,Channel>();
    }
    
    public DispatchCenter instance()
    {
        if(instance == null)
        {
            instance = new DispatchCenter();
        }
        return instance;
    }
    
    public void bandUserAndChannel(String key,String pwd,Channel newChannel)
    {
         Object[] c = new Object[2];
        c[0] = pwd;
        c[1] = newChannel;
        UserAndChannel.put(key,c);
    }
    
    public void bandAppAndChannel(String key,Channel newChannel)
    {
       
        AppAndChannel.put(key,newChannel);
      
    }
    
    public Channel getAppChannel(String key)
    {
        return AppAndChannel.get(key);
    }
    
    public Object[] getDevChannel(String key)
    {
        return UserAndChannel.get(key);
    }
    
    public void removeDevChannel(String key)
    {
        Object[] d = UserAndChannel.get(key);
        Channel c = (Channel)d[1];
        c.close();
        
        UserAndChannel.remove(key);
    }
    
    public void removeAppChannel(String key)
    {
        Channel d = AppAndChannel.get(key);
        d.close();
        
        AppAndChannel.remove(key);
    }
    
    public int getMapSize(String mapName)
    {
        if(mapName.compareTo("dev") == 0)
        {
            return UserAndChannel.size();
        }else if(mapName.compareTo("app")==0){
            return AppAndChannel.size();
        }else{
            return -1;
        }
    }
    
    
    
}

