package com.example.multidextest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Application;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class ApplicationLoader extends Application{
	
	private static final String TAG = "ApplicationLoader";
	
	@Override
	public void onCreate(){
		super.onCreate();
		Log.d(TAG, "onCreate");
		try {
			copyDex();
			loadDex();
		} catch (Exception e) {
			Log.d(TAG, "load error " + e);
		}
		
	}
	
	//复制apk中的subdex到私有目录
	private void copyDex() throws Exception{
		Log.d(TAG, "copyDex");
		//获取apk文件
		File originalApk = new File(getApplicationInfo().sourceDir);
		
		File newApk = new File(getFilesDir().getAbsoluteFile() + File.separator + "test.apk");
		if (!newApk.exists()) {
			newApk.createNewFile();
		}
		//拷贝apk
		copyFile(new FileInputStream(originalApk), new FileOutputStream(newApk));
		
		ZipFile apk = new ZipFile(newApk);
		
		Enumeration<? extends ZipEntry> enumeration = apk.entries();
		
		ZipEntry zipEntry = null;
		
		while (enumeration.hasMoreElements()) {
			zipEntry = (ZipEntry) enumeration.nextElement();
			
			if (!zipEntry.isDirectory() && zipEntry.getName().endsWith("dex")&& !"classes.dex".equals(zipEntry.getName())) {
				Log.d(TAG, "zip entry name " + zipEntry.getName() + " file size "+ zipEntry.getSize());
				InputStream is = apk.getInputStream(zipEntry);
				FileOutputStream fos = openFileOutput(zipEntry.getName(), MODE_PRIVATE);
				copyFile(is, fos);
			}
			
		}
		
		apk.close();
	}
	
	private void loadDex(){
		Log.d(TAG, "loadDex");
		File[] files = getFilesDir().listFiles();
		if (null != files) {
			for(File file: files){
				String fileName = file.getName();
				if (fileName.endsWith("dex") && !"classes.dex".equals(fileName)) {
					injectDex(file.getAbsolutePath());
				}
			}
		}
	}
	
	
	
	 //拷贝文件
    private void copyFile(InputStream is, FileOutputStream fos) {
        try {
            int hasRead = 0;
            byte[] buf = new byte[1024];
            while((hasRead = is.read(buf)) > 0) {
                fos.write(buf, 0, hasRead);
            }
            fos.flush();
        } catch (Exception e) {
            Log.d(TAG, "copyFile error " + e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if(is != null) {
                    is.close();
                }
            } catch (Exception e2) {
            	Log.d(TAG, "copyFile close error " + e2);
            }
        }
    }

    
    private String injectDex(String dexPath){
    	boolean hasBaseDexClassLoaded = true;
    	try {
			Class.forName("dalvik.system.BaseDexClassLoader");
		} catch (Exception e) {
			Log.d(TAG, "load BaseDexClassLoader fail " + e);
			hasBaseDexClassLoaded = false;
		}
    	
    	if (hasBaseDexClassLoaded) {
			PathClassLoader pathClassLoader = (PathClassLoader)getClassLoader();
			DexClassLoader dexClassLoader = new DexClassLoader(dexPath, getDir("dex", 0).getAbsolutePath(), dexPath, getClassLoader());
			try {
				Object dexElements = combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));
				Object pathList = getPathList(pathClassLoader);
				setField(pathList,  pathList.getClass(), "dexElements", dexElements);
				return "SUCCESS";
			} catch (Exception e) {
				Log.d(TAG, " " + e);
			}	
		}
    	return "";
    }
    
    public Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }
    
    public Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    public static void setField(Object obj, Class<?> cl, String field, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }
    
    public Object getDexElements(Object paramObject)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return getField(paramObject, paramObject.getClass(), "dexElements");
    }

    
    public static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> localClass = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);
        int j = i + Array.getLength(arrayRhs);
        Object result = Array.newInstance(localClass, j);
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(arrayLhs, k));
            } else {
                Array.set(result, k, Array.get(arrayRhs, k - i));
            }
        }
        return result;
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
