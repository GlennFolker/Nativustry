package nativustry;

import arc.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Nativustry extends Mod{
    // The native libraries won't be loaded until `FileTreeInitEvent` is fired.
    private static boolean isLoaded = false;

    public Nativustry(){
        Events.on(FileTreeInitEvent.class, e -> Core.app.post(() -> {
            synchronized(Nativustry.class){
                if(isLoaded) return;

                String name;
                if(OS.isWindows){
                    if(OS.isARM){
                        throw new IllegalStateException("Nativustry doesn't work on ARM machines running Windows");
                    }else{
                        name = "windows/natives" + (OS.is64Bit ? "64.dll" : ".dll");
                    }
                }else if(OS.isLinux || OS.isAndroid){
                    name = (OS.isLinux ? "linux" : "android") + "/libnatives" + (OS.isARM ? "arm" : "") + (OS.is64Bit ? "64.so" : ".so");
                }else if(OS.isMac){
                    name = "apple/libnatives" + (OS.is64Bit ? "64.dylib" : ".dylib");
                }else{
                    throw new IllegalStateException("Unsupported OS " + OS.osName);
                }

                name = "libs/" + name;
                loadFile(name);

                initialize();
                isLoaded = true;

                Log.info("Successfully loaded Nativustry native libraries.");
            }
        }));
    }

    @Override
    public void init(){
        sayHello();
    }

    private static native void initialize();

    public static native void sayHello();

    private static void loadFile(String srcPath){
        String srcCrc = crc(tree.get(srcPath).read());
        String fileName = new File(srcPath).getName();

        File file = new File(OS.prop("java.io.tmpdir") + "/nativustry/" + srcCrc, fileName);

        if(loadFile(srcPath, srcCrc, file)) return;
        try{
            file = File.createTempFile(srcCrc, null);
            if(file.delete() && loadFile(srcPath, srcCrc, file)) return;
        }catch(Throwable ignored){}

        file = new File(OS.userHome + "/.nativustry/" + srcCrc, fileName);
        if(loadFile(srcPath, srcCrc, file)) return;

        file = new File(".temp/" + srcCrc, fileName);
        if(loadFile(srcPath, srcCrc, file)) return;

        file = new File(fileName);
        if(loadFile(srcPath, srcCrc, file)) return;

        file = new File(OS.prop("java.library.path"), srcPath);
        if(file.exists()){
            System.load(file.getAbsolutePath());
            return;
        }

        throw new IllegalStateException("Couldn't load native library");
    }

    private static boolean loadFile(String srcPath, String srcCrc, File file){
        try{
            System.load(extractFile(srcPath, srcCrc, file).getAbsolutePath());
            return true;
        }catch(Throwable ignored){
            return false;
        }
    }

    private static File extractFile(String srcPath, String srcCrc, File file) throws IOException{
        String extCrc = null;
        if(file.exists()){
            try{
                extCrc = crc(new FileInputStream(file));
            }catch(FileNotFoundException ignored){}
        }

        if(extCrc == null || !extCrc.equals(srcCrc)){
            InputStream input = null;
            FileOutputStream output = null;

            try{
                input = tree.get(srcPath).read();
                if(file.getParentFile() != null){
                    file.getParentFile().mkdirs();
                }

                output = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                while(true){
                    int length = input.read(buffer);
                    if(length == -1) break;
                    output.write(buffer, 0, length);
                }
            }catch(IOException ex){
                throw new ArcRuntimeException("Error extracting file: " + srcPath + "\nTo: " + file.getAbsolutePath(), ex);
            }finally{
                Streams.close(input);
                Streams.close(output);
            }
        }

        return file;
    }

    private static String crc(InputStream input){
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        try{
            while(true){
                int length = input.read(buffer);
                if(length == -1) break;
                crc.update(buffer, 0, length);
            }
        }catch(Exception ignored){
        }finally{
            Streams.close(input);
        }

        return Long.toString(crc.getValue(), 16);
    }
}
