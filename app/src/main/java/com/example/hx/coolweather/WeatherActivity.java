package com.example.hx.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.hx.coolweather.gson.Forecast;
import com.example.hx.coolweather.gson.Weather;
import com.example.hx.coolweather.util.HttpUtil;
import com.example.hx.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by ppc on 2017/7/30.
 */

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;//城市名
    private TextView titleUpdateTime;//更新时间
    private TextView degreeText;//度数
    private TextView weatherInfoText;//天气信息
    private LinearLayout forecastLayout;//未来几天的天气信息的外部控件
    private TextView aqiText;//aqi信息
    private TextView pm25Text;//pm25信息
    private TextView comfortText;//舒适度
    private TextView carWashText;
    private TextView sportText;
    private ImageView bcPic;//背景图片

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        judgeVersion();
        initView();
    }

    private void judgeVersion() {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                          View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initView() {
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.cityName);
        titleUpdateTime = (TextView) findViewById(R.id.update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);

        bcPic = (ImageView) findViewById(R.id.bing_pic_img);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = sp.getString("weather", null);
        String bing_pic = sp.getString("bing_pic", null);
        //加载背景图片
        if (bing_pic != null && !"".equals(bing_pic)) {
            Glide.with(this).load(bing_pic).into(bcPic);
        } else {
            loadBingPic();
        }
        //加载天气信息
        if (weather != null && !"".equals(weather)) {
            //有缓存时直接解析天气数据
            Weather weather1 = Utility.handleWeatherResponse(weather);
            showWeatherInfo(weather1);
        } else {
            String weather_id = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weather_id);
        }
    }

    /**
     * 加载背景图片
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String pic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic", pic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(pic).into(bcPic);
                    }
                });
            }
        });
    }

    /**
     * 加载天气信息
     *
     * @param weatherId 城市代码
     */
    private void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId
                + "&key=0db8030943ac43218365e36cf8c5b025";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            //请求成功
                            SharedPreferences.Editor sp = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            sp.putString("weather", responseText);
                            sp.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
    }

    /**
     * 将天气信息显示在界面上
     *
     * @param weather 天气信息
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast f : weather.forecastList) {
            View view = LayoutInflater.from(WeatherActivity.this).
                    inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(f.date);
            infoText.setText(f.more.info);
            maxText.setText(f.temperature.max);
            minText.setText(f.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            if (weather.aqi.aqiCity != null && weather.aqi.aqiCity.aqi != null)
                aqiText.setText(weather.aqi.aqiCity.aqi);
            if (weather.aqi.aqiCity != null && weather.aqi.aqiCity.pm25 != null)
                pm25Text.setText(weather.aqi.aqiCity.pm25);
        }
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        weatherLayout.setVisibility(View.VISIBLE);
    }
}
