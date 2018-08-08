package com.kodingkingdom.craftersync;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.kodingkingdom.craftercoordinator.CrafterCoordinator;
import com.kodingkingdom.craftercoordinator.CrafterCoordinatorPlugin;

public class CrafterSync implements Listener, CommandExecutor{
	public final static long pollInterval=8;

	CrafterSyncPlugin plugin;	
	public CrafterSync(CrafterSyncPlugin Plugin){plugin=Plugin;}
	
	public void Live(){
		plugin .getCommand ("csync").setExecutor(this);
	}
	public void Die(){}
	
	public void registerEvents(Listener listener){
		plugin.getServer().getPluginManager().registerEvents(listener, plugin);}

	public int scheduleAsyncTask(Runnable task){
		return plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, task);}
	public int scheduleAsyncTask(Runnable task, long delay){
		return plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, task, delay);}
	public int scheduleTask(Runnable task, long delay){
		return plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, delay);}
	public void cancelTask(int taskId){
		plugin.getServer().getScheduler().cancelTask(taskId);}

	
	@Override
	public boolean onCommand(CommandSender x, Command y, String z, String[] args) {
		if (args .length < 8)
			return false;
		else {
			try {
				
				long load_time = CrafterSync.getCoordinator().getLoadTimeLimit();

				String who = x instanceof Player ? ((Player) x) .getName() : null;
				
				String to_world = args [0];
				int to_x_min = Integer.parseInt(args [1]);
				int to_z_min = Integer.parseInt(args [2]);
				int to_x_max = Integer.parseInt(args [3]);
				int to_z_max = Integer.parseInt(args [4]);
				String from_world = args [5];
				int from_x_min = Integer.parseInt(args [6]);
				int from_z_min = Integer.parseInt(args [7]);
				
				this .scheduleTask (new BukkitRunnable(){public void run(){
					sync (to_world, to_x_min, to_z_min, to_x_max, to_z_max, from_world, from_x_min, from_z_min,
							who, from_x_min, from_z_min, Long.MAX_VALUE);}}, load_time);
				
				return true;
			}
			catch (Exception e) {
				return false;
			}
		}
	}
	
	void sync (String to_world, int to_x_min, int to_z_min, int to_x_max, int to_z_max, String from_world, int from_x_min, int from_z_min,
			String who, int x_now, int z_now, long time_since_hi) {
		long beginTime = System.currentTimeMillis();
		
		long load_time = CrafterSync.getCoordinator().getLoadTimeLimit();
		long load_amount = CrafterSync.getCoordinator().getLoadAmountLimit();
		
		//World from_world_w = new WorldCreator (from_world) .createWorld ();
		//World to_world_w = new WorldCreator (to_world) .createWorld ();
		World from_world_w = Bukkit.getWorld(from_world);
		World to_world_w = Bukkit.getWorld(to_world);
		
		int total_columns = (to_x_max - to_x_min) * (to_z_max - to_z_min);
		int columns_done = (x_now - from_x_min) * (to_z_max - to_z_min) + (z_now - from_z_min);
		int columns_left = total_columns - columns_done;
		
		long total_blocks = total_columns * 256;
		long blocks_done = columns_done * 256;
		if (time_since_hi > 20) {
			time_since_hi = 0;
			String msg = "Synced " + blocks_done + " blocks, " + ((blocks_done * 100) / total_blocks) + "% done";
			CrafterSyncPlugin.debug(msg);
			if (who != null) {
				Player player = Bukkit.getPlayer(who);
				if (player != null) player.sendMessage(msg);}}
		
		long column_stride = Math .min ((load_amount / 256) + 1, columns_left);
		long column_strides_left = column_stride;
		while (column_strides_left > 0) {
			CrafterSyncPlugin.debug(column_strides_left + ": " + x_now + ";" + z_now);
			for (int y_now = 0; y_now < 256; y_now ++) {
				try {
					BlockState from_state = from_world_w.getBlockAt(x_now, y_now, z_now) .getState();
					BlockState to_state = to_world_w.getBlockAt(x_now + (to_x_min - from_x_min), y_now, z_now + (to_z_min - from_z_min)) .getState();
					to_state.setType(from_state .getType());
					to_state.setData(from_state .getData());
					to_state.update(true, false);
				}
				catch (Throwable e) {
					String msg = "Found bad block";
					CrafterSyncPlugin.debug(msg);
					if (who != null) {
						Player player = Bukkit.getPlayer(who);
						if (player != null) player.sendMessage(msg);}
				}
			}
			z_now ++;
			if (z_now == from_z_min + (to_z_max - to_z_min)) {
				z_now = from_z_min;
				x_now ++; }
			column_strides_left --; }
		
		columns_left -= column_stride;

		final int final_x_now = x_now;
		final int final_z_now = z_now;
		final long final_time_since_hi = time_since_hi + load_time;

		long endTime = System.currentTimeMillis();
		long tickOffset = (endTime-beginTime)/50;
		
		if (columns_left == 0) {
			time_since_hi = 0;
			
			columns_done = (x_now - from_x_min) * (to_z_max - to_z_min) + (z_now - from_z_min);
			blocks_done = columns_done * 256;
			
			String msg = "Synced " + blocks_done + " blocks, " + ((blocks_done * 100) / total_blocks) + "% done";
			CrafterSyncPlugin.debug(msg);
			if (who != null) {
				Player player = Bukkit.getPlayer(who);
				if (player != null) player.sendMessage(msg);}}
		
		/*/if (false) {/*/if (columns_left > 0) {/**/
			this .scheduleTask (new BukkitRunnable(){public void run(){
				sync (to_world, to_x_min, to_z_min, to_x_max, to_z_max, from_world, from_x_min, from_z_min,
						who, final_x_now, final_z_now, final_time_since_hi);}}, load_time+tickOffset); }
	}
	

    private static CrafterCoordinator coordinator;
    public static CrafterCoordinator getCoordinator(){
    	if (coordinator==null)coordinator=CrafterCoordinatorPlugin.getPlugin().getCoordinator();
    	return coordinator;}
}
