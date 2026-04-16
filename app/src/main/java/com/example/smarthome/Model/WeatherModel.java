package com.example.smarthome.Model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 天气数据模型
 * 
 * 对应天气API返回的JSON数据结构
 * API文档: http://www.tianqiapi.com/index/doc?version=day
 * 
 * API返回格式（扁平结构）:
 * {
 *   "cityid": "101020100",
 *   "city": "上海",
 *   "update_time": "15:32",
 *   "wea": "多云",
 *   "wea_img": "yun",
 *   "tem": "25",
 *   "tem_day": "31",
 *   "tem_night": "25",
 *   "win": "北风",
 *   "win_speed": "1级",
 *   "win_meter": "1km/h",
 *   "air": "30",
 *   "humidity": "74%",
 *   "visibility": "30km",
 *   "pressure": "1006"
 * }
 */
public class WeatherModel {
    
    @SerializedName("cityid")
    private String cityId;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("update_time")
    private String updateTime;
    
    @SerializedName("date")
    private String date;
    
    @SerializedName("week")
    private String week;
    
    @SerializedName("wea")
    private String weather;
    
    @SerializedName("wea_img")
    private String weatherImg;
    
    @SerializedName("tem")
    private String temperature;
    
    @SerializedName("tem_day")
    private String temperatureDay;
    
    @SerializedName("tem_night")
    private String temperatureNight;
    
    @SerializedName("win")
    private String wind;
    
    @SerializedName("win_speed")
    private String windSpeed;
    
    @SerializedName("win_meter")
    private String windMeter;
    
    @SerializedName("air")
    private String air;
    
    @SerializedName("humidity")
    private String humidity;
    
    @SerializedName("visibility")
    private String visibility;
    
    @SerializedName("pressure")
    private String pressure;
    
    @SerializedName("sunrise")
    private String sunrise;
    
    @SerializedName("sunset")
    private String sunset;
    
    @SerializedName("alarm")
    private List<WeatherAlarm> alarms;
    
    public String getCityId() {
        return cityId;
    }
    
    public void setCityId(String cityId) {
        this.cityId = cityId;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getWeek() {
        return week;
    }
    
    public void setWeek(String week) {
        this.week = week;
    }
    
    public String getWeather() {
        return weather;
    }
    
    public void setWeather(String weather) {
        this.weather = weather;
    }
    
    public String getWeatherImg() {
        return weatherImg;
    }
    
    public void setWeatherImg(String weatherImg) {
        this.weatherImg = weatherImg;
    }
    
    public String getTemperature() {
        return temperature;
    }
    
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
    
    public String getTemperatureDay() {
        return temperatureDay;
    }
    
    public void setTemperatureDay(String temperatureDay) {
        this.temperatureDay = temperatureDay;
    }
    
    public String getTemperatureNight() {
        return temperatureNight;
    }
    
    public void setTemperatureNight(String temperatureNight) {
        this.temperatureNight = temperatureNight;
    }
    
    public String getWind() {
        return wind;
    }
    
    public void setWind(String wind) {
        this.wind = wind;
    }
    
    public String getWindSpeed() {
        return windSpeed;
    }
    
    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }
    
    public String getWindMeter() {
        return windMeter;
    }
    
    public void setWindMeter(String windMeter) {
        this.windMeter = windMeter;
    }
    
    public String getAir() {
        return air;
    }
    
    public void setAir(String air) {
        this.air = air;
    }
    
