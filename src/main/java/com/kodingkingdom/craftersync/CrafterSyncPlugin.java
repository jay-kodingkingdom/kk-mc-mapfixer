package com.kodingkingdom.craftersync;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class CrafterSyncPlugin extends JavaPlugin {
	CrafterSync x=new CrafterSync(this);
	@Override
    public void onEnable(){x.Live();} 
    @Override
    public void onDisable(){x.Die();}
        
	
	static CrafterSyncPlugin singleton;
	public CrafterSyncPlugin(){singleton=this;}
	public static CrafterSyncPlugin getPlugin(){return singleton;}
	public static void debug(String msg){
			singleton.getLogger().log(Level.INFO
					, msg);}}