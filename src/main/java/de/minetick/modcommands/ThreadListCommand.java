package de.minetick.modcommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ThreadListCommand extends Command {

    public ThreadListCommand(String name) {
        super(name);
        this.usageMessage = "/threadlist";
        this.description = "Creates a file with debug information about currently active threads";
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if(sender instanceof ConsoleCommandSender || sender instanceof Player) {
            if(sender instanceof Player) {
                Player p = (Player) sender;
                if(!p.isOp()) {
                    p.sendMessage("You are not allowed to use this command!");
                    return true;
                }
            }

            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            TreeMap<Long, Thread> map = new TreeMap<Long, Thread>();
            for(Thread t: threadSet) {
                map.put(t.getId(), t);
            }
            SimpleDateFormat df = new SimpleDateFormat ("yyyy.MM.dd_HH-mm-ss");
            String folderName = "Profiler";
            File folder = new File(folderName);
            String pathToFile = folderName + "\\ThreadList-" + df.format(new Date()) + ".txt";
            File file = new File(pathToFile);
            NavigableMap<Long, Thread> rev = map.descendingMap();
            Iterator<Entry<Long, Thread>> iter = rev.entrySet().iterator();
            PrintWriter bw = null;
            try {
                if(!folder.exists()) {
                    folder.mkdirs();
                }
                bw = new PrintWriter(new BufferedWriter(new FileWriter(file, true), 8 * 1024));
                while(iter.hasNext()) {
                    Thread t = iter.next().getValue();
                    bw.println("================================================================================");
                    bw.println("================================================================================");
                    bw.println("Thread name:  " + t.getName() + "  ID:  " + t.getId());
                    bw.println("is alive:  " + String.valueOf(t.isAlive()));
                    bw.println("Thread priority:  " + t.getPriority());
                    bw.println("Thread state:  " + t.getState().toString());
                    bw.println("Thread group name:  " + t.getThreadGroup().getName());
                    
                    StackTraceElement[] s = t.getStackTrace();
                    for(int i = s.length - 1; i >= 0 && i > s.length - 10; i--) {
                        StackTraceElement x = s[i];
                        bw.println(x.getClassName() + " : " + x.getMethodName() + " : " + x.getLineNumber());
                    }
                }
                bw.flush();
                sender.sendMessage("Thread details logged: "  + pathToFile);
            } catch (IOException e) {
                sender.sendMessage("An error ocurrred, while logging all Threads: " + e.getMessage());
            } finally {
                if(bw != null) {
                    bw.close();
                }
            }

        }
        return true;
    }
}
