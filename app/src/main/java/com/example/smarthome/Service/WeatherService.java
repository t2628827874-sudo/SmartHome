package com.example.smarthome.Service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.smarthome.Model.WeatherModel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 天气API服务类
 * 
 * 功能说明：
 * - 调用天气API获取天气数据
 * - 支持异步请求和回调
 * - 包含错误处理和重试机制
 * - 数据验证确保温度为数字格式
 * 
 * API文档: http://www.tianqiapi.com/index/doc?version=day
 * 
 * 使用方式：
 * WeatherService service = WeatherService.getInstance();
 * service.getWeather("沧州", new WeatherCallback() { ... });
 */
public class WeatherService {
    
    private static final String TAG = "WeatherService";
    
    private static final String BASE_URL = "http://v1.yiketianqi.com/free/day";
    private static final String APP_ID = "33196931";
    private static final String APP_SECRET = "FeqO28GZ";
    private static final String CANGZHOU_CITY_ID = "101090701"; // 沧州城市ID
    
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000;
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    
    private WeatherModel cachedWeather;
    private long lastFetchTime;
    
    private static WeatherService instance;
    
    public interface WeatherCallback {
        void onSuccess(WeatherModel weather);
        void onError(String errorMessage);
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized WeatherService getInstance() {
        if (instance == null) {
            instance = new WeatherService();
        }
        return instance;
    }
    
    private WeatherService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 获取天气数据（带缓存）
     * 
     * @param city 城市名称
     * @param callback 回调接口
     */
    public void getWeather(String city, WeatherCallback callback) {
        fetchWeatherFromApi(city, callback);
    }
    
    /**
     * 强制刷新天气数据（忽略缓存）
     */
    public void refreshWeather(String city, WeatherCallback callback) {
        fetchWeatherFromApi(city, callback);
    }
    
    /**
     * 从API获取天气数据
     * 
     * @param city 城市名称
     * @param callback 回调接口
     */
    private void fetchWeatherFromApi(String city, WeatherCallback callback) {
        // 清除旧缓存，确保获取最新数据
        cachedWeather = null;
        lastFetchTime = 0;
        
        String url = buildRequestUrl(city);
        Log.d(TAG, "请求天气API: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .cacheControl(new okhttp3.CacheControl.Builder()
                        .noCache()
                        .noStore()
                        .build())
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "天气API请求失败: " + e.getMessage(), e);
                String errorMsg = "网络请求失败: " + e.getMessage();
                
                if (cachedWeather != null) {
                    Log.w(TAG, "使用缓存数据作为备用");
                    notifySuccess(callback, cachedWeather);
                } else {
                    notifyError(callback, errorMsg);
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "服务器错误: " + response.code();
                    Log.e(TAG, errorMsg);
                    notifyError(callback, errorMsg);
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "========== 天气API响应 ==========");
                    Log.d(TAG, "完整响应: " + responseBody);
                    Log.d(TAG, "================================");
                    
                    WeatherModel weather = parseWeatherResponse(responseBody);
                    
                    if (weather != null && validateWeatherData(weather)) {
                        cachedWeather = weather;
                        lastFetchTime = System.currentTimeMillis();
                        
                        Log.d(TAG, "========== 天气数据解析成功 ==========");
                        Log.d(TAG, "城市: " + weather.getCity());
                        Log.d(TAG, "温度(原始): " + weather.getTemperature());
                        Log.d(TAG, "温度(格式化): " + weather.getFormattedTemperature());
                        Log.d(TAG, "天气: " + weather.getWeather());
                        Log.d(TAG, "最高温: " + weather.getTemperatureDay());
                        Log.d(TAG, "最低温: " + weather.getTemperatureNight());
                        Log.d(TAG, "=====================================");
                        
                        notifySuccess(callback, weather);
                    } else {
                        String errorMsg = "数据验证失败";
                        if (weather == null) {
                            errorMsg = "数据解析失败";
                        }
                        Log.e(TAG, errorMsg);
                        notifyError(callback, errorMsg);
                    }
                    
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析错误: " + e.getMessage(), e);
                    notifyError(callback, "数据格式错误");
                } catch (Exception e) {
                    Log.e(TAG, "处理响应失败: " + e.getMessage(), e);
                    notifyError(callback, "处理响应失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 构建请求URL
     * 
     * @param city 城市名称
     * @return 完整的请求URL
     */
    private String buildRequestUrl(String city) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("?appid=").append(APP_ID);
        url.append("&appsecret=").append(APP_SECRET);
        url.append("&unescape=1");
        
        // 使用城市名称参数
        if (city != null && !city.isEmpty()) {
            try {
                String encodedCity = URLEncoder.encode(city, "UTF-8");
                url.append("&city=").append(encodedCity);
                Log.d(TAG, "城市参数: city=" + encodedCity);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "URL编码失败: " + e.getMessage());
                url.append("&city=").append(city);
            }
        }
        
        Log.d(TAG, "完整请求URL: " + url.toString());
        return url.toString();
    }
    