    public String getHumidity() {
        return humidity;
    }
    
    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }
    
    public String getVisibility() {
        return visibility;
    }
    
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
    
    public String getPressure() {
        return pressure;
    }
    
    public void setPressure(String pressure) {
        this.pressure = pressure;
    }
    
    public String getSunrise() {
        return sunrise;
    }
    
    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }
    
    public String getSunset() {
        return sunset;
    }
    
    public void setSunset(String sunset) {
        this.sunset = sunset;
    }
    
    public List<WeatherAlarm> getAlarms() {
        return alarms;
    }
    
    public void setAlarms(List<WeatherAlarm> alarms) {
        this.alarms = alarms;
    }
    
    /**
     * 获取空气质量等级描述
     */
    public String getAirQualityLevel() {
        if (air == null || air.isEmpty()) return "未知";
        
        try {
            int airValue = Integer.parseInt(air);
            if (airValue <= 50) return "优";
            else if (airValue <= 100) return "良";
            else if (airValue <= 150) return "轻度污染";
            else if (airValue <= 200) return "中度污染";
            else if (airValue <= 300) return "重度污染";
            else return "严重污染";
        } catch (NumberFormatException e) {
            return "未知";
        }
    }
    
    /**
     * 获取空气质量颜色值
     */
    public int getAirQualityColor() {
        if (air == null || air.isEmpty()) return 0xFF666666;
        
        try {
            int airValue = Integer.parseInt(air);
            if (airValue <= 50) return 0xFF4CAF50;      // 绿色 - 优
            else if (airValue <= 100) return 0xFF8BC34A; // 浅绿 - 良
            else if (airValue <= 150) return 0xFFFF9800; // 橙色 - 轻度污染
            else if (airValue <= 200) return 0xFFFF5722; // 深橙 - 中度污染
            else return 0xFFF44336;                      // 红色 - 重度/严重污染
        } catch (NumberFormatException e) {
            return 0xFF666666;
        }
    }
    
    /**
     * 验证温度数据是否有效（数字格式）
     */
    public boolean isValidTemperature() {
        if (temperature == null || temperature.isEmpty()) return false;
        try {
            Double.parseDouble(temperature);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 获取格式化的温度显示
     */
    public String getFormattedTemperature() {
        if (temperature == null || temperature.isEmpty()) return "--°";
        try {
            double temp = Double.parseDouble(temperature);
            return String.format("%.0f°", temp);
        } catch (NumberFormatException e) {
            return temperature + "°";
        }
    }
    
    /**
     * 获取格式化的最高温度显示
     */
    public String getFormattedTemperatureDay() {
        if (temperatureDay == null || temperatureDay.isEmpty()) return "--°";
        try {
            double temp = Double.parseDouble(temperatureDay);
            return String.format("%.0f°", temp);
        } catch (NumberFormatException e) {
            return temperatureDay + "°";
        }
    }
    
    /**
     * 获取格式化的最低温度显示
     */
    public String getFormattedTemperatureNight() {
        if (temperatureNight == null || temperatureNight.isEmpty()) return "--°";
        try {
            double temp = Double.parseDouble(temperatureNight);
            return String.format("%.0f°", temp);
        } catch (NumberFormatException e) {
            return temperatureNight + "°";
        }
    }
    
    /**
     * 获取格式化的湿度显示
     */
    public String getFormattedHumidity() {
        if (humidity == null || humidity.isEmpty()) return "--%";
        if (humidity.contains("%")) return humidity;
        return humidity + "%";
    }
    
    /**
     * 天气预警信息
     */
    public static class WeatherAlarm {
        @SerializedName("alarm_type")
        private String alarmType;
        
        @SerializedName("alarm_level")
        private String alarmLevel;
        
        @SerializedName("alarm_title")
        private String alarmTitle;
        
        @SerializedName("alarm_content")
        private String alarmContent;
        
        public String getAlarmType() {
            return alarmType;
        }
        
        public void setAlarmType(String alarmType) {
            this.alarmType = alarmType;
        }
        
        public String getAlarmLevel() {
            return alarmLevel;
        }
        
        public void setAlarmLevel(String alarmLevel) {
            this.alarmLevel = alarmLevel;
        }
        
        public String getAlarmTitle() {
            return alarmTitle;
        }
        
        public void setAlarmTitle(String alarmTitle) {
            this.alarmTitle = alarmTitle;
        }
        
        public String getAlarmContent() {
            return alarmContent;
        }
        
        public void setAlarmContent(String alarmContent) {
            this.alarmContent = alarmContent;
        }
    }
}
