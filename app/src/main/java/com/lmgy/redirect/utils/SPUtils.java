package com.lmgy.redirect.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Created by lmgy on 14/8/2019
 */
public class SPUtils {
    private static String SP_NAME = "redirect_sp";
    private static final String TAG = "SPUtils";

    /**
     * 获取SharedPreference保存的value
     * 值的类型：（int，float，long，boolean）+String
     *
     * @param context 上下文
     * @param key     储存值的key
     * @param clazz   获取值的类型   int，float，long，boolean）+String
     * @param <T>     T
     * @return 值
     */
    public static <T> T getValue(Context context, String key, Class<T> clazz) {
        if (context == null) {
            throw new RuntimeException("context is null");
        }

        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return getValue(key, getT(clazz), sp);
    }

    /**
     * 获取SharedPreference保存的value
     * 值的类型：（int，float，long，boolean）+String
     */
    private static <T> T getValue(String key, T t, SharedPreferences sp) {
        if (t == null) {
            return null;
        }
        if (t instanceof Integer) {
            return (T) Integer.valueOf(sp.getInt(key, 0));
        } else if (t instanceof String) {
            return (T) sp.getString(key, "");
        } else if (t instanceof Boolean) {
            return (T) Boolean.valueOf(sp.getBoolean(key, false));
        } else if (t instanceof Long) {
            return (T) Long.valueOf(sp.getLong(key, 0L));
        } else if (t instanceof Float) {
            return (T) Float.valueOf(sp.getFloat(key, 0L));
        }
        Log.e(TAG, "无法找到" + key + "对应的值");
        return null;
    }

    /**
     * 通过反射创建类实例
     * 反射有两种方式创建类实例：
     * 1、Class.newInstance()：只能调用无参的构造函数（默认构造函数）
     * 2、Constructor.newInstance()：可以根据传入的参数，调用任意构造构造函数。
     * Integer、Boolean、Long、Float 没有默认构造函数，只能通过 Constructor.newInstance() 调用
     * String 是有默认构造函数的，两种方法都适用
     * @param clazz  String.class、Integer.class、Boolean.class、Long.class、Float.class
     * @param <T> T
     * @return T
     */
    private static <T> T getT(Class<T> clazz) {
        T t = null;
        String clazzName = clazz.getName();
        Log.e(TAG, "基本类型名字是[" + clazzName + "]");
        try {
            if ("java.lang.Integer".equals(clazzName)) {
                t = clazz.getConstructor(int.class).newInstance(1);
            } else if ("java.lang.String".equals(clazzName)) {
                t = clazz.newInstance();
            } else if ("java.lang.Boolean".equals(clazzName)) {
                t = clazz.getConstructor(boolean.class).newInstance(false);
            } else if ("java.lang.Long".equals(clazzName)) {
                t = clazz.getConstructor(long.class).newInstance(0L);
            } else if ("java.lang.Float".equals(clazzName)) {
                t = clazz.getConstructor(float.class).newInstance(0F);
            }
        } catch (InstantiationException e) {
            Log.e(TAG, "类型输入错误或者复杂类型无法解析[" + e.getMessage() + "]");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "类型输入错误或者复杂类型无法解析[" + e.getMessage() + "]");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, "类型输入错误或者复杂类型无法解析[" + e.getMessage() + "]");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "类型输入错误或者复杂类型无法解析[" + e.getMessage() + "]");
            e.printStackTrace();
        }

        return t;
    }

    /**
     * 使用SharedPreference保存value
     *
     * @param context 上下文
     * @param key     储存值的key
     * @param value   值
     */
    public static void setValue(Context context, String key, Object value) {
        if (context == null) {
            throw new RuntimeException("context is null");
        }

        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        setValue(key, value, sp);
    }

    /**
     * 使用SharedPreference保存value
     *
     * @param key   储存值的key
     * @param value 值
     * @param sp    SharedPreferences
     */
    private static void setValue(String key, Object value, SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        }
        editor.apply();
    }

    /**
     * 使用SharedPreference保存序列化对象
     * 用Base64.encode将字节文件转换成Base64编码保存在String中
     *
     * @param context 上下文
     * @param key     储存对象的key
     * @param object  object对象  对象必须实现Serializable序列化，否则会出问题，
     *                out.writeObject 无法写入 Parcelable 序列化的对象
     */
    public static void setObject(Context context, String key, Object object) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        //创建字节输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //创建字节对象输出流
        ObjectOutputStream out = null;
        try {
            //然后通过将字对象进行64转码，写入sp中
            out = new ObjectOutputStream(baos);
            out.writeObject(object);
            String objectValue = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, objectValue);
            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }

                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取SharedPreference保存的对象
     * 使用Base64解密String，返回Object对象
     *
     * @param context 上下文
     * @param key     储存对象的key
     * @param <T>     泛型
     * @return 返回保存的对象
     */
    public static <T> T getObject(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (sp.contains(key)) {
            String objectValue = sp.getString(key, null);
            byte[] buffer = Base64.decode(objectValue, Base64.DEFAULT);
            //一样通过读取字节流，创建字节流输入流，写入对象并作强制转换
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(bais);
                T t = (T) ois.readObject();
                return t;
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bais != null) {
                        bais.close();
                    }

                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 使用SharedPreferences保存List
     * 支持类型：List<String>，List<JavaBean>
     *
     * @param context  上下文
     * @param key      储存的key
     * @param dataList 存储数据
     * @param <T>      泛型
     */
    public static <T> void setDataList(Context context, String key, List<T> dataList) {
        if (null == dataList) {
            return;
        }

        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(dataList);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, strJson);
        editor.apply();
    }

    /**
     * 获取SharedPreferences保存的List
     *
     * @param context 上下文
     * @param key     储存的key
     * @param <T>     泛型
     * @return 存储List<T>数据
     */
    public static <T> List<T> getDataList(Context context, String key, Class<T> cls) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        List<T> dataList = new ArrayList<T>();
        String strJson = sp.getString(key, null);
        if (null == strJson) {
            return dataList;
        }

        Gson gson = new Gson();

        //使用泛型解析数据会出错，返回的数据类型是LinkedTreeMap
//        dataList = gson.fromJson(strJson, new TypeToken<List<T>>() {
//        }.getType());

        //这样写，太死
//        dataList = gson.fromJson(strJson, new TypeToken<List<UserModel>>() {
//        }.getType());

        JsonArray arry = new JsonParser().parse(strJson).getAsJsonArray();
        for (JsonElement jsonElement : arry) {
            dataList.add(gson.fromJson(jsonElement, cls));
        }

        return dataList;
    }

    /**
     * 使用SharedPreferences保存集合
     * @param context  上下文
     * @param key  储存的key
     * @param map  map数据
     */
    public static <K, V> void setHashMapData(Context context, String key, Map<K, V> map){
        if (map == null) {
            return ;
        }

        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String strJson = gson.toJson(map);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, strJson);
        editor.apply();
    }

    /**
     * 获取SharedPreferences保存的集合
     * @param context  上下文
     * @param key  储存的key
     * @param clsV  解析类型
     * @return  Map集合
     */
    public static <V> Map<String, V> getHashMapData(Context context, String key, Class<V> clsV){
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Map<String, V> map = new HashMap<>();
        String strJson = sp.getString(key, "");
        Gson gson = new Gson();
        JsonObject obj =new JsonParser().parse(strJson).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entrySet = obj.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String entryKey = entry.getKey();
            JsonObject value = (JsonObject) entry.getValue();
            map.put(entryKey, gson.fromJson(value, clsV));
        }

        return map;
    }
}