    /**
     * 解析天气API响应
     * 
     * @param responseBody JSON响应体
     * @return WeatherModel对象，解析失败返回null
     */
    private WeatherModel parseWeatherResponse(String responseBody) {
        try {
            return gson.fromJson(responseBody, WeatherModel.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "解析天气数据失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 验证天气数据有效性
     * 确保温度值为数字格式，天气状况为标准描述文本
     * 
     * @param weather 天气数据
     * @return 数据是否有效
     */
    private boolean validateWeatherData(WeatherModel weather) {
        if (weather == null) {
            Log.e(TAG, "天气数据为空");
            return false;
        }
        
        // 验证城市
        if (weather.getCity() == null || weather.getCity().isEmpty()) {
            Log.w(TAG, "城市数据为空");
        }
        
        // 验证温度（必须为数字格式）
        String temp = weather.getTemperature();
        if (temp == null || temp.isEmpty()) {
            Log.w(TAG, "温度数据为空");
        } else {
            try {
                double tempValue = Double.parseDouble(temp);
                if (tempValue < -50 || tempValue > 60) {
                    Log.w(TAG, "温度值异常: " + tempValue);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "温度格式无效: " + temp);
                return false;
            }
        }
        
        // 验证天气描述
        String weatherDesc = weather.getWeather();
        if (weatherDesc == null || weatherDesc.isEmpty()) {
            Log.w(TAG, "天气描述为空");
        }
        
        return true;
    }
    
    /**
     * 在主线程回调成功
     */
    private void notifySuccess(WeatherCallback callback, WeatherModel weather) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(weather));
        }
    }
    
    /**
     * 在主线程回调错误
     */
    private void notifyError(WeatherCallback callback, String errorMessage) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(errorMessage));
        }
    }
    
    /**
     * 获取缓存的天气数据
     */
    public WeatherModel getCachedWeather() {
        return cachedWeather;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedWeather = null;
        lastFetchTime = 0;
    }
    
    /**
     * 获取天气图标资源名称
     * 
     * @param weatherImg 天气图片代码
     * @return 图标资源名称
     */
    public static String getWeatherIconName(String weatherImg) {
        if (weatherImg == null) return "ic_weather_default";
        
        switch (weatherImg) {
            case "qing":
                return "ic_weather_sunny";
            case "yin":
                return "ic_weather_cloudy";
            case "yun":
                return "ic_weather_partly_cloudy";
            case "yu":
                return "ic_weather_rain";
            case "xue":
                return "ic_weather_snow";
            case "wu":
                return "ic_weather_fog";
            case "lei":
                return "ic_weather_thunder";
            default:
                return "ic_weather_default";
        }
    }
    
    /**
     * 获取天气描述的简短版本
     */
    public static String getShortWeatherDesc(String weather) {
        if (weather == null) return "未知";
        
        if (weather.contains("晴")) return "晴";
        if (weather.contains("云") || weather.contains("阴")) return "阴";
        if (weather.contains("雨")) return "雨";
        if (weather.contains("雪")) return "雪";
        if (weather.contains("雾")) return "雾";
        if (weather.contains("雷")) return "雷";
        
        return weather;
    }
}
